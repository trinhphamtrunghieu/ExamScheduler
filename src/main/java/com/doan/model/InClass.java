package com.doan.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class InClass {
	String id;

	@JsonIgnore
	List<Student> studentList = new ArrayList<Student>();

	public InClass() {}

	public InClass(String id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InClass)) {
			return false;
		}
		InClass s = (InClass) obj;
		return s.id.equals(this.id);
	}

}
