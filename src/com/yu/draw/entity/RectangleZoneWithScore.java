package com.yu.draw.entity;

public class RectangleZoneWithScore extends RectangleZone {
	private String score;
	public RectangleZoneWithScore(String lu_lng, String lu_lat, String rd_lng,
			String rd_lat, String score) {
		super(lu_lng, lu_lat, rd_lng, rd_lat);
		this.score = score;
	}
	
	public String getScore(){
		return score;
	}

}
