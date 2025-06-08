package com.doan.model;

import java.util.ArrayList;
import java.util.List;

public class Student {
	public String name;
	public String id;
	public InClass inClass;

	public List<Subject> participateIn = new ArrayList<>();

	public Student(String id, String name) {
		this.name = name;
		this.id = id;
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		for (Subject s : participateIn) {
			String studentInfo = "%s,%s";
			res.append(String.format(studentInfo, this.id, this.name)).append(",")
					.append(s.id).append(",").append(s.name).append(",")
					.append(inClass.id).append(",")
					.append(participateIn.size())
					.append("\n");
		}
		return res.toString();
	}


	public void assignToClass(InClass inClass) {
		inClass.studentList.add(this);
		this.inClass = inClass;
	}

	public void registerSubject(Subject subject) {
		if (participateIn.contains(subject)) return;
		this.participateIn.add(subject);
		subject.studentList.add(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Student)) {
			return false;
		}
		Student s = (Student) obj;
		return s.id.equals(this.id) && s.name.equals(this.name);
	}
}
