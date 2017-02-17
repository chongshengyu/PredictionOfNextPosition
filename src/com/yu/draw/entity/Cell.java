package com.yu.draw.entity;

public class Cell {
	private int gridX;
	private int gridY;
	public Cell(int gridX, int gridY) {
		super();
		this.gridX = gridX;
		this.gridY = gridY;
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
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + gridX;
		result = prime * result + gridY;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Cell other = (Cell) obj;
		if (gridX != other.gridX)
			return false;
		if (gridY != other.gridY)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "cell:"+this.gridX+","+this.gridY;
	}
	
}
