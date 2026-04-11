package com.doan.common;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVPrinter;
import java.io.StringReader;
import java.util.stream.Collectors;
import java.util.*;

public class Helper {
	public static class ParsedCSV {
		public final List<CSVRecord> records;
		public final Map<String, String> resolvedHeaders;
		public final List<String> actualHeaders;

		public ParsedCSV(List<CSVRecord> records, Map<String, String> resolvedHeaders, List<String> actualHeaders) {
			this.records = records;
			this.resolvedHeaders = resolvedHeaders;
			this.actualHeaders = actualHeaders;
		}
	}

	private static final List<Charset> CANDIDATE_CHARSETS = List.of(
			StandardCharsets.UTF_8,
			Charset.forName("windows-1258"),
			Charset.forName("windows-1252")
	);

	public static List<CSVRecord> parseCSVFromValidHeader(InputStreamReader reader,
	                                                      Set<String> requiredHeaders) throws IOException {
		StringBuilder content = new StringBuilder();
		try (BufferedReader bufferedReader = new BufferedReader(reader)) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				content.append(line).append("\n");
			}
		}
		return parseCSVFromValidHeader(content.toString().getBytes(StandardCharsets.UTF_8), requiredHeaders, null).records;
	}

	public static ParsedCSV parseCSVFromValidHeader(byte[] fileBytes,
	                                               Set<String> requiredHeaders,
	                                               Map<String, String> requestedHeaderMapping) throws IOException {
		IllegalStateException lastError = null;
		for (Charset charset : CANDIDATE_CHARSETS) {
			try {
				return parseCSVWithCharset(fileBytes, requiredHeaders, requestedHeaderMapping, charset);
			} catch (IllegalStateException ex) {
				lastError = ex;
			}
		}
		throw (lastError != null) ? lastError : new IllegalStateException("No valid header line found");
	}

	private static ParsedCSV parseCSVWithCharset(byte[] fileBytes,
	                                            Set<String> requiredHeaders,
	                                            Map<String, String> requestedHeaderMapping,
	                                            Charset charset) throws IOException {
		List<String> allLines = new ArrayList<>();
		try (BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(fileBytes), charset))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				allLines.add(stripBom(line));
			}
		}
		HeaderLineInfo headerLineInfo = findHeaderLine(allLines, requiredHeaders, requestedHeaderMapping);
		String[] headers = headerLineInfo.headers.toArray(new String[0]);

		StringBuilder csvContent = new StringBuilder();
		for (int i = headerLineInfo.headerLineIndex; i < allLines.size(); i++) {
			csvContent.append(allLines.get(i)).append("\n");
		}
		String[] cleanedHeaders = Arrays.stream(headers)
				.map(String::trim)
				.filter(h -> !h.isEmpty())
				.toArray(String[]::new);
		try (Reader stringReader = new StringReader(csvContent.toString());
		     CSVParser parser = CSVFormat.DEFAULT
				     .withDelimiter(headerLineInfo.delimiter)
				     .withHeader(cleanedHeaders)
				     .withSkipHeaderRecord()
				     .withIgnoreHeaderCase()
				     .withTrim()
				     .parse(stringReader)) {
			Map<String, String> resolvedHeaders = resolveHeaders(requiredHeaders, headerLineInfo.headers, requestedHeaderMapping);
			List<String> actualHeaders = new ArrayList<>(headerLineInfo.headers);
			if (actualHeaders.isEmpty()) {
				actualHeaders = new ArrayList<>(parser.getHeaderMap().keySet());
			}
			return new ParsedCSV(parser.getRecords(), resolvedHeaders, actualHeaders);
		}
	}

	public static List<String> detectHeaders(byte[] fileBytes) throws IOException {
		return detectHeaders(fileBytes, null);
	}

	public static List<String> detectHeaders(byte[] fileBytes, Set<String> expectedHeaders) throws IOException {
		HeaderDetectionCandidate bestCandidate = null;
		boolean hasExpectedHeaders = expectedHeaders != null && !expectedHeaders.isEmpty();

		for (Charset charset : CANDIDATE_CHARSETS) {
			try {
				List<String> lines = new ArrayList<>();
				try (BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(new ByteArrayInputStream(fileBytes), charset))) {
					String line;
					while ((line = bufferedReader.readLine()) != null) {
						lines.add(stripBom(line));
					}
				}
				for (String line : lines) {
					if (line.trim().isEmpty()) continue;
					HeaderLineInfo info = parseLineToHeaderInfo(line);
					if (info.headers.stream().filter(h -> !h.isBlank()).count() < 2) continue;

					int expectedMatchCount = countExpectedHeaderMatches(expectedHeaders, info.headers);
					int replacementCharCount = countReplacementCharacters(info.headers);
					HeaderDetectionCandidate current = new HeaderDetectionCandidate(
							info.headers,
							expectedMatchCount,
							replacementCharCount
					);

					if (isBetterHeaderCandidate(current, bestCandidate, hasExpectedHeaders)) {
						bestCandidate = current;
					}

					if (hasExpectedHeaders
							&& expectedMatchCount == expectedHeaders.size()
							&& replacementCharCount == 0) {
						return info.headers;
					}
				}
			} catch (Exception ignored) {
			}
		}
		if (bestCandidate != null) {
			return bestCandidate.headers;
		}
		throw new IllegalStateException("Unable to detect CSV headers");
	}

	private static int countExpectedHeaderMatches(Set<String> expectedHeaders, List<String> actualHeaders) {
		if (expectedHeaders == null || expectedHeaders.isEmpty()) return 0;

		Map<String, String> normalizedToActual = new LinkedHashMap<>();
		for (String header : actualHeaders) {
			normalizedToActual.putIfAbsent(normalizeHeader(header), header);
		}

		int matches = 0;
		for (String expected : expectedHeaders) {
			if (normalizedToActual.containsKey(normalizeHeader(expected))) {
				matches++;
			}
		}
		return matches;
	}

	private static int countReplacementCharacters(List<String> headers) {
		int count = 0;
		for (String header : headers) {
			for (int i = 0; i < header.length(); i++) {
				if (header.charAt(i) == '\uFFFD') {
					count++;
				}
			}
		}
		return count;
	}

	private static boolean isBetterHeaderCandidate(HeaderDetectionCandidate current,
	                                               HeaderDetectionCandidate existing,
	                                               boolean hasExpectedHeaders) {
		if (existing == null) return true;

		if (hasExpectedHeaders && current.expectedMatchCount != existing.expectedMatchCount) {
			return current.expectedMatchCount > existing.expectedMatchCount;
		}
		if (current.replacementCharCount != existing.replacementCharCount) {
			return current.replacementCharCount < existing.replacementCharCount;
		}
		if (current.headers.size() != existing.headers.size()) {
			return current.headers.size() > existing.headers.size();
		}
		return false;
	}

	private static HeaderLineInfo findHeaderLine(List<String> allLines,
	                                            Set<String> requiredHeaders,
	                                            Map<String, String> requestedHeaderMapping) throws IOException {
		for (int i = 0; i < allLines.size(); i++) {
			String raw = allLines.get(i);
			if (raw.trim().isEmpty()) continue;
			HeaderLineInfo parsedLine = parseLineToHeaderInfo(raw);
			try {
				resolveHeaders(requiredHeaders, parsedLine.headers, requestedHeaderMapping);
				parsedLine.headerLineIndex = i;
				return parsedLine;
			} catch (IllegalStateException ignored) {
			}
		}
		throw new IllegalStateException("No valid header line found with required headers: " + requiredHeaders);
	}

	private static HeaderLineInfo parseLineToHeaderInfo(String line) throws IOException {
		HeaderLineInfo info = new HeaderLineInfo();
		char delimiter = countChar(line, ';') > countChar(line, ',') ? ';' : ',';
		info.delimiter = delimiter;
		try (CSVParser parser = CSVParser.parse(stripBom(line), CSVFormat.DEFAULT.withDelimiter(delimiter))) {
			List<CSVRecord> records = parser.getRecords();
			if (!records.isEmpty()) {
				CSVRecord record = records.get(0);
				for (String value : record) {
					info.headers.add(stripBom(value).trim());
				}
			}
		}
		return info;
	}

	private static Map<String, String> resolveHeaders(Set<String> requiredHeaders,
	                                                 List<String> actualHeaders,
	                                                 Map<String, String> requestedHeaderMapping) {
		Map<String, String> normalizedToActual = new LinkedHashMap<>();
		for (String header : actualHeaders) {
			normalizedToActual.putIfAbsent(normalizeHeader(header), header);
		}

		Map<String, String> result = new LinkedHashMap<>();
		for (String expected : requiredHeaders) {
			String mapped = null;
			if (requestedHeaderMapping != null) {
				mapped = requestedHeaderMapping.get(expected);
			}
			String resolved = resolveSingleHeader(expected, mapped, actualHeaders, normalizedToActual);
			// if (resolved == null) {
			// 	throw new IllegalStateException("Missing required header: " + expected);
			// }
			result.put(expected, resolved);
		}
		System.out.println(result);
		return result;
	}

	private static String resolveSingleHeader(String expected,
	                                         String mappedHeader,
	                                         List<String> actualHeaders,
	                                         Map<String, String> normalizedToActual) {
		if (mappedHeader != null && !mappedHeader.trim().isEmpty()) {
			for (String actual : actualHeaders) {
				if (actual.equals(mappedHeader.trim())) return actual;
			}
			String byNormalized = normalizedToActual.get(normalizeHeader(mappedHeader));
			if (byNormalized != null) return byNormalized;
		}
		return normalizedToActual.get(normalizeHeader(expected));
	}

	private static String normalizeHeader(String input) {
		if (input == null) return "";
		String noBom = stripBom(input).trim().toLowerCase();
		String normalized = Normalizer.normalize(noBom, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		normalized = normalized.replaceAll("[^\\p{Alnum}]+", " ");
		return normalized.trim().replaceAll("\\s+", " ");
	}

	private static String stripBom(String value) {
		if (value == null || value.isEmpty()) return value;
		return value.charAt(0) == '\uFEFF' ? value.substring(1) : value;
	}

	private static int countChar(String input, char c) {
		int total = 0;
		for (int i = 0; i < input.length(); i++) {
			if (input.charAt(i) == c) total++;
		}
		return total;
	}

	private static class HeaderLineInfo {
		int headerLineIndex = -1;
		char delimiter = ',';
		List<String> headers = new ArrayList<>();
	}

	private static class HeaderDetectionCandidate {
		List<String> headers;
		int expectedMatchCount;
		int replacementCharCount;

		HeaderDetectionCandidate(List<String> headers, int expectedMatchCount, int replacementCharCount) {
			this.headers = headers;
			this.expectedMatchCount = expectedMatchCount;
			this.replacementCharCount = replacementCharCount;
		}
	}

	public static String getValue(CSVRecord record, Map<String, String> resolvedHeaders, String expectedHeader) {
		String actualHeader = resolvedHeaders.get(expectedHeader);
		if (actualHeader == null) {
			throw new IllegalStateException("Header mapping missing for expected header: " + expectedHeader);
		}
		try {
			return record.get(actualHeader);
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("Could not read mapped header: " + actualHeader);
		}
	}

	public static Map<String, String> parseHeaderMappingString(String headerMappingRaw) {
		Map<String, String> mapping = new LinkedHashMap<>();
		if (headerMappingRaw == null || headerMappingRaw.trim().isEmpty()) {
			return mapping;
		}
		String[] entries = headerMappingRaw.split(";");
		for (String entry : entries) {
			String[] pair = entry.split("=", 2);
			if (pair.length != 2) continue;
			String expected = pair[0].trim();
			String actual = pair[1].trim();
			if (!expected.isEmpty() && !actual.isEmpty()) {
				mapping.put(expected, actual);
			}
		}
		return mapping;
	}

	public static String serializeHeaderMapping(Map<String, String> headerMapping) {
		if (headerMapping == null || headerMapping.isEmpty()) return "";
		return headerMapping.entrySet().stream()
				.map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining(";"));
	}

	public static List<CSVRecord> parseCSVFromValidHeader(byte[] fileBytes,
	                                                      Set<String> requiredHeaders) throws IOException {
		return parseCSVFromValidHeader(fileBytes, requiredHeaders, null).records;
	}

	public static List<CSVRecord> parseCSVFromValidHeader(InputStreamReader reader,
	                                                      Set<String> requiredHeaders,
	                                                      Map<String, String> requestedHeaderMapping) throws IOException {
		StringBuilder content = new StringBuilder();
		try (BufferedReader bufferedReader = new BufferedReader(reader)) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				content.append(line).append("\n");
			}
		}
		return parseCSVFromValidHeader(content.toString().getBytes(StandardCharsets.UTF_8), requiredHeaders, requestedHeaderMapping).records;
	}

	public static List<CSVRecord> parseCSVFromValidHeaderWithResolvedHeaders(byte[] fileBytes,
	                                                                         Set<String> requiredHeaders,
	                                                                         Map<String, String> requestedHeaderMapping,
	                                                                         Map<String, String> resolvedHeadersOut) throws IOException {
		ParsedCSV parsed = parseCSVFromValidHeader(fileBytes, requiredHeaders, requestedHeaderMapping);
		resolvedHeadersOut.putAll(parsed.resolvedHeaders);
		return parsed.records;
	}

	public static List<CSVRecord> parseTabularFileFromValidHeaderWithResolvedHeaders(String fileName,
	                                                                                  byte[] fileBytes,
	                                                                                  Set<String> requiredHeaders,
	                                                                                  Map<String, String> requestedHeaderMapping,
	                                                                                  Map<String, String> resolvedHeadersOut) throws IOException {
		byte[] normalizedFileBytes = isXlsxFile(fileName)
				? convertXlsxToCsvBytes(fileBytes, requiredHeaders, requestedHeaderMapping)
				: fileBytes;
		return parseCSVFromValidHeaderWithResolvedHeaders(
				normalizedFileBytes,
				requiredHeaders,
				requestedHeaderMapping,
				resolvedHeadersOut
		);
	}

	public static List<String> detectHeadersFromFile(String fileName, byte[] fileBytes, Set<String> expectedHeaders) throws IOException {
		if (isXlsxFile(fileName)) {
			return detectXlsxHeaders(fileBytes, expectedHeaders);
		}
		return detectHeaders(fileBytes, expectedHeaders);
	}

	private static boolean isXlsxFile(String fileName) {
		if (fileName == null || fileName.isBlank()) return false;
		return fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx");
	}

	private static List<String> detectXlsxHeaders(byte[] fileBytes, Set<String> expectedHeaders) throws IOException {
		List<List<String>> rows = readXlsxRows(fileBytes);
		List<String> bestHeaders = null;
		int bestExpectedMatch = -1;
		int bestHeaderSize = -1;
		boolean hasExpectedHeaders = expectedHeaders != null && !expectedHeaders.isEmpty();

		for (List<String> row : rows) {
			List<String> cleanedHeaders = cleanHeaders(row);
			if (cleanedHeaders.size() < 2) continue;

			int expectedMatchCount = countExpectedHeaderMatches(expectedHeaders, cleanedHeaders);
			if (hasExpectedHeaders && expectedMatchCount == expectedHeaders.size()) {
				return cleanedHeaders;
			}

			if (expectedMatchCount > bestExpectedMatch
					|| (expectedMatchCount == bestExpectedMatch && cleanedHeaders.size() > bestHeaderSize)) {
				bestHeaders = cleanedHeaders;
				bestExpectedMatch = expectedMatchCount;
				bestHeaderSize = cleanedHeaders.size();
			}
		}

		if (bestHeaders != null) {
			return bestHeaders;
		}
		throw new IllegalStateException("Unable to detect XLSX headers");
	}

	private static byte[] convertXlsxToCsvBytes(byte[] fileBytes,
	                                            Set<String> requiredHeaders,
	                                            Map<String, String> requestedHeaderMapping) throws IOException {
		List<List<String>> rows = readXlsxRows(fileBytes);
		int headerRowIndex = findXlsxHeaderRowIndex(rows, requiredHeaders, requestedHeaderMapping);
		List<String> headers = cleanHeaders(rows.get(headerRowIndex));
		if (headers.isEmpty()) {
			throw new IllegalStateException("Unable to find headers in XLSX file");
		}

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		     OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
		     CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			printer.printRecord(headers);
			for (int rowIndex = headerRowIndex + 1; rowIndex < rows.size(); rowIndex++) {
				List<String> rowValues = rows.get(rowIndex);
				List<String> record = new ArrayList<>();
				boolean hasData = false;
				for (int col = 0; col < headers.size(); col++) {
					String value = col < rowValues.size() ? rowValues.get(col).trim() : "";
					if (!value.isEmpty()) hasData = true;
					record.add(value);
				}
				if (hasData) {
					printer.printRecord(record);
				}
			}
			printer.flush();
			return outputStream.toByteArray();
		}
	}

	private static int findXlsxHeaderRowIndex(List<List<String>> rows,
	                                          Set<String> requiredHeaders,
	                                          Map<String, String> requestedHeaderMapping) {
		int bestIndex = -1;
		int bestExpectedMatch = -1;
		int bestHeaderSize = -1;
		boolean hasExpectedHeaders = requiredHeaders != null && !requiredHeaders.isEmpty();

		for (int i = 0; i < rows.size(); i++) {
			List<String> cleanedHeaders = cleanHeaders(rows.get(i));
			if (cleanedHeaders.size() < 2) continue;

			try {
				resolveHeaders(requiredHeaders, cleanedHeaders, requestedHeaderMapping);
				int expectedMatchCount = countExpectedHeaderMatches(requiredHeaders, cleanedHeaders);
				if (hasExpectedHeaders && expectedMatchCount == requiredHeaders.size()) {
					return i;
				}
				if (expectedMatchCount > bestExpectedMatch
						|| (expectedMatchCount == bestExpectedMatch && cleanedHeaders.size() > bestHeaderSize)) {
					bestIndex = i;
					bestExpectedMatch = expectedMatchCount;
					bestHeaderSize = cleanedHeaders.size();
				}
			} catch (IllegalStateException ignored) {
			}
		}

		if (bestIndex >= 0) return bestIndex;
		throw new IllegalStateException("No valid XLSX header line found");
	}

	private static List<List<String>> readXlsxRows(byte[] fileBytes) throws IOException {
		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
			Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
			if (sheet == null) {
				throw new IllegalStateException("XLSX file does not contain sheets");
			}

			List<List<String>> rows = new ArrayList<>();
			DataFormatter formatter = new DataFormatter();
			for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row != null && row.getZeroHeight()) {
					continue;
				}
				if (row == null || row.getLastCellNum() < 0) {
					rows.add(new ArrayList<>());
					continue;
				}
				List<String> values = new ArrayList<>();
				for (int colIndex = 0; colIndex < row.getLastCellNum(); colIndex++) {
					Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
					String value = cell == null ? "" : formatter.formatCellValue(cell);
					values.add(stripBom(value).trim());
				}
				rows.add(values);
			}
			return rows;
		}
	}

	private static List<String> cleanHeaders(List<String> headers) {
		List<String> cleanedHeaders = new ArrayList<>();
		for (String header : headers) {
			String cleaned = stripBom(header).trim();
			if (!cleaned.isEmpty()) {
				cleanedHeaders.add(cleaned);
			}
		}
		return cleanedHeaders;
	}

}
