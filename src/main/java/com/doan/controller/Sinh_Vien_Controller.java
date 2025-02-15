package com.doan.controller;

import com.doan.dto.Sinh_Vien;
import com.doan.model.UserRole;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/students")
public class Sinh_Vien_Controller {
	@Autowired
	private Sinh_Vien_Service sinhVienService;

	@GetMapping("/list")
	public ResponseEntity<List<Sinh_Vien>> getAllStudent(HttpSession session) {
		if (Common.checkAllowRole(session, UserRole.PROFESSOR)) {
			List<Sinh_Vien> result = sinhVienService.getAllStudent();
			return ResponseEntity.ok(result);
		}
		return ResponseEntity.badRequest().build();
	}
	@PostMapping("/add")
	public ResponseEntity<?> addStudent(@RequestBody Sinh_Vien newStudent, HttpSession session) {
		if (!Common.checkAllowRole(session, UserRole.PROFESSOR)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		if (newStudent.getMa_sinh_vien() == null || newStudent.getTen_sinh_vien() == null) {
			return ResponseEntity.badRequest().build();
		}
		Optional<Sinh_Vien> isAvailable = sinhVienService.findStudent(newStudent.getMa_sinh_vien());
		if (isAvailable.isPresent()) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Sinh viên với mã số " + newStudent.getMa_sinh_vien() + " đã tồn tại."));
		}
		Sinh_Vien savedStudent = sinhVienService.addStudent(newStudent);
		return ResponseEntity.ok(savedStudent);
	}
	@PostMapping("/export")
	public ResponseEntity<byte[]> exportStudents(@RequestBody List<Map<String, Object>> students) {
		try {
			StringWriter stringWriter = new StringWriter();
			CSVWriter csvWriter = new CSVWriter(stringWriter);

			// Write header
			String[] header = {"Mã Sinh Viên", "Tên Sinh Viên"};
			csvWriter.writeNext(header);

			// Write data rows
			for (Map<String, Object> student : students) {
				String[] row = {
						String.valueOf(student.get("ma_sinh_vien")),
						String.valueOf(student.get("ten_sinh_vien"))
				};
				csvWriter.writeNext(row);
			}

			csvWriter.close();
			byte[] csvBytes = stringWriter.toString().getBytes("UTF-8");

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("text/csv"));
			headers.setContentDispositionFormData("attachment",
					"students_" + java.time.LocalDate.now() + ".csv");
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			return ResponseEntity.ok()
					.headers(headers)
					.body(csvBytes);

		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}
}
