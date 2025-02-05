package com.doan.services;

import com.doan.dto.Mon_Hoc;
import com.doan.repository.Mon_Hoc_Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Mon_Hoc_Service {
	@Autowired
	private Mon_Hoc_Repository monHocRepository;
	public List<Mon_Hoc> getAllSubjects() {
		return monHocRepository.findAll();
	}
	public Mon_Hoc addSubjecy(Mon_Hoc subject) {
		return monHocRepository.save(subject);
	}
}
