package com.doan.controller;

import com.doan.model.Registration;
import com.doan.model.Student;
import com.doan.model.Subject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
		List<String[]> rows = new ArrayList<>();
		int counter = 1;
		for (Registration registration : registrationList) {
			rows.add(new String[] {
					String.valueOf(counter),
					registration.getMa_sinh_vien(),
					registration.getTen_sinh_vien(),
					registration.getMa_mon_hoc(),
					registration.getTen_mon_hoc(),
					registration.getTen_giang_vien(),
					registration.getStudentClass()
			});
			counter++;
		}
		return exportCsv(
				new String[] {"STT", "MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên", "Lớp"},
				rows
		);
	}

	public static byte[] exportRegistrationsXlsx(List<Registration> registrationList) throws IOException {
		List<String[]> rows = new ArrayList<>();
		int counter = 1;
		for (Registration registration : registrationList) {
			rows.add(new String[] {
					String.valueOf(counter),
					registration.getMa_sinh_vien(),
					registration.getTen_sinh_vien(),
					registration.getMa_mon_hoc(),
					registration.getTen_mon_hoc(),
					registration.getTen_giang_vien(),
					registration.getStudentClass()
			});
			counter++;
		}
		return exportXlsx(
				"Registrations",
				new String[] {"STT", "MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên", "Lớp"},
				rows
		);
	}

	public static byte[] exportCsv(String[] headers, List<String[]> rows) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
		OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT);
		printer.printRecord((Object[]) headers);
		for (String[] row : rows) {
			printer.printRecord((Object[]) row);
		}
		printer.flush();
		return out.toByteArray();
	}

	public static byte[] exportXlsx(String sheetName, String[] headers, List<String[]> rows) throws IOException {
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet(sheetName);
			Row headerRow = sheet.createRow(0);
			for (int i = 0; i < headers.length; i++) {
				headerRow.createCell(i).setCellValue(headers[i]);
			}

			int rowIndex = 1;
			for (String[] rowData : rows) {
				Row row = sheet.createRow(rowIndex++);
				for (int i = 0; i < rowData.length; i++) {
					row.createCell(i).setCellValue(rowData[i] == null ? "" : rowData[i]);
				}
			}

			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}
			workbook.write(out);
			return out.toByteArray();
		}
	}
}
