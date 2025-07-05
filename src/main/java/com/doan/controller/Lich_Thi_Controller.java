package com.doan.controller;

import com.doan.dto.Lich_Thi;
import com.doan.dto.Lich_Thi_DTO;
import com.doan.dto.Lich_Thi_Option;
import com.doan.model.Schedule;
import com.doan.model.Subject;
import com.doan.model.UserRole;
import com.doan.services.ExamSchedulerService;
import com.doan.services.ExamSchedulerService2;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/schedule")
public class Lich_Thi_Controller {
	@Autowired
	private ExamSchedulerService schedulerService;

	@Autowired
	private ExamSchedulerService2 schedulerService2;

	@PostMapping("/generate")
	public ResponseEntity<?> generateSchedule(HttpSession httpSession, @RequestBody Lich_Thi_Option options) {
		if (Common.checkAllowRole(httpSession, UserRole.PROFESSOR)) {
			//only 5 timeslot. must have better way
			if (options.getMaxExamPerDay() * 5 * options.getDayDiff() < options.getSelectedSubjects().size()) {
				return ResponseEntity.status(HttpStatus.OK).body(Collections.singletonMap("error", "Max exam per slot is too small."));
			}
			List<Schedule> result = schedulerService.generateExamSchedule(options);
//			List<Lich_Thi_DTO> ldto = schedulerService.convertSchedule(result);
			String conflict_check = schedulerService.evaluate(result);
			if (!conflict_check.isEmpty()) {
				Map<String, Object> response = new HashMap<>();
				response.put("error", conflict_check);
				response.put("data", result);
				System.out.println(conflict_check);
				return ResponseEntity.status(HttpStatus.OK)
						.body(response);
			} else {
				Map<String, Object> response = new HashMap<>();
				response.put("error", "success");
				response.put("data", result);
//				schedulerService.saveToDB(ldto);
				return ResponseEntity.status(HttpStatus.OK)
						.body(response);
			}
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	@PostMapping("/generate2")
	public ResponseEntity<?> generateSchedule2(HttpSession httpSession, @RequestBody Lich_Thi_Option options) {
		if (Common.checkAllowRole(httpSession, UserRole.PROFESSOR)) {
			//only 5 timeslot. must have better way
			if (options.getMaxExamPerDay() * 5 * options.getDayDiff() < options.getSelectedSubjects().size()) {
				return ResponseEntity.status(HttpStatus.OK).body(Collections.singletonMap("error", "Max exam per slot is too small."));
			}
			List<Schedule> schedule = schedulerService2.generateExamSchedule(options);
			String conflict_check = "";
			boolean is_conflict = schedulerService2.validateSchedule(conflict_check, schedule, schedulerService2.cur_registration);
			if (!is_conflict) {
				Map<String, Object> response = new HashMap<>();
				response.put("error", conflict_check);
				response.put("data", schedule);
				System.out.println(conflict_check);
				return ResponseEntity.status(HttpStatus.OK)
						.body(response);
			} else {
				Map<String, Object> response = new HashMap<>();
				response.put("error", "success");
				response.put("data", schedule);
//				schedulerService.saveToDB(ldto);
				return ResponseEntity.status(HttpStatus.OK)
						.body(response);
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
				System.out.println(exam.get("subject"));
				String[] row = {
						String.valueOf(exam.get("id")),
						String.valueOf(exam.get("subjectName")),
						String.valueOf(exam.get("date")),
						String.valueOf(exam.get("time")),
						String.valueOf(exam.get("endTime")),
				};
				csvWriter.writeNext(row);
			}

			csvWriter.close();
			byte[] csvBytes = stringWriter.toString().getBytes(StandardCharsets.UTF_8);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("text/csv"));
			headers.setContentDispositionFormData("attachment",
					"exam_schedule_" + java.time.LocalDate.now() + ".csv");
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
					.contentLength(csvBytes.length)
					.body(csvBytes);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}

}
