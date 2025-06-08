package com.doan.model;

import java.util.ArrayList;
import java.util.List;

public class InClass {
	String id;
	List<Student> studentList = new ArrayList<Student>();

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
