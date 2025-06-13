package com.doan.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.sql.Time;

@Getter
@Setter
public class Schedule {

	private Integer id;
	public Date date;
	public String subjectName;
	public String room;
	public Time time;
	public Time endTime;
	private Subject subject;

	public Schedule() {}

	public Schedule(String ten_mon_hoc, Date ngay_thi, Time gio_thi, Subject monHoc, String phong_thi) {
		this.date = ngay_thi;
		this.time = gio_thi;
		this.subject = monHoc;
		this.room = "to be implement";
		this.subjectName = ten_mon_hoc;
		this.endTime = Time.valueOf(this.time.toLocalTime().plusMinutes(monHoc.duration));
	}

	public Schedule(Schedule lt) {
		this.id = lt.id;
		this.date = lt.date;
		this.subject = lt.subject;
		this.time = lt.time;
		this.room = lt.room;
		this.subjectName = lt.subjectName;
		this.endTime = lt.endTime;
	}

	@Override
	public String toString() {
		return "Schedule{" +
				"id=" + id +
				", date=" + date +
				", subjectName='" + subjectName + '\'' +
				", room='" + room + '\'' +
				", time=" + time +
				", endTime=" + endTime +
				", subject=" + subject +
				'}';
	}
}
