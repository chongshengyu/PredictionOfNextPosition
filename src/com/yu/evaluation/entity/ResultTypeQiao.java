package com.yu.evaluation.entity;

import java.util.ArrayList;

import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;

public class ResultTypeQiao {
	public ArrayList<GPSCell> cellList;
	public ArrayList<GPSPoint> pointList;
	public ResultTypeQiao(ArrayList<GPSCell> cellList,
			ArrayList<GPSPoint> pointList) {
		super();
		this.cellList = cellList;
		this.pointList = pointList;
	}
	
}
