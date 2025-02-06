package com.doan.repository;

import com.doan.dto.Mon_Hoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Mon_Hoc_Repository extends JpaRepository<Mon_Hoc, String> {
	List<Mon_Hoc> findByMaMonHocIn(List<String> maMonHocList);
	List<Mon_Hoc> findByTenMonHocIn(List<String> tenMonHocList);
	Mon_Hoc findByMaMonHoc(String maMonHoc);
}
