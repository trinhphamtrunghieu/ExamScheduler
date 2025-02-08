package com.doan.controller;

import com.doan.dto.*;
import com.doan.model.UserRole;
import com.doan.repository.Mon_Hoc_Repository;
import com.doan.services.Dang_Ky_Service;
import com.doan.services.Sinh_Vien_Service;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/registrations")
public class Dang_Ky_Controller {
	@Autowired
	private Dang_Ky_Service dangKyService;

	@Autowired
	private Sinh_Vien_Service sinhVienService;

	@Autowired
	private Mon_Hoc_Repository monHocRepository;

	@GetMapping
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
	public ResponseEntity<List<Mon_Hoc>> findAvailableCourse(@RequestBody RegisterRequest request) {
		List<Dang_Ky_DTO> dangKyDtos = dangKyService.getByMaSinhVien(request.getMaSinhVien());
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
			byte[] csvBytes = stringWriter.toString().getBytes("UTF-8");

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

}
