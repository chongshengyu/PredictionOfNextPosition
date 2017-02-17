package com.yu.prepare.entity;

import com.yu.prepare.util.WgsToGcj;

public class GPSRecord {
	private String userId;
	private String longitude_wgs;
	private String latitude_wgs;
	private String longtitude_gcj;
	private String latitude_gcj;
	private String traNum;
	private String dateTime;
	
	public GPSRecord(String userId, String longitude_wgs, String latitude_wgs,
			String dateTime, String traNum) {
		this.userId = userId;
		this.longitude_wgs = longitude_wgs;
		this.latitude_wgs = latitude_wgs;
		this.dateTime = dateTime;
		this.traNum = traNum;
		GPSPoint point = WgsToGcj.transform(Double.parseDouble(longitude_wgs), Double.parseDouble(latitude_wgs));
		this.longtitude_gcj = String.valueOf(point.getLongitude());
		this.latitude_gcj = String.valueOf(point.getLatitude());
	}

	public String getLongitude_wgs() {
		return longitude_wgs;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setTraNum(String traNum) {
		this.traNum = traNum;
	}

	public void setLongitude_wgs(String longitude_wgs) {
		this.longitude_wgs = longitude_wgs;
	}

	public String getLatitude_wgs() {
		return latitude_wgs;
	}

	public void setLatitude_wgs(String latitude_wgs) {
		this.latitude_wgs = latitude_wgs;
	}

	public String getLongtitude_gcj() {
		return longtitude_gcj;
	}

	public void setLongtitude_gcj(String longtitude_gcj) {
		this.longtitude_gcj = longtitude_gcj;
	}

	public String getLatitude_gcj() {
		return latitude_gcj;
	}

	public void setLatitude_gcj(String latitude_gcj) {
		this.latitude_gcj = latitude_gcj;
	}

	public String getTraNum() {
		return traNum;
	}

	public void setAltitude(String traNum) {
		this.traNum = traNum;
	}

	public String getDateTime() {
		return dateTime;
	}

	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}
	
	
	
	
}
