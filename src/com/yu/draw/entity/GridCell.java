package com.yu.draw.entity;

import com.yu.draw.util.ConvertDistGPS;
import com.yu.draw.util.GridUtil;

public class GridCell implements Comparable<GridCell>{
	private int gridX;
	private int gridY;
	private GPS luGps;
	private GPS ruGps;
	private GPS rdGps;
	private GPS ldGps;
	private int pointNumInCell;
	private GPS gridOriginGps;//grid原点坐标
	public GridCell(int gridX, int gridY, GPS gridOrigin) {
		this.gridX = gridX;
		this.gridY = gridY;
		this.gridOriginGps = gridOrigin;
		//先获得cell左下角的坐标
		GPS tmp = ConvertDistGPS.ConvertDistanceToLogLat(this.gridOriginGps, gridX * Parameter.cellWidth, 90);
		this.ldGps = ConvertDistGPS.ConvertDistanceToLogLat(tmp, gridY * Parameter.cellWidth, 0);
		//再根据左下角坐标获得其他角的坐标
		this.luGps = ConvertDistGPS.ConvertDistanceToLogLat(this.ldGps, Parameter.cellWidth, 0);
		this.ruGps = ConvertDistGPS.ConvertDistanceToLogLat(this.luGps, Parameter.cellWidth, 90);
		this.rdGps = ConvertDistGPS.ConvertDistanceToLogLat(this.ldGps, Parameter.cellWidth, 90);
	}
	public GPS getLuGps() {
		return luGps;
	}
	public void setLuGps(GPS luGps) {
		this.luGps = luGps;
	}
	public GPS getRuGps() {
		return ruGps;
	}
	public void setRuGps(GPS ruGps) {
		this.ruGps = ruGps;
	}
	public GPS getRdGps() {
		return rdGps;
	}
	public void setRdGps(GPS rdGps) {
		this.rdGps = rdGps;
	}
	public GPS getLdGps() {
		return ldGps;
	}
	public void setLdGps(GPS ldGps) {
		this.ldGps = ldGps;
	}
	public int getPointNumInCell() {
		return pointNumInCell;
	}
	public void setPointNumInCell(int pointNumInCell) {
		this.pointNumInCell = pointNumInCell;
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
	public String toString() {
		// TODO Auto-generated method stub
		return this.gridX + "," +this.gridY + ","+this.pointNumInCell;
	}
	public int compareTo(GridCell o) {
		if(this.pointNumInCell < o.pointNumInCell)
			return -1;
		return 1;
	}
	
}
