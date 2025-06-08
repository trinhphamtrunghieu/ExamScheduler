package com.doan.services;

import com.doan.dto.Dang_Ky;
import com.doan.dto.Dang_Ky_DTO;
import com.doan.repository.Dang_Ky_Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class Dang_Ky_Service {
	@Autowired
	private Dang_Ky_Repository dangKyRepository;
	public List<Dang_Ky_DTO> getAllRegistration() {
		return dangKyRepository.findAllWithDetail();
	}

	public List<Dang_Ky> saveAll(List<Dang_Ky> newRegistration) {
		return dangKyRepository.saveAll(newRegistration);
	}

	public List<Dang_Ky_DTO> getByMaSinhVien(String studentId) {
		return dangKyRepository.findByMSV(studentId);
	}

	public int deleteAllDangKy() {
		return dangKyRepository.deleteAllDangKy();
	}
}
