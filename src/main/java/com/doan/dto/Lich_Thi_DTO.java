package com.doan.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalTime;

@Getter
@Setter
public class Lich_Thi_DTO {
//	private String ma_mon_hoc;
	private String ten_mon_hoc;
	private String ten_giang_vien;
	private Date ngay_thi;
	private Time gio_thi;
	private Time gio_ket_thuc;
	private String phong_thi;
	private Integer thoi_luong_thi;
	public Lich_Thi lichThi;
	public Lich_Thi_DTO() {}
	public Lich_Thi_DTO(String ma_mon_hoc, String ten_mon_hoc, String ten_giang_vien, Date ngay_thi, Time gio_thi, String phong_thi, Integer thoi_luong_thi) {
//		this.ma_mon_hoc = ma_mon_hoc;
		this.ten_mon_hoc = ten_mon_hoc;
		this.ten_giang_vien = ten_giang_vien;
		this.ngay_thi = ngay_thi;
		this.gio_thi = gio_thi;
		this.phong_thi = phong_thi;
		this.thoi_luong_thi = thoi_luong_thi;
		this.lichThi = new Lich_Thi();
		this.lichThi.setNgay_thi(ngay_thi);
		this.lichThi.setPhong_thi(phong_thi);
		this.lichThi.setGio_thi(gio_thi);
	}

	public Lich_Thi_DTO(Lich_Thi lt) {
//		this.ten_mon_hoc = lt.ten_mon_hoc;
		this.ten_mon_hoc = lt.ten_mon_hoc;
		this.ten_giang_vien = lt.getMonHoc().ten_gv_dung_lop;
		this.ngay_thi = lt.ngay_thi;
		this.gio_thi = lt.gio_thi;
		this.phong_thi = lt.phong_thi;
		this.thoi_luong_thi = lt.getMonHoc().thoi_luong_thi;
		this.gio_ket_thuc = Time.valueOf(lt.gio_thi.toLocalTime().plusMinutes(thoi_luong_thi));
		this.lichThi = lt;
	}
}
