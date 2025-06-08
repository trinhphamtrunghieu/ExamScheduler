package com.doan.services;

import com.doan.dto.Sinh_Vien;
import com.doan.repository.Sinh_Vien_Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class Sinh_Vien_Service {
	@Autowired
	private Sinh_Vien_Repository sinhVienRepository;
	public List<Sinh_Vien> getAllStudent() {
		return sinhVienRepository.findAll();
	}
	public Sinh_Vien addStudent(Sinh_Vien newSV) {
		return sinhVienRepository.save(newSV);
	}

	public List<Sinh_Vien> addAllStudent(List<Sinh_Vien> newSV) {
		return sinhVienRepository.saveAll(newSV);
	}


	public Optional<Sinh_Vien> findStudent(String id) {
		return sinhVienRepository.findById(id);
	}
}
