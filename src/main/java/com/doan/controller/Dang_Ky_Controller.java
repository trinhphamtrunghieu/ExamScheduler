package com.doan.controller;

import com.doan.dto.*;
import com.doan.model.*;
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

	@GetMapping("/list")
	public ResponseEntity<?> getStudentRegistration(HttpSession session) {
		if (Common.checkAllowRole(session, UserRole.PROFESSOR)) {
			HashMap<Student, List<Subject>> registrations = new HashMap<>();
			List<Registration> result = new ArrayList<>();
			List<Student> students = new ArrayList<>(Cache.cache.students.values());
			for (Student student : students) {
				result.addAll(student.registrations);
			}
			return ResponseEntity.ok(result);
		}
		String studentID = (String) session.getAttribute("studentId");
		Student student = Cache.cache.students.get(studentID);
		if (student != null) {
			return ResponseEntity.ok(student);
		}
		return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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
}
