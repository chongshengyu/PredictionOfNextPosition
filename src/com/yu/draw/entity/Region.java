package com.yu.draw.entity;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class Region {
	private String label;//区域的标号，可用所包含的cell的坐标拼接表示，如R-0000-0001（包含两个cell，分别为0000,0001）
	private GPS luGps;
	private GPS ldGps;
	private GPS ruGps;
	private GPS rdGps;
	private LinkedList<GridCell> cellsContainedList;//区域所包含的cell list
	private int avgPointNum;//cells的平均点数
	
	public Region(String label, int avgPointNum, LinkedList<GridCell> list) {
		this.label = label;
		this.avgPointNum = avgPointNum;
		cellsContainedList = list;
		//得到四个角的cell坐标
		int initalX = list.get(0).getGridX();
		int initalY = list.get(0).getGridY();
		Cell luCell = new Cell(initalX, initalY);
		Cell ruCell = new Cell(initalX, initalY);
		Cell ldCell = new Cell(initalX, initalY);
		Cell rdCell = new Cell(initalX, initalY);
		//左上角
		for(GridCell gc:list){
			if(gc.getGridX() <= luCell.getGridX() && gc.getGridY() >= luCell.getGridY()){//X更小或者Y更高
				luCell = new Cell(gc.getGridX(), gc.getGridY());
			}
		}
		//右上角
		for(GridCell gc:list){
			if(gc.getGridX() >= ruCell.getGridX() && gc.getGridY() >= ruCell.getGridY()){//X更大或者Y更高
				ruCell = new Cell(gc.getGridX(), gc.getGridY());
			}
		}
		//右下角
		for(GridCell gc:list){
			if(gc.getGridX() >= rdCell.getGridX() && gc.getGridY() <= rdCell.getGridY()){//Y更低或者X更大
				rdCell = new Cell(gc.getGridX(), gc.getGridY());
			}
		}
		//左下角
		for(GridCell gc:list){
			if(gc.getGridX() <= ldCell.getGridX() && gc.getGridY() <= ldCell.getGridY()){//X更小或者Y更低
				ldCell = new Cell(gc.getGridX(), gc.getGridY());
			}
		}
		
		//获取四个角的gps
		for(GridCell gc:list){
			int x = gc.getGridX();
			int y = gc.getGridY();
			//同一个cell可能同时为左上角和右下角，所以不能用if else
			if(x == luCell.getGridX() && y == luCell.getGridY()){//左上
				luGps = gc.getLuGps();
			}
			if(x == ruCell.getGridX() && y == ruCell.getGridY()){//右上
				ruGps = gc.getRuGps();
			}
			if(x == ldCell.getGridX() && y == ldCell.getGridY()){//左下
				ldGps = gc.getLdGps();
			}
			if(x == rdCell.getGridX() && y == rdCell.getGridY()){//右下
				rdGps = gc.getRdGps();
			}
		}
	}

	public String getLabel() {
		return label;
	}

	public int getAvgPointNum() {
		return avgPointNum;
	}

	public GPS getLuGps() {
		return luGps;
	}

	public GPS getLdGps() {
		return ldGps;
	}

	public GPS getRuGps() {
		return ruGps;
	}

	public GPS getRdGps() {
		return rdGps;
	}
	@Override
	public String toString() {
		return "label:"+label +";avgPoint:"+avgPointNum;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
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
		Region other = (Region) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}
	
}
