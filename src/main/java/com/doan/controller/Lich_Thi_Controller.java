package com.doan.controller;

import com.doan.dto.Lich_Thi;
import com.doan.dto.Lich_Thi_DTO;
import com.doan.dto.Lich_Thi_Option;
import com.doan.model.UserRole;
import com.doan.services.ExamSchedulerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

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

}
