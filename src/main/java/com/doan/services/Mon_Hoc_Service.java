package com.doan.services;

import com.doan.dto.Mon_Hoc;
import com.doan.repository.Mon_Hoc_Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
	public List<Mon_Hoc> saveAll(List<Mon_Hoc> src){
		return monHocRepository.saveAll(src);
	}

	/**
	 * Retrieves all subjects with unique subject names.
	 * If multiple subjects have the same name, only one is chosen.
	 *
	 * @return List of Mon_Hoc with unique names
	 */
	public List<Mon_Hoc> getSubjectsWithUniqueNames(List<String> subjectNames) {
		// Get all subjects from the repository
		List<Mon_Hoc> allSubjects = monHocRepository.findAll();

		// Create a map to store unique subjects by name
		Map<String, Mon_Hoc> uniqueSubjectMap = new HashMap<>();

		// Iterate through all subjects
		for (Mon_Hoc subject : allSubjects) {
			String subjectName = subject.getTenMonHoc();

			// If this subject name is not already in our map, add it
			// This automatically keeps only the first occurrence of each subject name
			if (!uniqueSubjectMap.containsKey(subjectName)) {
				uniqueSubjectMap.put(subjectName, subject);
			}
		}

		// Convert map values to a list and return
		return new ArrayList<>(uniqueSubjectMap.values());
	}
}
