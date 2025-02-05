package com.doan.repository;

import com.doan.dto.Dang_Ky;
import com.doan.dto.Dang_Ky_DTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Dang_Ky_Repository extends JpaRepository<Dang_Ky, String> {
	@Query("SELECT new com.doan.dto.Dang_Ky_DTO(d.sinhVien.ma_sinh_vien, d.sinhVien.ten_sinh_vien, d.monHoc.maMonHoc, d.monHoc.tenMonHoc, d.monHoc.ten_gv_dung_lop) from Dang_Ky d")
	List<Dang_Ky_DTO> findAllWithDetail();

	@Query("SELECT new com.doan.dto.Dang_Ky_DTO(d.sinhVien.ma_sinh_vien, d.sinhVien.ten_sinh_vien, d.monHoc.maMonHoc, d.monHoc.tenMonHoc, d.monHoc.ten_gv_dung_lop) from Dang_Ky d where d.ma_sinh_vien = ?1")
	List<Dang_Ky_DTO> findByMSV(String id);

	List<Dang_Ky> findDangKyByMaMonHocIn(List<String> maMonHocList);
	List<Dang_Ky> findDangKyByMaMonHoc(String maMonHocList);
}
