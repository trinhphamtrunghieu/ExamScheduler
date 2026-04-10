package com.doan.common;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
