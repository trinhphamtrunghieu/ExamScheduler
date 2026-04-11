package com.doan.controller;

import com.doan.common.Helper;
import com.doan.dto.Sinh_Vien;
import com.doan.model.Cache;
import com.doan.model.Student;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/students")
public class Sinh_Vien_Controller {
	private static final Set<String> REQUIRED_HEADERS = Set.of("MSSV", "Họ tên");

	@GetMapping("/list")
	public ResponseEntity<List<Student>> getAllStudent() {
		Cache cache = Cache.cache;
		List<Student> result = new ArrayList<>(cache.students.values());
		return ResponseEntity.ok(result);
	}

	@PostMapping("/add")
	public ResponseEntity<?> addStudent(@RequestBody Student newStudent) {
		if (newStudent.getId() == null || newStudent.getName() == null) {
			return ResponseEntity.badRequest().build();
		}
		Cache cache = Cache.cache;
		Student isAvailable = cache.students.get(newStudent.getId());
		if (isAvailable != null) {
			return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Sinh viên với mã số " + newStudent.getId() + " đã tồn tại."));
		}
		Cache.cache.students.put(newStudent.getId(), newStudent);
		return ResponseEntity.ok(newStudent);
	}

	@PostMapping("/export")
	public ResponseEntity<byte[]> exportStudents(@RequestBody List<Map<String, Object>> students,
	                                             @RequestParam(value = "format", required = false, defaultValue = "csv") String format
	) {
		try {
			List<String[]> rows = new ArrayList<>();
			for (Map<String, Object> student : students) {
				rows.add(new String[] {
						String.valueOf(student.get("id")),
						String.valueOf(student.get("name"))
				});
			}
			boolean exportAsXlsx = "xlsx".equalsIgnoreCase(format);
			byte[] exportedBytes = exportAsXlsx
					? Common.exportXlsx("Students", new String[] {"MSSV", "Họ tên"}, rows)
					: Common.exportCsv(new String[] {"MSSV", "Họ tên"}, rows);
			String fileExtension = exportAsXlsx ? "xlsx" : "csv";
			String contentType = exportAsXlsx
					? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
					: "text/csv; charset=UTF-8";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(contentType));
			headers.setContentDispositionFormData("attachment",
					"students_" + java.time.LocalDate.now() + "." + fileExtension);
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			return ResponseEntity.ok()
					.headers(headers)
					.contentType(MediaType.parseMediaType(contentType))
					.contentLength(exportedBytes.length)
					.body(exportedBytes);

		} catch (IOException e) {
			return ResponseEntity.internalServerError().build();
		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/import")
	public ResponseEntity<Map<String, Object>> importStudents(@RequestParam("file") MultipartFile file,
	                                                         @RequestParam(value = "headerMapping", required = false) String headerMapping) {
		try {
			Map<String, Object> response = new HashMap<>();
			if (file.isEmpty()) {
				response.put("error", "Please upload a csv file to import");
			}
			Map<String, String> resolvedHeaders = new HashMap<>();
			Map<String, String> requestedHeaderMapping = Helper.parseHeaderMappingString(headerMapping);
			List<CSVRecord> records = Helper.parseCSVFromValidHeaderWithResolvedHeaders(file.getBytes(), REQUIRED_HEADERS, requestedHeaderMapping, resolvedHeaders);
			List<Student> students = new ArrayList<>();
			Cache cache = Cache.cache;
			Set<String> existingIds = new HashSet<>(cache.students.values()
					.stream()
					.map(Student::getId)
					.collect(Collectors.toSet()));
			for (CSVRecord record : records) {
				String maSV = Helper.getValue(record, resolvedHeaders, "MSSV");
				String tenSV = Helper.getValue(record, resolvedHeaders, "Họ tên");

				// Skip if student ID already exists
				if (existingIds.contains(maSV)) continue;

				// Skip if required fields are missing
				if (maSV == null || maSV.isEmpty() || tenSV == null || tenSV.isEmpty()) continue;

				Student student = new Student();
				student.setId(maSV);
				student.setName(tenSV);
				students.add(student);
				existingIds.add(maSV); // Prevent duplicates in same file
			}

			if (!students.isEmpty()) {
				cache.addStudent(students);
				response.put("message", "Imported " + students.size() + " students successfully");
				return ResponseEntity.ok(response);
			} else {
				response.put("error", "No valid students found in CSV");
				return ResponseEntity.badRequest().body(response);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/import/headers")
	public ResponseEntity<Map<String, Object>> detectStudentImportHeaders(@RequestParam("file") MultipartFile file) {
		Map<String, Object> response = new HashMap<>();
		try {
			response.put("headers", Helper.detectHeaders(file.getBytes(), REQUIRED_HEADERS));
			response.put("expectedHeaders", REQUIRED_HEADERS);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.put("error", "Cannot detect headers from CSV file");
			return ResponseEntity.badRequest().body(response);
		}
	}
}
