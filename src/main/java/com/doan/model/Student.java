package com.doan.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Student {
	public String name;
	public String id;
	public InClass inClass;

	@JsonIgnore
	public List<Subject> participateIn = new ArrayList<>();

	@JsonIgnore
	public List<Registration> registrations = new ArrayList<>();

	public Student() {}

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
		if (this.inClass != null && this.inClass.studentList.contains(this)){
			this.inClass.studentList.remove(this);
		}
 		inClass.studentList.add(this);
		this.inClass = inClass;
	}

	public void registerSubject(Subject subject) {
		if (participateIn.contains(subject)) return;
		this.participateIn.add(subject);
		subject.studentList.add(this);
		Registration registration = new Registration();
		registration.setStudentClass(this.inClass.id);
		registration.setMa_sinh_vien(this.id);
		registration.setTen_sinh_vien(this.name);
		registration.setMa_mon_hoc(subject.id);
		registration.setTen_mon_hoc(subject.name);
		registration.setTen_giang_vien(subject.teacher);
		registrations.add(registration);
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
