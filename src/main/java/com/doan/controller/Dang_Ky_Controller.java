package com.doan.controller;

import com.doan.dto.*;
import com.doan.model.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/registrations")
public class Dang_Ky_Controller {

	@GetMapping("/list")
	public ResponseEntity<?> getStudentRegistration() {
		List<Registration> result = new ArrayList<>();
		for (Student student : Cache.cache.students.values()) {
			result.addAll(student.registrations);
		}
		return ResponseEntity.ok(result);
	}

	@PostMapping("/register")
	public ResponseEntity<?> registerStudent(@RequestBody RegisterRequest request) {
		Cache cache = Cache.cache;
		if (request.getMaSinhVien() == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Hãy nhập mã sinh viên"));
		}
		Student isAvailable = cache.students.get(request.getMaSinhVien());
		if (isAvailable == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Sinh viên không tồn tại."));
		}

		for (String courseId : request.getCourseIds()) {
			if (!cache.subjects.containsKey(courseId)) {
				return ResponseEntity.badRequest().body(Collections.singletonMap("error", courseId + " không tồn tại"));
			}
			Cache.cache.students.get(request.getMaSinhVien()).registerSubject(cache.subjects.get(courseId));
		}
		return ResponseEntity.ok(Collections.singletonMap(
				"error", "Sinh viên " + request.getMaSinhVien() + " đã đăng ký thành công vào các môn: " + request.getCourseIds()));
	}

	@PostMapping("/find-available")
	public ResponseEntity<?> findAvailableCourse(@RequestBody RegisterRequest request) {
		if (request.getMaSinhVien() == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Hãy nhập mã sinh viên"));
		}
		Cache cache = Cache.cache;
		Student student = cache.students.get(request.getMaSinhVien());
		if (student == null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Sinh viên không tồn tại."));
		}
		if (student.participateIn.isEmpty()) {
			List<Subject> result = new ArrayList<>(cache.subjects.values());
			return ResponseEntity.ok(result);
		}
		List<Subject> result = cache.subjects
				.entrySet()
				.stream()
				.filter(entry -> !request.getCourseIds().contains(entry.getKey()))
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());
		return ResponseEntity.ok(result);
	}

	@PostMapping("/export")
	public ResponseEntity<?> exportRegistration(@RequestParam(value = "format", required = false, defaultValue = "csv") String format) {
		List<Registration> registrations = new ArrayList<>();
		for (Student student : Cache.cache.students.values()) {
			registrations.addAll(student.getRegistrations());
		}
		try {
			if (registrations.isEmpty()) {
				Map<String, Object> response = new HashMap<>();
				response.put("error", "no data to export");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
			boolean exportAsXlsx = "xlsx".equalsIgnoreCase(format);
			byte[] exportBytes = exportAsXlsx
					? Common.exportRegistrationsXlsx(registrations)
					: Common.exportRegistrations(registrations);
			String fileExtension = exportAsXlsx ? "xlsx" : "csv";
			String contentType = exportAsXlsx
					? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
					: "text/csv; charset=UTF-8";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(contentType));
			headers.setContentDispositionFormData("attachment",
					"exam_schedule_" + java.time.LocalDate.now() + "." + fileExtension);
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			return ResponseEntity.ok()
					.headers(headers)
					.contentType(MediaType.parseMediaType(contentType))
					.contentLength(exportBytes.length)
					.body(exportBytes);

		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}

}
