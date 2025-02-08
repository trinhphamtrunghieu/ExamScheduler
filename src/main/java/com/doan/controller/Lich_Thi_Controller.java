package com.doan.controller;

import com.doan.dto.Lich_Thi_DTO;
import com.doan.dto.Lich_Thi_Option;
import com.doan.model.UserRole;
import com.doan.services.ExamSchedulerService;
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

@RestController
@RequestMapping("/generate")
public class Lich_Thi_Controller {
	@Autowired
	private ExamSchedulerService schedulerService;

	@PostMapping
	public ResponseEntity<?> generateSchedule(HttpSession httpSession, @RequestBody Lich_Thi_Option options) {
		if (Common.checkAllowRole(httpSession, UserRole.PROFESSOR)) {
			//only 5 timeslot. must have better way
			if (options.getMaxExamPerDay() * 5 * options.getDayDiff() < options.getSelectedSubjects().size()) {
				return ResponseEntity.status(HttpStatus.OK).body(Collections.singletonMap("error", "Max exam per slot is too small."));
			}
			List<Lich_Thi_DTO> result = schedulerService.generateExamSchedule(options);
			String conflict_check = schedulerService.evaluate(result);
			if (!conflict_check.isEmpty()) {
				System.out.println(conflict_check);
				return ResponseEntity.status(HttpStatus.OK)
						.body(Collections.singletonMap("error", conflict_check));
			} else {
				System.out.println("No conflict");
				return ResponseEntity.ok(result);
			}
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	@PostMapping("/export")
	public ResponseEntity<byte[]> exportSchedule(@RequestBody List<Map<String, Object>> schedule) {
		try {
			StringWriter stringWriter = new StringWriter();
			CSVWriter csvWriter = new CSVWriter(stringWriter);

			// Write header
			String[] header = {"Mã Môn Học", "Tên Môn Học", "Ngày Thi", "Giờ Bắt Đầu Thi",
					"Giờ Kết Thúc Thi", "Thời Lượng Thi"};
			csvWriter.writeNext(header);

			// Write data rows
			for (Map<String, Object> exam : schedule) {
				String[] row = {
						String.valueOf(exam.get("ma_mon_hoc")),
						String.valueOf(exam.get("ten_mon_hoc")),
						String.valueOf(exam.get("ngay_thi")),
						String.valueOf(exam.get("gio_thi")),
						String.valueOf(exam.get("gio_ket_thuc")),
						String.valueOf(exam.get("thoi_luong_thi")) + " phút"
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
