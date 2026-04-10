package com.doan.dto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="sinh_vien")
@Getter
@Setter
public class Sinh_Vien {
	@Id
	private String ma_sinh_vien;

	private String ten_sinh_vien;

//	@OneToMany(mappedBy = "sinhVien")
//	private List<Dang_Ky> registrations;

}
