package com.doan.controller;

import com.doan.common.Helper;
import com.doan.dto.Dang_Ky;
import com.doan.dto.Mon_Hoc;
import com.doan.dto.Sinh_Vien;
import com.doan.model.Cache;
import com.doan.model.InClass;
import com.doan.model.Student;
import com.doan.model.Subject;
import com.doan.repository.Dang_Ky_Repository;
import com.doan.repository.Lich_Thi_Repository;
import com.doan.repository.Mon_Hoc_Repository;
import com.doan.repository.Sinh_Vien_Repository;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.sql.Date;
import java.util.*;

@RestController
@RequestMapping("/config")
public class ConfigurationController {
	private Cache cache = Cache.cache;
	@Autowired
	private Dang_Ky_Repository dangKyRepository;

	@Autowired
	private Mon_Hoc_Repository monHocRepository;

	@Autowired
	private Sinh_Vien_Repository sinhVienRepository;

	@Autowired
	private Lich_Thi_Repository lichThiRepository;

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
			cache.importAll(records, true);
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
	public ResponseEntity<?> saveCacheToDB() {
		try {
			Map<String, Student> students = this.cache.students;
			Map<String, InClass> classes = this.cache.classes;
			Map<String, Subject> subjects = this.cache.subjects;
			dangKyRepository.deleteAllDangKy();
			monHocRepository.deleteAll();
			sinhVienRepository.deleteAll();
			lichThiRepository.deleteAll();
			List<Sinh_Vien> sinh_vienList = new ArrayList<>();
			List<Mon_Hoc> mon_hocList = new ArrayList<>();
			List<Dang_Ky> dang_kyList = new ArrayList<>();
			for (Student s : students.values()) {
				Sinh_Vien sinh_vien = new Sinh_Vien();
				sinh_vien.setMa_sinh_vien(s.id);
				sinh_vien.setTen_sinh_vien(s.name);
				sinh_vienList.add(sinh_vien);
				for (Subject regisSubject : s.participateIn) {
					Dang_Ky dang_ky = new Dang_Ky();
					dang_ky.setMaMonHoc(regisSubject.id);
					dang_ky.setTenMonHoc(regisSubject.name);
					dang_ky.setMa_sinh_vien(s.id);
					dang_kyList.add(dang_ky);
				}
			}
			for (Subject s : subjects.values()) {
				Mon_Hoc monHoc = new Mon_Hoc();
				monHoc.setTenMonHoc(s.name);
				monHoc.setMaMonHoc(s.id);
				monHoc.setTen_gv_dung_lop(s.teacher);
				monHoc.setNgay_bat_dau(new Date(System.currentTimeMillis()));
				monHoc.setNgay_ket_thuc(new Date(System.currentTimeMillis() + (long) 3600 * 1000 * 24 * 30));
				monHoc.setThoi_luong_thi(90);
				mon_hocList.add(monHoc);
			}
			sinhVienRepository.saveAllAndFlush(sinh_vienList);
			monHocRepository.saveAllAndFlush(mon_hocList);
			dangKyRepository.saveAllAndFlush(dang_kyList);
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().build();
		}
	}
}
