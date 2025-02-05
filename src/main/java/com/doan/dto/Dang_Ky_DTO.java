package com.doan.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Dang_Ky_DTO {
	private String ma_sinh_vien;
	private String ten_sinh_vien;
	private String ma_mon_hoc;
	private String ten_mon_hoc;
	private String ten_giang_vien;

	public Dang_Ky_DTO(String ma_sinh_vien, String ten_sinh_vien, String ma_mon_hoc, String ten_mon_hoc, String ten_giang_vien) {
		this.ma_sinh_vien = ma_sinh_vien;
		this.ten_sinh_vien = ten_sinh_vien;
		this.ma_mon_hoc = ma_mon_hoc;
		this.ten_mon_hoc = ten_mon_hoc;
		this.ten_giang_vien = ten_giang_vien;
	}
}
