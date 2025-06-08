package com.doan.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.sql.Time;

@Entity
@Table(name = "lich_thi")
@Getter
@Setter
public class Lich_Thi {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)  // auto-increment id
	private Integer id;

	@Column(name = "ngay_thi", nullable = false)
	public Date ngay_thi;

	@Column(name = "ten_mon_hoc", nullable = false)
	public String ten_mon_hoc;

	@Column(name = "phong_thi", nullable = false)
	public String phong_thi;

	@Column(name = "gio_thi", nullable = false)
	public Time gio_thi;

	@ManyToOne
	@JoinColumn(name = "ten_mon_hoc", insertable = false, updatable = false)
	private Mon_Hoc monHoc;

	public Lich_Thi() {}

	public Lich_Thi(String ten_mon_hoc, Date ngay_thi, Time gio_thi, Mon_Hoc monHoc, String phong_thi) {
		this.ngay_thi = ngay_thi;
		this.gio_thi = gio_thi;
		this.monHoc = monHoc;
		this.phong_thi = "to be implement";
		this.ten_mon_hoc = ten_mon_hoc;
	}

	public Lich_Thi(Lich_Thi lt) {
		this.ngay_thi = lt.ngay_thi;
		this.monHoc = lt.monHoc;
		this.gio_thi = lt.gio_thi;
		this.phong_thi = lt.phong_thi;
		this.ten_mon_hoc = lt.ten_mon_hoc;
	}
}
