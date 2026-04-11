package com.doan.controller;

import com.doan.dto.Lich_Thi_Option;
import com.doan.model.Schedule;
import com.doan.services.ExamSchedulerService;
import com.doan.services.ExamSchedulerService2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.*;

@RestController
@RequestMapping("/schedule")
public class Lich_Thi_Controller {
	@Autowired
	private ExamSchedulerService schedulerService;

	@Autowired
	private ExamSchedulerService2 schedulerService2;

	@PostMapping("/generate")
	public ResponseEntity<?> generateSchedule(@RequestBody Lich_Thi_Option options) {
		//only 5 timeslot. must have better way
		if (options.getMaxExamPerDay() * 5 * options.getDayDiff() < options.getSelectedSubjects().size()) {
			return ResponseEntity.status(HttpStatus.OK).body(Collections.singletonMap("error", "Max exam per slot is too small."));
		}
		List<Schedule> result = schedulerService.generateExamSchedule(options);
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
			return ResponseEntity.status(HttpStatus.OK)
					.body(response);
		}
	}

	@PostMapping("/generate2")
	public ResponseEntity<?> generateSchedule2(@RequestBody Lich_Thi_Option options) {
		//only 5 timeslot. must have better way
		if (options.getMaxExamPerDay() * 5 * options.getDayDiff() < options.getSelectedSubjects().size()) {
			return ResponseEntity.status(HttpStatus.OK).body(Collections.singletonMap("error", "Max exam per slot is too small."));
		}
		List<Schedule> schedule = new ArrayList<>();
		try {
			schedule = schedulerService2.generateExamSchedule(options);
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
				return ResponseEntity.status(HttpStatus.OK)
						.body(response);
			}
		} catch (IllegalStateException ex) {
			System.out.println(ex.getMessage());
			Map<String, Object> response = new HashMap<>();
			response.put("error", "Generate schedule failed");
			response.put("data", ex.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(response);
		}
	}


	@PostMapping("/export")
	public ResponseEntity<byte[]> exportSchedule(@RequestBody List<Map<String, Object>> schedule,
	                                             @RequestParam(value = "format", required = false, defaultValue = "csv") String format) {
		try {
			List<String[]> rows = new ArrayList<>();
			for (Map<String, Object> exam : schedule) {
				rows.add(new String[] {
						String.valueOf(exam.get("id")),
						String.valueOf(exam.get("subjectName")),
						String.valueOf(exam.get("date")),
						String.valueOf(exam.get("time")),
						String.valueOf(exam.get("endTime")),
				});
			}

			boolean exportAsXlsx = "xlsx".equalsIgnoreCase(format);
			byte[] exportedBytes = exportAsXlsx
					? Common.exportXlsx(
					"Schedule",
					new String[] {"Mã Môn Học", "Tên Môn Học", "Ngày Thi", "Giờ Bắt Đầu Thi", "Giờ Kết Thúc Thi"},
					rows
			)
					: Common.exportCsv(
					new String[] {"Mã Môn Học", "Tên Môn Học", "Ngày Thi", "Giờ Bắt Đầu Thi", "Giờ Kết Thúc Thi"},
					rows
			);
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
					.contentLength(exportedBytes.length)
					.body(exportedBytes);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}

}
