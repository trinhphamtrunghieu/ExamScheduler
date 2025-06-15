package com.doan.controller;

import com.doan.common.Helper;
import com.doan.model.Cache;
import com.doan.model.InClass;
import com.doan.model.Student;
import com.doan.model.Subject;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.sql.Date;
import java.util.*;

@RestController
@RequestMapping("/config")
public class ConfigurationController {
	private Cache cache = Cache.cache;

	@GetMapping("/export")
	public ResponseEntity<?> exportRegistration() {
		try {
			if (cache.students.isEmpty() && cache.subjects.isEmpty() && cache.classes.isEmpty()) {
				System.out.println("Cache empty");
				Map<String, Object> response = new HashMap<>();
				response.put("error", "no data to export");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
			byte[] csvBytes = cache.exportAll();
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

	@PostMapping("/import")
	public ResponseEntity<Map<String, Object>> importRegistration(@RequestBody @RequestParam("file") MultipartFile file) {
		Map<String, Object> response = new HashMap<>();
		try {
			if (file.isEmpty()) {
				response.put("error", "Please upload a csv file to import");
			}
			List<CSVRecord> records = Helper.parseCSVFromValidHeader(new InputStreamReader(file.getInputStream()),
					Set.of("MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên"));
			cache.importAll(records, false);
			response.put("message", "Imported successfully");
			return ResponseEntity.ok(response);
		} catch (IllegalStateException ex) {
			response.put("error", "Required field: MSSV, Họ tên, Mã lớp học, Môn học, Giáo viên");
			return ResponseEntity.badRequest().body(response);
	    } catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/save")
	public ResponseEntity<Map<String, Object>> saveDB() {
		Map<String, Object> response = new HashMap<>();
		try {
			Cache.cache.saveToDisk();
			response.put("message", "Data save");
			return ResponseEntity.ok(response);
		} catch (IllegalStateException ex) {
			response.put("error", "Data save failed");
			return ResponseEntity.badRequest().body(response);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}
}
