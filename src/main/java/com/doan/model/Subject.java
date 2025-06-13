package com.doan.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Subject {
	public String name;
	public String id;
	public String teacher;
	public Date startDate;
	public Date endDate;
	public Integer duration;

	@JsonIgnore
	public List<Student> studentList = new ArrayList<>();

	public Subject() {}

	public Subject(String id, String name, String teacher) {
		this.name = name;
		this.id = id;
		this.teacher = teacher;
		this.duration = 90;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Subject)) {
			return false;
		}
		Subject s = (Subject) obj;
		return s.name.equals(this.name);
	}

	@Override
	public int hashCode() {
		return this.name.toString().hashCode();
	}
}
