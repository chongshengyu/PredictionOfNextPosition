package com.yu.draw.entity;

public class RegionTime {
	private Region region;
	private String time;
	
	public RegionTime(){
		
	}

	public RegionTime(Region region, String time) {
		super();
		this.region = region;
		this.time = time;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
	
}
