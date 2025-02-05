package com.doan.dto;

import jakarta.persistence.*;

import java.sql.Date;
import java.util.List;

@Entity
@Table(name = "mon_hoc")
public class Mon_Hoc {

	@Id
	@Column(name = "ma_mon_hoc")
	public String maMonHoc;

	public String ten_gv_dung_lop;
	public Date ngay_bat_dau;
	public Date ngay_ket_thuc;
	public Integer thoi_luong_thi;
	@Column(name = "ten_mon_hoc")
	public String tenMonHoc;

	@OneToMany(mappedBy = "monHoc")
	private List<Dang_Ky> registrations;
}
