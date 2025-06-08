package com.doan.model;

import java.util.ArrayList;
import java.util.List;

public class Subject {
	public String name;
	public String id;
	public String teacher;
	public List<Student> studentList = new ArrayList<>();

	public Subject(String id, String name, String teacher) {
		this.name = name;
		this.id = id;
		this.teacher = teacher;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Subject)) {
			return false;
		}
		Subject s = (Subject) obj;
		return s.id.equals(this.id);
	}
}
