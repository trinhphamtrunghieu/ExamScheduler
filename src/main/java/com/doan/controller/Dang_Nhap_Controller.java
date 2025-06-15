package com.doan.controller;

import com.doan.dto.Sinh_Vien;
import com.doan.model.Cache;
import com.doan.model.Student;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class Dang_Nhap_Controller {

	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request, HttpSession session) {
		Map<String, Object> response = new HashMap<>();
		System.out.println("Login as: " + request.getRole());
		if ("PROFESSOR".equals(request.getRole())) {
			session.setAttribute("userRole", "PROFESSOR");
			response.put("role", "PROFESSOR");
			return ResponseEntity.ok(response);
		}

		if ("STUDENT".equals(request.getRole()) && request.getStudentId() != null) {
			Cache cache = Cache.cache;
			Student student = cache.students.get(request.getStudentId());
			if (student != null) {
				session.setAttribute("userRole", "STUDENT");
				session.setAttribute("studentId", request.getStudentId());
				response.put("role", "STUDENT");
				response.put("studentId", request.getStudentId());
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Student ID not found"));
			}
		}

		return ResponseEntity.badRequest().body(Map.of("error", "Invalid login request"));
	}
	@PostMapping("/logout")
	public ResponseEntity<String> logout(HttpSession session) {
		session.invalidate();
		return ResponseEntity.ok("Logged out successfully");
	}
	@GetMapping("/session")
	public ResponseEntity<Map<String, Object>> getSession(HttpSession session) {
		Map<String, Object> response = new HashMap<>();
		String userRole = (String) session.getAttribute("userRole");
		String studentId = (String) session.getAttribute("studentId");

		if (userRole == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not logged in"));
		}

		response.put("role", userRole);
		if ("STUDENT".equals(userRole)) {
			response.put("studentId", studentId);
		}

		return ResponseEntity.ok(response);
	}
}

// DTO for Login Request
class LoginRequest {
	private String role;
	private String studentId;

	public String getRole() { return role; }
	public void setRole(String role) { this.role = role; }

	public String getStudentId() { return studentId; }
	public void setStudentId(String studentId) { this.studentId = studentId; }
}
