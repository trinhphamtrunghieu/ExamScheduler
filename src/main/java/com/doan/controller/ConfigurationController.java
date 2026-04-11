package com.doan.controller;

import com.doan.common.Helper;
import com.doan.model.Cache;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/config")
public class ConfigurationController {
	private Cache cache = Cache.cache;
	private static final Set<String> REQUIRED_HEADERS = Set.of("MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên");

	@GetMapping("/export")
	public ResponseEntity<?> exportRegistration(@RequestParam(value = "format", required = false, defaultValue = "csv") String format) {
		try {
			if (cache.students.isEmpty() && cache.subjects.isEmpty() && cache.classes.isEmpty()) {
				System.out.println("Cache empty");
				Map<String, Object> response = new HashMap<>();
				response.put("error", "no data to export");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
			boolean exportAsXlsx = "xlsx".equalsIgnoreCase(format);
			byte[] exportedBytes = exportAsXlsx ? cache.exportAllXlsx() : cache.exportAll();
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
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/import")
	public ResponseEntity<Map<String, Object>> importRegistration(@RequestParam("file") MultipartFile file,
	                                                              @RequestParam(value = "headerMapping", required = false) String headerMapping) {
		Map<String, Object> response = new HashMap<>();
		try {
			if (file.isEmpty()) {
				response.put("error", "Please upload a file to import");
				return ResponseEntity.badRequest().body(response);
			}
			Map<String, String> requestedHeaderMapping = Helper.parseHeaderMappingString(headerMapping);
			Map<String, String> resolvedHeaders = new HashMap<>();
			List<CSVRecord> records = Helper.parseTabularFileFromValidHeaderWithResolvedHeaders(
					file.getOriginalFilename(),
					file.getBytes(),
					REQUIRED_HEADERS,
					requestedHeaderMapping,
					resolvedHeaders
			);
			cache.importAll(records, false, resolvedHeaders);
			response.put("message", "Imported successfully");
			return ResponseEntity.ok(response);
		} catch (IllegalStateException ex) {
			ex.printStackTrace();
			response.put("error", "Required field: MSSV, Họ tên, Mã lớp học, Môn học, Giáo viên");
			return ResponseEntity.badRequest().body(response);
	    } catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/import/headers")
	public ResponseEntity<Map<String, Object>> detectConfigImportHeaders(@RequestParam("file") MultipartFile file) {
		Map<String, Object> response = new HashMap<>();
		try {
			response.put("headers", Helper.detectHeadersFromFile(file.getOriginalFilename(), file.getBytes(), REQUIRED_HEADERS));
			response.put("expectedHeaders", REQUIRED_HEADERS);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.put("error", "Cannot detect headers from file");
			return ResponseEntity.badRequest().body(response);
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
