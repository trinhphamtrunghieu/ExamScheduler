package com.doan.common;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.io.StringReader;
import java.util.stream.Collectors;
import java.util.Arrays;
public class Helper {
	public static List<CSVRecord> parseCSVFromValidHeader(InputStreamReader reader,
	                                                      Set<String> requiredHeaders) throws IOException {
		List<String> allLines = new ArrayList<>();
		BufferedReader bufferedReader = new BufferedReader(reader);
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			allLines.add(line);
		}

		// Normalize required headers to lower case
		Set<String> lowerRequiredHeaders = requiredHeaders.stream()
				.map(String::toLowerCase)
				.collect(Collectors.toSet());

		int headerLineIndex = -1;
		String[] headers = null;

		for (int i = 0; i < allLines.size(); i++) {
			String raw_str = new String(allLines.get(i).getBytes(), StandardCharsets.UTF_8);
			String[] columns = raw_str.split(",");

			Set<String> lowerColumns = Arrays.stream(columns)
					.map(String::trim)
					.map(String::toLowerCase)
					.map(String::toString)
					.collect(Collectors.toSet());
			System.out.println("line: " + i + ": " + raw_str);
			System.out.println("required: " + i + ": " + lowerRequiredHeaders);
			if (lowerColumns.containsAll(lowerRequiredHeaders)) {
				headers = Arrays.stream(columns).map(String::trim).toArray(String[]::new);
				headerLineIndex = i;
				break;
			}
		}

		if (headers == null) {
			throw new IllegalStateException("No valid header line found with required headers: " + requiredHeaders);
		}

		// Reconstruct content from header line onward
		StringBuilder csvContent = new StringBuilder();
		for (int i = headerLineIndex; i < allLines.size(); i++) {
			csvContent.append(allLines.get(i)).append("\n");
		}

		// Parse using Apache Commons CSV
		try (Reader stringReader = new StringReader(csvContent.toString());
		     CSVParser parser = CSVFormat.DEFAULT
				     .withHeader(headers)
				     .withSkipHeaderRecord()
				     .withIgnoreHeaderCase()
				     .withTrim()
				     .parse(stringReader)) {

			return parser.getRecords();
		}
	}
}
