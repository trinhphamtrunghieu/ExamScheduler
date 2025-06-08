package com.doan.controller;

import com.doan.dto.*;
import com.doan.model.Cache;
import com.doan.model.UserRole;
import com.doan.repository.Mon_Hoc_Repository;
import com.doan.services.Dang_Ky_Service;
import com.doan.services.Sinh_Vien_Service;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/registrations")
public class Dang_Ky_Controller {
	private Cache cache = Cache.cache;
	@Autowired
	private Dang_Ky_Service dangKyService;

	@Autowired
	private Sinh_Vien_Service sinhVienService;

	@Autowired
	private Mon_Hoc_Repository monHocRepository;

	@GetMapping("/list")
	public ResponseEntity<List<Dang_Ky_DTO>> getStudentRegistration(HttpSession session) {
		if (Common.checkAllowRole(session, UserRole.PROFESSOR)) {
			return ResponseEntity.ok(dangKyService.getAllRegistration());
		}
		String studentID = (String) session.getAttribute("studentId");
		Optional<Sinh_Vien> student = sinhVienService.findStudent(studentID);
		if (student.isPresent()) {
			return ResponseEntity.ok(dangKyService.getByMaSinhVien(studentID));
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	}

	@PostMapping("/register")
	public ResponseEntity<?> registerStudent(@RequestBody RegisterRequest request) {
		if (request.getMaSinhVien() == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Hãy nhập mã sinh viên"));
		}
		Optional<Sinh_Vien> isAvailable = sinhVienService.findStudent(request.getMaSinhVien());
		if (!isAvailable.isPresent()) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Sinh viên không tồn tại."));
		}
		List<Dang_Ky> registrations = new ArrayList<>();

		for (String courseId : request.getCourseIds()) {
			Dang_Ky registration = new Dang_Ky();
			registration.setMa_sinh_vien(request.getMaSinhVien());
			registration.setMaMonHoc(courseId);
			registrations.add(registration);
		}

		List<Dang_Ky> result = dangKyService.saveAll(registrations);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/find-available")
	public ResponseEntity<?> findAvailableCourse(@RequestBody RegisterRequest request) {
		List<Dang_Ky_DTO> dangKyDtos = dangKyService.getByMaSinhVien(request.getMaSinhVien());
		if (request.getMaSinhVien() == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Hãy nhập mã sinh viên"));
		}
		Optional<Sinh_Vien> isAvailable = sinhVienService.findStudent(request.getMaSinhVien());
		if (!isAvailable.isPresent()) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Sinh viên không tồn tại."));
		}
		if (dangKyDtos.isEmpty()) {
			List<Mon_Hoc> result = monHocRepository.findAll();
			return ResponseEntity.ok(result);
		}
		List<String> maMonHoc = dangKyDtos.stream().map(Dang_Ky_DTO::getMa_mon_hoc).collect(Collectors.toList());
		List<Mon_Hoc> result = monHocRepository.findByMaMonHocNotIn(maMonHoc);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/export")
	public ResponseEntity<byte[]> exportRegistration(@RequestBody List<Map<String, Object>> schedule) {
		try {
			StringWriter stringWriter = new StringWriter();
			CSVWriter csvWriter = new CSVWriter(stringWriter);

			// Write header
			String[] header = {"ma_sinh_vien", "ma_mon_hoc", "ten_mon_hoc", "ten_giang_vien", "ten_sinh_vien"};
			csvWriter.writeNext(header);

			// Write data rows
			for (Map<String, Object> exam : schedule) {
				String[] row = {
						String.valueOf(exam.get("ma_sinh_vien")),
						String.valueOf(exam.get("ma_mon_hoc")),
						String.valueOf(exam.get("ten_mon_hoc")),
						String.valueOf(exam.get("ten_giang_vien")),
						String.valueOf(exam.get("ten_sinh_vien"))
				};
				csvWriter.writeNext(row);
			}

			csvWriter.close();
//			byte[] csvBytes = stringWriter.toString().getBytes("UTF-8");
			byte[] csvBytes = cache.exportAll();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("text/csv"));
			headers.setContentDispositionFormData("attachment",
					"exam_schedule_" + java.time.LocalDate.now() + ".csv");
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			return ResponseEntity.ok()
					.headers(headers)
					.body(csvBytes);

		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/import")
	public ResponseEntity<Map<String, Object>> importRegistration(@RequestBody @RequestParam("file") MultipartFile file) {
		try {
			Map<String, Object> response = new HashMap<>();
			if (file.isEmpty()) {
				response.put("error", "Please upload a csv file to import");
			}
			Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
			CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL.withTrim().withFirstRecordAsHeader());
			List<Dang_Ky> list_dang_ky = new ArrayList<>();
			Map<String, String> existingIds = new HashMap<>(dangKyService.getAllRegistration()
					.stream()
					.collect(Collectors.toMap(
							k -> k.getMa_sinh_vien(),
							v -> v.getMa_mon_hoc())));
			for (CSVRecord record : parser) {
				String maSV = record.get("MSSV");
				String tenSV = record.get("Họ tên");
				String tenMH = record.get("Môn học");
				String maMH = record.get("Mã lớp học");
				// Skip if student ID already exists
				if (existingIds.keySet().contains(maSV) && existingIds.keySet().contains(maMH)) continue;

				// Skip if required fields are missing
				if (maSV == null || maSV.isEmpty()
						|| maMH == null || maMH.isEmpty()
						|| tenMH == null || tenMH.isEmpty()) continue;

				Dang_Ky dangKyDto = new Dang_Ky();
				dangKyDto.setTenMonHoc(tenMH);
				dangKyDto.setMa_sinh_vien(maSV);
				dangKyDto.setMaMonHoc(maMH);
				list_dang_ky.add(dangKyDto);
			}

			parser.close();

			if (!list_dang_ky.isEmpty()) {
				dangKyService.saveAll(list_dang_ky);
				response.put("message", "Imported " + list_dang_ky.size() + " students successfully");
				return ResponseEntity.ok(response);
			} else {
				response.put("error", "No valid students found in CSV");
				return ResponseEntity.badRequest().body(response);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}}
