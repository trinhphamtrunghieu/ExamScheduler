package com.doan.common;

import com.doan.model.Cache;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelperTest {

	@Test
	void parseCsv_shouldMatchNormalizedVietnameseHeaders() throws Exception {
		String decomposedHoTen = Normalizer.normalize("Họ tên", Normalizer.Form.NFD);
		String csv = "MSSV," + decomposedHoTen + "\nSV01,Nguyễn Văn A\n";
		List<CSVRecord> records = Helper.parseCSVFromValidHeader(csv.getBytes(StandardCharsets.UTF_8),
				Set.of("MSSV", "Họ tên"));

		assertEquals(1, records.size());
		assertEquals("SV01", records.get(0).get("MSSV"));
		assertEquals("Nguyễn Văn A", records.get(0).get(decomposedHoTen));
	}

	@Test
	void parseCsv_shouldHandleUtf8BomAndVietnameseHeaders() throws Exception {
		String csv = "MSSV,Họ tên\nSV02,Trần Thị B\n";
		byte[] utf8Bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
		byte[] body = csv.getBytes(StandardCharsets.UTF_8);
		byte[] bytes = new byte[utf8Bom.length + body.length];
		System.arraycopy(utf8Bom, 0, bytes, 0, utf8Bom.length);
		System.arraycopy(body, 0, bytes, utf8Bom.length, body.length);
		Helper.ParsedCSV parsed = Helper.parseCSVFromValidHeader(bytes, Set.of("MSSV", "Họ tên"), null);

		assertEquals(1, parsed.records.size());
		assertEquals("SV02", Helper.getValue(parsed.records.get(0), parsed.resolvedHeaders, "MSSV"));
		assertEquals("Trần Thị B", Helper.getValue(parsed.records.get(0), parsed.resolvedHeaders, "Họ tên"));
	}

	@Test
	void parseCsv_shouldUseExplicitHeaderMapping() throws Exception {
		String csv = "StudentID,StudentName\nSV03,Lê Văn C\n";
		Map<String, String> mapping = Map.of("MSSV", "StudentID", "Họ tên", "StudentName");

		Helper.ParsedCSV parsed = Helper.parseCSVFromValidHeader(csv.getBytes(StandardCharsets.UTF_8),
				Set.of("MSSV", "Họ tên"), mapping);

		assertEquals(1, parsed.records.size());
		assertEquals("SV03", Helper.getValue(parsed.records.get(0), parsed.resolvedHeaders, "MSSV"));
		assertEquals("Lê Văn C", Helper.getValue(parsed.records.get(0), parsed.resolvedHeaders, "Họ tên"));
	}

	@Test
	void detectHeaders_shouldDecodeWindows1258VietnameseHeaders() throws Exception {
		String header = Normalizer.normalize("Họ tên", Normalizer.Form.NFD);
		String studentName = Normalizer.normalize("Phạm Văn D", Normalizer.Form.NFD);
		String csv = "MSSV," + header + "\nSV04," + studentName + "\n";
		byte[] bytes = csv.getBytes(Charset.forName("windows-1258"));

		List<String> headers = Helper.detectHeaders(bytes, Set.of("MSSV", "Họ tên"));

		assertEquals("MSSV", headers.get(0));
		assertEquals(-1, headers.get(1).indexOf('\uFFFD'));
		assertTrue(Normalizer.normalize(headers.get(1), Normalizer.Form.NFC).startsWith("Họ"));
	}

	@Test
	void parseTabularFile_shouldSkipHiddenXlsxRows() throws Exception {
		byte[] xlsxBytes = createXlsxWithHiddenDataRow();
		Map<String, String> resolvedHeaders = new LinkedHashMap<>();

		List<CSVRecord> records = Helper.parseTabularFileFromValidHeaderWithResolvedHeaders(
				"registrations.xlsx",
				xlsxBytes,
				Set.of("MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên"),
				null,
				resolvedHeaders
		);

		assertEquals(1, records.size());
		assertEquals("SV01", Helper.getValue(records.get(0), resolvedHeaders, "MSSV"));
	}

	@Test
	void importSummary_shouldCountOnlyVisibleNonEmptyRows() throws Exception {
		byte[] xlsxBytes = createXlsxWithHiddenDataRow();
		Map<String, String> resolvedHeaders = new LinkedHashMap<>();
		List<CSVRecord> records = Helper.parseTabularFileFromValidHeaderWithResolvedHeaders(
				"registrations.xlsx",
				xlsxBytes,
				Set.of("MSSV", "Họ tên", "Mã lớp học", "Môn học", "Giáo viên"),
				null,
				resolvedHeaders
		);

		Cache.ImportSummary summary = Cache.cache.importAll(records, false, resolvedHeaders);

		assertEquals(1, summary.totalLines);
		assertEquals(1, summary.importedLines);
	}

	private static byte[] createXlsxWithHiddenDataRow() throws Exception {
		try (Workbook workbook = new XSSFWorkbook();
		     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Sheet1");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("MSSV");
			header.createCell(1).setCellValue("Họ tên");
			header.createCell(2).setCellValue("Mã lớp học");
			header.createCell(3).setCellValue("Môn học");
			header.createCell(4).setCellValue("Giáo viên");

			Row visibleRow = sheet.createRow(1);
			visibleRow.createCell(0).setCellValue("SV01");
			visibleRow.createCell(1).setCellValue("Nguyễn Văn A");
			visibleRow.createCell(2).setCellValue("LHP001");
			visibleRow.createCell(3).setCellValue("Toán");
			visibleRow.createCell(4).setCellValue("GV A");

			Row hiddenRow = sheet.createRow(2);
			hiddenRow.createCell(0).setCellValue("SV02");
			hiddenRow.createCell(1).setCellValue("Trần Thị B");
			hiddenRow.createCell(2).setCellValue("LHP002");
			hiddenRow.createCell(3).setCellValue("Lý");
			hiddenRow.createCell(4).setCellValue("GV B");
			hiddenRow.setZeroHeight(true);

			workbook.write(outputStream);
			return outputStream.toByteArray();
		}
	}
}
