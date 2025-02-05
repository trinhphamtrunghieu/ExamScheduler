package com.doan.controller;

import com.doan.dto.Dang_Ky;
import com.doan.dto.Dang_Ky_DTO;
import com.doan.dto.RegisterRequest;
import com.doan.dto.Sinh_Vien;
import com.doan.model.UserRole;
import com.doan.services.Dang_Ky_Service;
import com.doan.services.Sinh_Vien_Service;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/registrations")
public class Dang_Ky_Controller {
	@Autowired
	private Dang_Ky_Service dangKyService;

	@Autowired
	private Sinh_Vien_Service sinhVienService;

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

}
