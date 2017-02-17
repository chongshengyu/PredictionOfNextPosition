package com.yu.draw.util;

import java.util.ArrayList;

import com.yu.draw.entity.GPSPoint;

public class TraFilter {
	public static ArrayList<GPSPoint> getSparsedTra(ArrayList<GPSPoint> traOld){
		final int para = 15;//每隔para个点取一次
		ArrayList<GPSPoint> tra = new ArrayList<GPSPoint>();
		int i = 0;
		for(GPSPoint p:traOld){
			if(i % para == 0){
				tra.add(p);
			}
			i++;
		}
		return tra;
	}
}
