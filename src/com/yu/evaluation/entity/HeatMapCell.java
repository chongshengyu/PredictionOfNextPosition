package com.yu.evaluation.entity;

public class HeatMapCell {
	public double lng;
	public double lat;
	public int count;
	
	
	
	public HeatMapCell(double lng, double lat, int count) {
		super();
		this.lng = lng;
		this.lat = lat;
		this.count = count;
	}
	public double getLng() {
		return lng;
	}
	public void setLng(double lng) {
		this.lng = lng;
	}
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	
	
	
	
	
}
