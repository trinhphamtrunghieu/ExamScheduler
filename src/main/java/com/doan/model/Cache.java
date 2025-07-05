package com.doan.model;

import com.doan.common.Helper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Cache {
	public volatile Map<String, Student> students = new HashMap<>();
	public volatile Map<String, Subject> subjects = new HashMap<>();
	public volatile Map<String, InClass> classes = new HashMap<>();
	public static Cache cache = new Cache();
	private final String dataFile = "./data.csv";

	private Cache(){};

	public void initialize() {
		File file = new File(dataFile);
		try {
			List<CSVRecord> records = Helper.parseCSVFromValidHeader(
					new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8),
					Set.of("MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên"));
			importAll(records, false);
		} catch (Exception e) {
			System.out.println("init cache failed. ex: " + e.toString());
//			System.exit(1);
		}
	}

	public void saveToDisk() {
		try (FileOutputStream fos = new FileOutputStream(dataFile)) {
			byte[] data = exportAll(); // Reuse existing method
			fos.write(data);
			fos.flush();
			System.out.println("Cache saved to disk at: " + System.currentTimeMillis());
		} catch (IOException e) {
			System.err.println("Failed to save cache to disk: " + e.getMessage());
		}
	}

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

	public void importAll(List<CSVRecord> records, boolean isAppend) throws IOException {

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
			studentID = record.get("MSSV");
			studentName = record.get("Họ tên");
			subjectID = record.get("Mã lớp học");
			subjectName = record.get("Môn học");
			teacher = record.get("Giáo viên");
			classID = record.get("Lớp");
			if (subjectName.isEmpty()) continue;
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
			student.assignToClass(inClass);
			student.registerSubject(subject);
		}
		cache = newCache;
	}
}