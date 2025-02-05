package com.doan.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@IdClass(Dang_Ky_Id.class)
@Table(name = "dang_ky")
@Getter
@Setter
public class Dang_Ky {
	@Id
	public String ma_sinh_vien;

	@Id
	@Column(name="ma_mon_hoc")
	public String maMonHoc;

	@ManyToOne
	@JoinColumn(name = "ma_sinh_vien", insertable = false, updatable = false)
	private Sinh_Vien sinhVien;

	@ManyToOne
	@JoinColumn(name = "ma_mon_hoc", insertable = false, updatable = false)
	private Mon_Hoc monHoc;
}
