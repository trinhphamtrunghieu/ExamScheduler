package com.doan.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.sql.Time;

@Entity
@IdClass(Lich_Thi_Id.class)
@Table(name = "lich_thi")
@Getter
@Setter
public class Lich_Thi {
	@Id
	public Date ngay_thi;

	@Id
	public String ma_mon_hoc;

	public String phong_thi;
	public Time gio_thi;

	@ManyToOne
	@JoinColumn(name = "ma_mon_hoc", insertable = false, updatable = false)
	private Mon_Hoc monHoc;

	public Lich_Thi() {}

	public Lich_Thi(String ma_mon_hoc, Date ngay_thi, Time gio_thi, Mon_Hoc monHoc, String phong_thi) {
		this.ngay_thi = ngay_thi;
		this.ma_mon_hoc = ma_mon_hoc;
		this.gio_thi = gio_thi;
		this.monHoc = monHoc;
		this.phong_thi = "to be implement";
	}

	public Lich_Thi(Lich_Thi lt) {
		this.ngay_thi = lt.ngay_thi;
		this.monHoc = lt.monHoc;
		this.gio_thi = lt.gio_thi;
		this.phong_thi = lt.phong_thi;
		this.ma_mon_hoc = lt.ma_mon_hoc;
	}
}
