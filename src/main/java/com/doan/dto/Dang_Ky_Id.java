package com.doan.dto;

import java.io.Serializable;
import java.util.Objects;

public class Dang_Ky_Id implements Serializable {
	private String ma_sinh_vien;
	private String maMonHoc;

	public Dang_Ky_Id() {}

	public Dang_Ky_Id(String ma_sinh_vien, String maMonHoc) {
		this.ma_sinh_vien = ma_sinh_vien;
		this.maMonHoc = maMonHoc;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Dang_Ky_Id that = (Dang_Ky_Id) o;
		return Objects.equals(ma_sinh_vien, that.ma_sinh_vien) &&
				Objects.equals(maMonHoc, that.maMonHoc);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ma_sinh_vien, maMonHoc);
	}
}
