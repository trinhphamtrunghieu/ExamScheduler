package com.doan.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Setter
public class Lich_Thi_Option {
	private List<String> selectedSubjects;
	private String dayFrom;
	private String dayTo;
	private String hourFrom;
	private String hourTo;
	private int populationSize = 100;
	private double crossoverRate = 0.8;
	private double mutationRate = 0.1;
	private int maxGenerations = 500;
	private int convertTo24Hour(String time) {
		if (time != null && !time.isEmpty()) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
			LocalTime localTime = LocalTime.parse(time, formatter);
			return localTime.getHour();
		}
		return -1;  // Return -1 if invalid
	}
	public int getHourFromInt() {
		return convertTo24Hour(this.hourFrom);
	}
	public int getHourToInt() {
		return convertTo24Hour(this.hourTo);
	}
}
