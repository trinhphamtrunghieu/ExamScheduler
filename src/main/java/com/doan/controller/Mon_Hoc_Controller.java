package com.doan.controller;

import com.doan.dto.Mon_Hoc;
import com.doan.model.UserRole;
import com.doan.services.Mon_Hoc_Service;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subjects")
public class Mon_Hoc_Controller {
	@Autowired
	private Mon_Hoc_Service monHocService;

	@GetMapping
	public ResponseEntity<List<Mon_Hoc>> getAllSubject(HttpSession session) {
		if (Common.checkAllowRole(session, UserRole.PROFESSOR) || Common.checkAllowRole(session, UserRole.STUDENT)) {
			List<Mon_Hoc> result = monHocService.getAllSubjects();
			return ResponseEntity.ok(result);
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	@PostMapping("/add")
	public ResponseEntity<Mon_Hoc> addCourse(@RequestBody Mon_Hoc newCourse, HttpSession session) {
		if (!Common.checkAllowRole(session, UserRole.PROFESSOR) && !Common.checkAllowRole(session, UserRole.STUDENT)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		if (newCourse.maMonHoc == null || newCourse.tenMonHoc == null || newCourse.maMonHoc.isEmpty() || newCourse.tenMonHoc.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		Mon_Hoc savedCourse = monHocService.addSubjecy(newCourse);
		return ResponseEntity.ok(savedCourse);
	}
}
