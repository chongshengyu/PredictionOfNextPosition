package com.yu.draw.entity;

public class GPS {
	private String longitude;
	private String latitude;
	public GPS(String longitude, String latitude) {
		super();
		this.longitude = longitude;
		this.latitude = latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	@Override
	public String toString() {
		return "GPS [longitude=" + longitude + ", latitude=" + latitude + "]";
	}
	
}
