package com.doan.controller;

import com.doan.common.Helper;
import com.doan.model.Cache;
import com.doan.model.Subject;
import com.opencsv.CSVWriter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subjects")
public class Mon_Hoc_Controller {
	private static final Set<String> REQUIRED_HEADERS = Set.of("Mã lớp học", "TÊN MÔN HỌC", "GIẢNG VIÊN", "Ngày bắt đầu", "Ngày kết thúc");

	@GetMapping("/list")
	public ResponseEntity<List<Subject>> getAllSubject() {
		Cache cache = Cache.cache;
		List<Subject> result = new ArrayList<>(cache.subjects.values());
		System.out.println("Fetch subject list. size: " + result.size());
		return ResponseEntity.ok(result);
	}

	@PostMapping("/add")
	public ResponseEntity<Subject> addCourse(@RequestBody Subject newCourse) {
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
						String.valueOf(exam.get("id")),
						String.valueOf(exam.get("name")),
						String.valueOf(exam.get("teacher")),
						String.valueOf(exam.get("startDate")),
						String.valueOf(exam.get("endDate")),
						String.valueOf(exam.get("duration")) + " phút"
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
	public ResponseEntity<Map<String, Object>> importSubjects(@RequestParam("file") MultipartFile file,
	                                                         @RequestParam(value = "headerMapping", required = false) String headerMapping) {
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
			Map<String, String> requestedHeaderMapping = Helper.parseHeaderMappingString(headerMapping);
			Map<String, String> resolvedHeaders = new HashMap<>();
			List<CSVRecord> records = Helper.parseCSVFromValidHeaderWithResolvedHeaders(file.getBytes(), REQUIRED_HEADERS, requestedHeaderMapping, resolvedHeaders);
			for (CSVRecord record : records) {
				try {
					String subjectID = Helper.getValue(record, resolvedHeaders, "Mã lớp học");
					String subjectName = Helper.getValue(record, resolvedHeaders, "TÊN MÔN HỌC");
					String teacher = Helper.getValue(record, resolvedHeaders, "GIẢNG VIÊN");
					String startDate = Helper.getValue(record, resolvedHeaders, "Ngày bắt đầu");
					String endDate = Helper.getValue(record, resolvedHeaders, "Ngày kết thúc");
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
					String dateFailed = Helper.getValue(record, resolvedHeaders, "Mã lớp học");
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

	@PostMapping("/import/headers")
	public ResponseEntity<Map<String, Object>> detectSubjectImportHeaders(@RequestParam("file") MultipartFile file) {
		Map<String, Object> response = new HashMap<>();
		try {
			response.put("headers", Helper.detectHeaders(file.getBytes(), REQUIRED_HEADERS));
			response.put("expectedHeaders", REQUIRED_HEADERS);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.put("error", "Cannot detect headers from CSV file");
			return ResponseEntity.badRequest().body(response);
		}
	}
}
