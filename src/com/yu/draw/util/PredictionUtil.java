package com.yu.draw.util;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import com.yu.draw.entity.Parameter;

public class PredictionUtil {
	/*public static double EfficiencyBasedOnHalflife(String timeI, String timeD){
		double result = 0.0;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dateI = new Date(0);
		Date dateD = new Date(0);
		try {
			dateI = simpleDateFormat.parse(timeI);
			dateD = simpleDateFormat.parse(timeD);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int days = ((int)(dateD.getTime() - dateI.getTime()))/(24*60*60*1000);
		result = Parameter.coefficientOfHalfLift * Math.pow(0.5, days/Parameter.periodOfHalfLift);
		
		//保留两位
		NumberFormat nf = NumberFormat.getNumberInstance();
		// 保留两位小数
		nf.setMaximumFractionDigits(2); 
		// 如果不需要四舍五入，可以使用RoundingMode.DOWN
		nf.setRoundingMode(RoundingMode.UP);
		return Double.parseDouble(nf.format(result));
	}*/

	/**
	 * 基于半衰期的数据有效性计算
	 * @param timeI 待计算有效性的数据获取时间，较早
	 * @param timeD 设定的“当前”预测时的时间，较晚
	 * @return 基于半衰期的有效性系数
	 */
	public static double getScoreByTwoTime(String timeI, String timeD){
		double result1 = 0.0;//有效性
		double result2 = 0.0;//相似性
		//有效性
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dateI = new Date(0);
		Date dateD = new Date(0);
		try {
			dateI = simpleDateFormat.parse(timeI);
			dateD = simpleDateFormat.parse(timeD);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long days = ((dateD.getTime() - dateI.getTime()))/(24*60*60*1000);
		
		/**********************/
		//0.75^(x)
//		result1 = Parameter.coefficientOfHalfLift * Math.pow(Parameter.valueOfHalfLift, (double)days/Parameter.periodOfHalfLift);
		/**********************/
		//-(1/60)*x+1
		result1 = Parameter.coefficientOfHalfLift * (double)days * (-1.0/50) + 1.1;
		result1 = result1 > 0 ? result1 : 0;
		/**********************/
		//1/(1+e^((1/6)*(x-60))-e^(-60/6))
		result1 = 0.2 + 1.0/(1+Math.pow(Math.E, (1.0/4)*(days-40))-Math.pow(Math.E, -40/4));
		/**********************/
//		System.out.println(result1);
		//相似性
		String hourI = ""+dateI.getHours();
		String hourD = ""+dateD.getHours();
		double diff = Math.abs(Integer.parseInt(hourI) - Integer.parseInt(hourD)) > 12 ? 24 - Math.abs(Integer.parseInt(hourI) - Integer.parseInt(hourD)) : Math.abs(Integer.parseInt(hourI) - Integer.parseInt(hourD));
		result2 = (1-Parameter.coefficientOfHalfLift)*(1 - diff / 12);
		return result2;
//		return result1 + result2;
	}
	
	public static double keep2bit(double src){
		String s = String.valueOf(src);
		String out = s;
		if(s.split("\\.")[1].length()>2){
			out = s.split("\\.")[0]+"."+s.split("\\.")[1].substring(0, 2);
		}
		return Double.parseDouble(out);
	}

	public static void main(String[] args){
		getScoreByTwoTime("2015-01-01 12:00:00","2015-02-20 14:00:00");
	}
}
