package com.doan.dto;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.util.Objects;

public class Lich_Thi_Id implements Serializable {
	private String ten_mon_hoc;
	private Date ngay_thi;

	public Lich_Thi_Id() {}

	public Lich_Thi_Id(String ten_mon_hoc, Date ngay_thi) {
		this.ngay_thi = ngay_thi;
		this.ten_mon_hoc = ten_mon_hoc;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Lich_Thi_Id that = (Lich_Thi_Id) o;
		return Objects.equals(ngay_thi, that.ngay_thi) &&
				Objects.equals(ten_mon_hoc, that.ten_mon_hoc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ngay_thi, ten_mon_hoc);
	}
}
