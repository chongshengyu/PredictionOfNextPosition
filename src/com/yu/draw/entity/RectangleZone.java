package com.yu.draw.entity;

public class RectangleZone {
	private String recZoneStr;
	private String lu_lng;
	private String lu_lat;
	private String rd_lng;
	private String rd_lat;

	public RectangleZone(String lu_lng, String lu_lat, String rd_lng, String rd_lat) {
		this.lu_lng = lu_lng;
		this.lu_lat = lu_lat;
		this.rd_lng = rd_lng;
		this.rd_lat = rd_lat;
		this.recZoneStr = this.lu_lng + "," + this.lu_lat + "," + this.rd_lng
				+ "," + this.lu_lat + "," + this.rd_lng + "," + this.rd_lat
				+ "," + this.lu_lng + "," + this.rd_lat;// 左上，右上，左下，右下
	}

	public String getRecZoneStr() {
		return recZoneStr;
	}

	public void setRecZoneStr(String recZoneStr) {
		this.recZoneStr = recZoneStr;
	}

}
