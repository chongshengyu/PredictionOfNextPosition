package com.yu.draw.util;

import com.yu.draw.entity.GPS;

public class ConvertDistGPS {
	/**
	 * 
	 * @param gps 起点
	 * @param distance 距离，单位公里
	 * @param angle
	 * @return
	 */
	public static GPS ConvertDistanceToLogLat(GPS gps, double distance, double angle)
	{
	    double lng1 = Double.parseDouble(gps.getLongitude());
	    double lat1 = Double.parseDouble(gps.getLatitude());
	    double lon = lng1 + (distance * Math.sin(angle* Math.PI / 180)) / (111 * Math.cos(lat1 * Math.PI / 180));//将距离转换成经度的计算公式
	    double lat = lat1 + (distance * Math.cos(angle* Math.PI / 180)) / 111;//将距离转换成纬度的计算公式
	    return new GPS(String.valueOf(lon),String.valueOf(lat));
	}
	/**
	 * 
	 * @param gps1
	 * @param gps2
	 * @return 两点距离，单位公里
	 */
	public static double ConvertLogLatToDistance(GPS gps1, GPS gps2){
		double distance = 0;
		//地球半径   
	    double R=6378137.0;//单位米 
	    //模拟数据  
	    double lat1=Double.parseDouble(gps1.getLatitude());  
	    double log1=Double.parseDouble(gps1.getLongitude());  
	    double lat2=Double.parseDouble(gps2.getLatitude());  
	    double log2= Double.parseDouble(gps2.getLongitude());  
	    //将角度转化为弧度  
	    double radLat1=(lat1*Math.PI/180.0);  
	    double radLat2=(lat2*Math.PI/180.0);  
	    double radLog1=(log1*Math.PI/180.0);  
	    double radLog2=(log2*Math.PI/180.0);  
	    //纬度的差值  
	    double a=radLat1-radLat2;  
	    //经度差值  
	    double b=radLog1-radLog2;  
	    //弧度长度  
	    distance=2*Math.asin(Math.sqrt(Math.pow(Math.sin(a/2), 2)+Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2), 2)));  
	    //获取长度  
	    distance=distance*R;  
	    //返回最接近参数的 long。结果将舍入为整数：加上 1/2  
	    distance=Math.round(distance*10000)/10000;  
	    distance = distance / 1000;
//	    System.out.println(distance);
		return distance;
	}
}
