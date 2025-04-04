package com.doan.controller;

import com.doan.dto.Mon_Hoc;
import com.doan.model.UserRole;
import com.doan.services.Mon_Hoc_Service;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/subjects")
public class Mon_Hoc_Controller {
	@Autowired
	private Mon_Hoc_Service monHocService;

	@GetMapping("/list")
	public ResponseEntity<List<Mon_Hoc>> getAllSubject(HttpSession session) {
		if (Common.checkAllowRole(session, UserRole.PROFESSOR) || Common.checkAllowRole(session, UserRole.STUDENT)) {
			List<Mon_Hoc> result = monHocService.getAllSubjects();
			System.out.println("Fetch subject list. size: " + result.size());
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

	@PostMapping("/export")
	public ResponseEntity<byte[]> exportSubjects(@RequestBody List<Map<String, Object>> courses) {
		try {
			StringWriter stringWriter = new StringWriter();
			CSVWriter csvWriter = new CSVWriter(stringWriter);

			// Write header
			String[] header = {"Mã Môn Học", "Tên Môn Học", "Gỉảng viên", "Ngày bắt đầu",
					"Ngày kết thúc", "Thời Lượng Thi"};
			csvWriter.writeNext(header);

			// Write data rows
			for (Map<String, Object> exam : courses) {
				String[] row = {
						String.valueOf(exam.get("maMonHoc")),
						String.valueOf(exam.get("tenMonHoc")),
						String.valueOf(exam.get("ten_gv_dung_lop")),
						String.valueOf(exam.get("ngay_bat_dau")),
						String.valueOf(exam.get("ngay_ket_thuc")),
						String.valueOf(exam.get("thoi_luong_thi")) + " phút"
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
					.body(csvBytes);

		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}
}
