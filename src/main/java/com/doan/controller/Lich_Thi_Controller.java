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
			List<Lich_Thi_DTO> result = schedulerService.generateExamSchedule(options);
			List<Map.Entry<String, String>> conflict_check = schedulerService.evaluate(result);
			if (!conflict_check.isEmpty()) {
				System.out.println("Conflict");
				StringBuilder conflictDetail  = new StringBuilder();
				conflict_check.forEach(entry -> {
					conflictDetail.append("Conflict between ")
							.append(entry.getKey())  // Example: "Exam A"
							.append(" and ")
							.append(entry.getValue())  // Example: "Exam B"
							.append("\n");
				});
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(Collections.singletonMap("error", conflictDetail.toString()));
			} else {
				System.out.println("No conflict");
				return ResponseEntity.ok(result);
			}
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

}
