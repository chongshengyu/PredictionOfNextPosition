package com.yu.draw.entity;

public class GPSCell{
	private int gridX;
	private int gridY;
	private String cellTime;
	
	public GPSCell(int x, int y, String time){
		gridX = x;
		gridY = y;
		cellTime = time;
	}

	public int getGridX() {
		return gridX;
	}

	public void setGridX(int gridX) {
		this.gridX = gridX;
	}

	public int getGridY() {
		return gridY;
	}

	public void setGridY(int gridY) {
		this.gridY = gridY;
	}

	public String getCellTime() {
		return cellTime;
	}

	public void setCellTime(String cellTime) {
		this.cellTime = cellTime;
	}
	@Override
	public String toString() {
		return this.gridX + "," + this.gridY + "," + this.getCellTime();
	}
}
