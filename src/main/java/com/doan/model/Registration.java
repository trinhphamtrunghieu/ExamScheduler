package com.doan.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Registration {
	private String ma_sinh_vien;
	private String ten_sinh_vien;
	private String studentClass;
	private String ma_mon_hoc;
	private String ten_mon_hoc;
	private String ten_giang_vien;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Registration that = (Registration) o;
		return Objects.equals(ma_sinh_vien, that.ma_sinh_vien) && Objects.equals(ten_sinh_vien, that.ten_sinh_vien) && Objects.equals(ten_mon_hoc, that.ten_mon_hoc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ma_mon_hoc.toString(), ma_sinh_vien.toString());
	}
}
