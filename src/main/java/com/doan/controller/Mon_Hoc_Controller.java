package com.doan.controller;

import com.doan.common.Helper;
import com.doan.model.Cache;
import com.doan.model.Subject;
import com.doan.model.UserRole;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subjects")
public class Mon_Hoc_Controller {

	@GetMapping("/list")
	public ResponseEntity<List<Subject>> getAllSubject(HttpSession session) {
		if (Common.checkAllowRole(session, UserRole.PROFESSOR) || Common.checkAllowRole(session, UserRole.STUDENT)) {
			Cache cache = Cache.cache;
			List<Subject> result = new ArrayList<>(cache.subjects.values());
			System.out.println("Fetch subject list. size: " + result.size());
			return ResponseEntity.ok(result);
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	@PostMapping("/add")
	public ResponseEntity<Subject> addCourse(@RequestBody Subject newCourse, HttpSession session) {
		if (!Common.checkAllowRole(session, UserRole.PROFESSOR) && !Common.checkAllowRole(session, UserRole.STUDENT)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		if (newCourse.id == null || newCourse.name == null || newCourse.id.isEmpty() || newCourse.name.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		Cache cache = Cache.cache;
		cache.addSubjects(newCourse);
		return ResponseEntity.ok(newCourse);
	}

	@PostMapping("/export")
	public ResponseEntity<byte[]> exportSubjects(@RequestBody List<Map<String, Object>> courses) {
		try {
			StringWriter stringWriter = new StringWriter();
			CSVWriter csvWriter = new CSVWriter(stringWriter);

			// Write header
			String[] header = {"MSSV", "Tên Môn Học", "Gỉảng viên", "Ngày bắt đầu",
					"Ngày kết thúc", "Thời Lượng Thi"};
			csvWriter.writeNext(header);

			// Write data rows
			for (Map<String, Object> exam : courses) {
				String[] row = {
						String.valueOf(exam.get("maMonHoc")),
						String.valueOf(exam.get("tenMonHoc")),
						String.valueOf(exam.get("ten_gv_dung_lop")),
						String.valueOf(exam.get("ngay_bat_dau")),
						String.valueOf(exam.get("ngay_ket_thuc")),
						String.valueOf(exam.get("thoi_luong_thi")) + " phút"
				};
				csvWriter.writeNext(row);
			}

			csvWriter.close();
			byte[] csvBytes = stringWriter.toString().getBytes("UTF-8");

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("text/csv"));
			headers.setContentDispositionFormData("attachment",
					"subjects_" + java.time.LocalDate.now() + ".csv");
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			return ResponseEntity.ok()
					.headers(headers)
					.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
					.contentLength(csvBytes.length)
					.body(csvBytes);

		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/import")
	public ResponseEntity<Map<String, Object>> importSujects(@RequestBody @RequestParam("file") MultipartFile file) {
		Map<String, Object> response = new HashMap<>();
		StringBuilder error = new StringBuilder();
		try {
			if (file.isEmpty()) {
				response.put("error", "Please upload a csv file to import");
			}

			Cache cache = Cache.cache;
			Set<String> existingIds = new HashSet<>(cache.subjects.values()
					.stream()
					.map(Subject::getId)
					.collect(Collectors.toSet()));
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy"); // handles single-digit day/month
			List<Subject> subjects = new ArrayList<>();
			List<CSVRecord> records = Helper.parseCSVFromValidHeader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8),
					Set.of("Mã lớp học", "TÊN MÔN HỌC", "GIẢNG VIÊN", "Ngày bắt đầu", "Ngày kết thúc"));
			for (CSVRecord record : records) {
				try {
					String subjectID = record.get("Mã lớp học");
					String subjectName = record.get("TÊN MÔN HỌC");
					String teacher = record.get("GIẢNG VIÊN");
					String startDate = record.get("Ngày bắt đầu");
					String endDate = record.get("Ngày kết thúc");
					int duration = 90;

					// Skip if student ID already exists
					if (existingIds.contains(subjectID)) continue;

					// Skip if required fields are missing
					if (subjectID == null || subjectID.isEmpty() || subjectName == null || subjectName.isEmpty())
						continue;

					Subject subject = new Subject();
					subject.setId(subjectID);
					subject.setName(subjectName);
					subject.setTeacher(teacher);
					subject.setDuration(duration);
					subject.setStartDate(java.sql.Date.valueOf(LocalDate.parse(startDate, formatter)));
					subject.setEndDate(java.sql.Date.valueOf(LocalDate.parse(endDate, formatter)));
					existingIds.add(subjectID); // Prevent duplicates in same file
					subjects.add(subject);
				} catch (DateTimeParseException ex) {
					String dateFailed = record.get("Mã lớp học");
					error.append(dateFailed + " invalid datetime. format: d/M/yyyy").append("\n");
				}
			}

			if (!(error.length() == 0)) {
				response.put("error", error);
				return ResponseEntity.internalServerError().body(response);
			}
			if (!subjects.isEmpty()) {
				cache.addSubjects(subjects);
				response.put("message", "Imported " + subjects.size() + " students successfully");
				return ResponseEntity.ok(response);
			} else {
				response.put("error", "No valid subjects found in CSV");
				return ResponseEntity.badRequest().body(response);
			}
		} catch (IllegalStateException ex) {
			error.append("no header found. expect: Mã lớp học, TÊN MÔN HỌC, GIẢNG VIÊN, Ngày bắt đầu, Ngày kết thúc. case-insensitive\n");
			response.put("error", error);
			return ResponseEntity.internalServerError().body(response);
		} catch (IOException ex) {
			error.append("can not open file. ex: " + ex.toString());
			response.put("error", error);
			return ResponseEntity.internalServerError().body(response);
		}
	}
}
