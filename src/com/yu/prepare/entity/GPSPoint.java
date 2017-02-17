package com.yu.prepare.entity;

public class GPSPoint {
	private double longitude;
	private double latitude;
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double langitude) {
		this.longitude = langitude;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public GPSPoint(double langitude, double latitude) {
		super();
		this.longitude = langitude;
		this.latitude = latitude;
	}
	
}
