package com.doan.model;

import com.doan.common.Helper;
import com.opencsv.CSVWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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

	private Cache() {
	}

	;

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
		try (FileOutputStream fos = new FileOutputStream(dataFile);
		     OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

			// Write UTF-8 BOM (Byte Order Mark) for better Windows compatibility
			fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

			// Use CSVPrinter directly with UTF-8 writer
			try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL.withTrim().withFirstRecordAsHeader())) {

				// Write header
				printer.printRecord("STT", "MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên", "LỚP", "NGÀY THI");

				// Write data
				int counter = 1;
				for (Student student : students.values()) {
					for (Subject subject : student.participateIn) {
						printer.printRecord(counter,
								student.id, student.name,
								subject.id, subject.name, subject.teacher,
								student.inClass.id, ""); // Empty exam date
						counter++;
					}
				}

				printer.flush();
				writer.flush();
			}

			System.out.println("Cache saved to disk at: " + System.currentTimeMillis());

		} catch (IOException e) {
			System.err.println("Failed to save cache to disk: " + e.getMessage());
			e.printStackTrace();
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
		StringWriter stringWriter = new StringWriter();
		CSVWriter csvWriter = new CSVWriter(stringWriter);
		String[] header = {"STT", "MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên", "LỚP", "NGÀY THI"};
		csvWriter.writeNext(header);

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
				String[] row = {
						String.valueOf(counter),
						String.valueOf(student.id),
						String.valueOf(student.name),
						String.valueOf(subject.id),
						String.valueOf(subject.name),
						String.valueOf(subject.teacher),
						String.valueOf(student.inClass.id),
				};
				csvWriter.writeNext(row);
			}
		}
		csvWriter.close();
		byte[] csvBytes = stringWriter.toString().getBytes(StandardCharsets.UTF_8);

		printer.flush();
		return csvBytes;
	}

	public byte[] exportAllXlsx() throws IOException {
		try (Workbook workbook = new XSSFWorkbook();
		     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Registrations");
			String[] header = {"STT", "MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên", "LỚP", "NGÀY THI"};

			Row headerRow = sheet.createRow(0);
			for (int col = 0; col < header.length; col++) {
				headerRow.createCell(col).setCellValue(header[col]);
			}

			int rowIndex = 1;
			int counter = 1;
			for (Student student : students.values()) {
				for (Subject subject : student.participateIn) {
					Row row = sheet.createRow(rowIndex++);
					row.createCell(0).setCellValue(counter++);
					row.createCell(1).setCellValue(student.id);
					row.createCell(2).setCellValue(student.name);
					row.createCell(3).setCellValue(subject.id);
					row.createCell(4).setCellValue(subject.name);
					row.createCell(5).setCellValue(subject.teacher);
					row.createCell(6).setCellValue(student.inClass.id);
					row.createCell(7).setCellValue("");
				}
			}

			workbook.write(outputStream);
			return outputStream.toByteArray();
		}
	}

	public void importAll(List<CSVRecord> records, boolean isAppend) throws IOException {
		importAll(records, isAppend, null);
	}

	public void importAll(List<CSVRecord> records, boolean isAppend, Map<String, String> resolvedHeaders) throws IOException {

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
			if (resolvedHeaders == null || resolvedHeaders.isEmpty()) {
				studentID = record.get("MSSV");
				studentName = record.get("Họ tên");
				subjectID = record.get("Mã lớp học");
				subjectName = record.get("Môn học");
				teacher = record.get("Giáo viên");
				classID = record.get("Mã lớp học");
			} else {
				studentID = Helper.getValue(record, resolvedHeaders, "MSSV");
				studentName = Helper.getValue(record, resolvedHeaders, "Họ tên");
				subjectID = Helper.getValue(record, resolvedHeaders, "Mã lớp học");
				subjectName = Helper.getValue(record, resolvedHeaders, "Môn học");
				teacher = Helper.getValue(record, resolvedHeaders, "Giáo viên");
				classID = Helper.getValue(record, resolvedHeaders, "Mã lớp học");
			}
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
