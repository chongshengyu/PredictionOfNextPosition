package com.yu.prepare.entity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;

public class TraRecord {
	private String userId;
	private String traNum;
	private String startTime;
	private String endTime;
	private String duration;
	private int pointNum;

	public TraRecord(String userId, String traNum, String startTime,
			String endTime, int pointNum) {
		super();
		this.userId = userId;
		this.traNum = traNum;
		this.startTime = startTime;
		this.endTime = endTime;
		this.duration = getDurationByStartEndTime(startTime, endTime);
		this.pointNum = pointNum;
	}

	public int getPointNum() {
		return pointNum;
	}

	public void setPointNum(int pointNum) {
		this.pointNum = pointNum;
	}

	private String getDurationByStartEndTime(String startTime, String endTime) {
		return String.valueOf(getTimeInMillis(endTime)
				- getTimeInMillis(startTime));
	}

	private long getTimeInMillis(String time) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try {
			date = simpleDateFormat.parse(time);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTime(date);
		return calendar.getTimeInMillis();
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTraNum() {
		return traNum;
	}

	public void setTraNum(String traNum) {
		this.traNum = traNum;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}
}
