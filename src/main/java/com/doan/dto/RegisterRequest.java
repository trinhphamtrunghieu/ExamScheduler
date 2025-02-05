package com.doan.dto;

import java.util.List;

public class RegisterRequest {
	private String maSinhVien;
	private List<String> courseIds;

	public String getMaSinhVien() { return maSinhVien; }
	public void setMaSinhVien(String maSinhVien) { this.maSinhVien = maSinhVien; }

	public List<String> getCourseIds() { return courseIds; }
	public void setCourseIds(List<String> courseIds) { this.courseIds = courseIds; }
}