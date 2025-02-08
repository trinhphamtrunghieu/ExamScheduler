package com.doan.dto;

import com.doan.repository.Mon_Hoc_Repository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalTime;

@Getter
@Setter
public class Lich_Thi_DTO {
	@Autowired
	Mon_Hoc_Repository monHocRepository;
	private String ma_mon_hoc;
	private String ten_mon_hoc;
	private String ten_giang_vien;
	private Date ngay_thi;
	private Time gio_thi;
	private Time gio_ket_thuc;
	private String phong_thi;
	private Integer thoi_luong_thi;
	public Lich_Thi_DTO() {}
	public Lich_Thi_DTO(String ma_mon_hoc, String ten_mon_hoc, String ten_giang_vien, Date ngay_thi, Time gio_thi, String phong_thi, Integer thoi_luong_thi) {
		this.ma_mon_hoc = ma_mon_hoc;
		this.ten_mon_hoc = ten_mon_hoc;
		this.ten_giang_vien = ten_giang_vien;
		this.ngay_thi = ngay_thi;
		this.gio_thi = gio_thi;
		this.phong_thi = phong_thi;
		this.thoi_luong_thi = thoi_luong_thi;
	}

	public Lich_Thi_DTO(Lich_Thi lt) {
		this.ma_mon_hoc = lt.ma_mon_hoc;
		this.ten_mon_hoc = lt.getMonHoc().tenMonHoc;
		this.ten_giang_vien = lt.getMonHoc().ten_gv_dung_lop;
		this.ngay_thi = lt.ngay_thi;
		this.gio_thi = lt.gio_thi;
		this.phong_thi = lt.phong_thi;
		this.thoi_luong_thi = lt.getMonHoc().thoi_luong_thi;
		this.gio_ket_thuc = Time.valueOf(lt.gio_thi.toLocalTime().plusMinutes(thoi_luong_thi));
	}

	public Lich_Thi toLichThi() {
		Lich_Thi lt =  new Lich_Thi();
		lt.setPhong_thi(this.phong_thi);
		lt.setGio_thi(this.gio_thi);
		lt.setMa_mon_hoc(this.ma_mon_hoc);
		lt.setNgay_thi(this.ngay_thi);
		lt.setMonHoc(monHocRepository.findByMaMonHoc(this.ma_mon_hoc));
		return lt;
	}
}
