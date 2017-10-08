package com.yu.draw.util;

import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
	 * @param tH tao_H
	 * @param tL tao_L 两组取值可为  1.5/1.0, 1.2/1.0, 1.0/1.0
	 * @return 基于半衰期的有效性系数
	 */
	public static double getScoreByTwoTime(String timeI, String timeD, double tH, double tL){
		double result1 = 0.0;//有效性
		double result2 = 0.0;//相似性
		double result3 = tL;//weekly
		/******************************************************************/
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
		//0.75^(x)
//		result1 = Parameter.coefficientOfHalfLift * Math.pow(Parameter.valueOfHalfLift, (double)days/Parameter.periodOfHalfLift);
		//-(1/60)*x+1
//		result1 = Parameter.coefficientOfHalfLift * (double)days * (-1.0/50) + 1.1;
//		result1 = result1 > 0 ? result1 : 0;
		//1/(1+e^((1/6)*(x-60))-e^(-60/6))
//		result1 = 0.2 + 1.0/(1+Math.pow(Math.E, (1.0/4)*(days-40))-Math.pow(Math.E, -40/4));
		/******************************************************************/
		//相似性
		String hourI = ""+dateI.getHours();
		String hourD = ""+dateD.getHours();
		double diff = Math.abs(Integer.parseInt(hourI) - Integer.parseInt(hourD)) > 12 ? 24 - Math.abs(Integer.parseInt(hourI) - Integer.parseInt(hourD)) : Math.abs(Integer.parseInt(hourI) - Integer.parseInt(hourD));
//		result2 = (1-Parameter.coefficientOfHalfLift)*(1 - diff / 12);
		result2 = 1 - diff/12;
		/******************************************************************/
		//weekday
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("GMT+0800"));
		try {
			dateI = df.parse(timeI);
			dateD = df.parse(timeD);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dateI);
		int weekDayI = calendar.get(Calendar.DAY_OF_WEEK) - 1;//周天为0
		calendar.setTime(dateD);
		int weekDayD = calendar.get(Calendar.DAY_OF_WEEK) - 1;
		if((weekDayI>=1)&&(weekDayI<=5)&&(weekDayD>=1)&&(weekDayD<=5)){
			result3 = tH;
		}else if(((weekDayI==6)||(weekDayI==0))&&((weekDayD==6)||(weekDayD==0))){
			result3 = tH;
		} 
		/******************************************************************/
		double result = result2 * result3;
		return result;
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
		System.out.println(getScoreByTwoTime("2015-02-01 17:00:00","2015-02-20 17:00:00",1.2,1.0));
		//getScoreByTwoTime("2015-01-01 12:00:00","2015-02-20 14:00:00");
	}
}
