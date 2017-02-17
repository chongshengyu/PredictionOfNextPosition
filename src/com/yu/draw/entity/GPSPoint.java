package com.yu.draw.entity;

public class GPSPoint {
	private String lang;
	private String lat;
	private String dateTime;
	public GPSPoint(String lang, String lat, String dateTime) {
		super();
		this.lang = lang;
		this.lat = lat;
		this.dateTime = dateTime;
	}
	public String getLang() {
		return lang;
	}
	public void setLang(String lang) {
		this.lang = lang;
	}
	public String getLat() {
		return lat;
	}
	public void setLat(String lat) {
		this.lat = lat;
	}
	public String getDateTime() {
		return dateTime;
	}
	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}
	
}
