package com.doan.controller;

import com.doan.model.Registration;
import com.doan.model.Student;
import com.doan.model.Subject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Common {
	public static class Pair<K, V> {
		private final K key;
		private final V value;

		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}
	}

	public static byte[] exportRegistrations(List<Registration> registrationList) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL.withTrim().withFirstRecordAsHeader());

		// Write data
		printer.printRecord("STT", "MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên", "Lớp");
		int counter = 1;
		for (Registration registration : registrationList) {
			printer.printRecord(counter, registration.getMa_sinh_vien(), registration.getTen_sinh_vien(),
					registration.getMa_mon_hoc(), registration.getTen_mon_hoc(), registration.getTen_giang_vien(),
					registration.getStudentClass());
			counter++;
		}

		printer.flush();
		return out.toByteArray();
	}
}
