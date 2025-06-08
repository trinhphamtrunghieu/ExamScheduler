package com.doan.model;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cache {
	public volatile Map<String, Student> students = new HashMap<>();
	public volatile Map<String, Subject> subjects = new HashMap<>();
	public volatile Map<String, InClass> classes = new HashMap<>();
	public static Cache cache = new Cache();

	private Cache(){};

	public void addStudent(Student s) {
		students.putIfAbsent(s.id, s);
	}

	public void addStudent(List<Student> s) {
		for (Student obj : s) {
			addStudent(obj);
		}
	}

	public void addSubjects(Subject s) {
		subjects.putIfAbsent(s.id, s);
	}

	public void addSubjects(List<Subject> s) {
		for (Subject obj : s) {
			addSubjects(obj);
		}
	}

	public void addClass(InClass s) {
		classes.putIfAbsent(s.id, s);
	}

	public void addClass(List<InClass> s) {
		for (InClass obj : s) {
			addClass(obj);
		}
	}

	public byte[] exportAll() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL.withTrim().withFirstRecordAsHeader());

		// Write data
		printer.printRecord("STT", "MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên", "LỚP", "NGÀY THI");
		int counter = 1;
		for (Student student : students.values()) {
			for (Subject subject : student.participateIn) {
				printer.printRecord(counter,
						student.id, student.name,
						subject.id, subject.name, subject.teacher,
						student.inClass.id);
				counter++;
			}
		}

		printer.flush();
		return out.toByteArray();
	}

	public void importAll(InputStream csvInputStream, boolean isAppend) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(csvInputStream, StandardCharsets.UTF_8));
		CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL.withFirstRecordAsHeader().withTrim());

		List<CSVRecord> records = parser.getRecords();

		Cache newCache;
		if (isAppend) {
			newCache = this.cache;
		} else {
			newCache = new Cache();
		}
		for (int i = 0; i < records.size(); i++) {
			CSVRecord record = records.get(i);
			//STT, MSSV, Họ tên, Mã lớp học, Môn học, Giáo viên, Lớp
			String studentID, studentName, subjectID, subjectName, teacher, classID;
			studentID = record.get(1);
			studentName = record.get(2);
			subjectID = record.get(3);
			subjectName = record.get(4);
			teacher = record.get(5);
			classID = record.get(6);
			Student student = newCache.students.get(studentID);
			if (student == null) {
				student = new Student(studentID, studentName);
				newCache.addStudent(student);
			}
			Subject subject = newCache.subjects.get(subjectID);
			if (subject == null) {
				subject = new Subject(subjectID, subjectName, teacher);
				newCache.addSubjects(subject);
			}
			InClass inClass = newCache.classes.get(classID);
			if (inClass == null) {
				inClass = new InClass(classID);
				newCache.addClass(inClass);
			}
			student.registerSubject(subject);
			student.assignToClass(inClass);
		}
		parser.close();
		cache = newCache;
	}
}