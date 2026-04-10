package com.doan.controller;

import com.doan.common.Helper;
import com.doan.dto.Sinh_Vien;
import com.doan.model.Cache;
import com.doan.model.Student;
import com.opencsv.CSVWriter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/students")
public class Sinh_Vien_Controller {

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
	                                             @RequestParam(defaultValue = "UTF-8") String encoding // default to UTF-8
	) {
		try {
			StringWriter stringWriter = new StringWriter();
			CSVWriter csvWriter = new CSVWriter(stringWriter);

			// Write header
			String[] header = {"MSSV", "Họ tên"};
			csvWriter.writeNext(header);

			// Write data rows
			for (Map<String, Object> student : students) {
				String[] row = {
						String.valueOf(student.get("id")),
						String.valueOf(student.get("name"))
				};
				csvWriter.writeNext(row);
			}

			csvWriter.close();
			byte[] csvBytes = stringWriter.toString().getBytes("UTF-8");
			byte[] bom = new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF};
			byte[] csvWithBom = new byte[bom.length + csvBytes.length];
			System.arraycopy(bom, 0, csvWithBom, 0, bom.length);
			System.arraycopy(csvBytes, 0, csvWithBom, bom.length, csvBytes.length);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("text/csv"));
			headers.setContentDispositionFormData("attachment",
					"students_" + java.time.LocalDate.now() + ".csv");
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
					.contentLength(csvBytes.length)
					.body(csvBytes);

		} catch (Exception e) {
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/import")
	public ResponseEntity<Map<String, Object>> importStudents(@RequestBody @RequestParam("file") MultipartFile file) {
		try {
			Map<String, Object> response = new HashMap<>();
			if (file.isEmpty()) {
				response.put("error", "Please upload a csv file to import");
			}
			List<CSVRecord> records = Helper.parseCSVFromValidHeader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8), Set.of("MSSV", "Họ tên"));
			List<Student> students = new ArrayList<>();
			Cache cache = Cache.cache;
			Set<String> existingIds = new HashSet<>(cache.students.values()
					.stream()
					.map(Student::getId)
					.collect(Collectors.toSet()));
			for (CSVRecord record : records) {
				String maSV = record.get("MSSV");
				String tenSV = record.get("Họ tên");

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
}