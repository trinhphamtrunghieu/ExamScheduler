package com.doan.repository;

import com.doan.dto.Dang_Ky;
import com.doan.dto.Dang_Ky_DTO;
import com.doan.dto.Lich_Thi;
import com.doan.dto.Lich_Thi_DTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Lich_Thi_Repository extends JpaRepository<Lich_Thi, String> {
	@Query("SELECT new com.doan.dto.Lich_Thi_DTO(l.monHoc.maMonHoc, l.monHoc.tenMonHoc, l.monHoc.ten_gv_dung_lop, l.ngay_thi, l.gio_thi, l.phong_thi, l.monHoc.thoi_luong_thi) from Lich_Thi l")
	List<Lich_Thi_DTO> findAllWithDetail();
}
