package com.doan.controller;

import com.doan.dto.Sinh_Vien;
import com.doan.model.UserRole;
import com.doan.services.Sinh_Vien_Service;
import jakarta.servlet.http.HttpSession;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping("/students")
public class Sinh_Vien_Controller {
	@Autowired
	private Sinh_Vien_Service sinhVienService;

	@GetMapping
	public ResponseEntity<List<Sinh_Vien>> getAllStudent(HttpSession session) {
		if (Common.checkAllowRole(session, UserRole.PROFESSOR)) {
			List<Sinh_Vien> result = sinhVienService.getAllStudent();
			return ResponseEntity.ok(result);
		}
		return ResponseEntity.badRequest().build();
	}
	@PostMapping("/add")
	public ResponseEntity<Sinh_Vien> addStudent(@RequestBody Sinh_Vien newStudent, HttpSession session) {
		if (!Common.checkAllowRole(session, UserRole.PROFESSOR)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		System.out.println("Thêm sinh viên");
		if (newStudent.getMa_sinh_vien() == null || newStudent.getTen_sinh_vien() == null) {
			return ResponseEntity.badRequest().build();
		}
		Sinh_Vien savedStudent = sinhVienService.addStudent(newStudent);
		return ResponseEntity.ok(savedStudent);
	}

}
