package com.yu.draw.util;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.yu.draw.entity.Parameter;

public class PredictionUtil {
	/**
	 * 基于半衰期的数据有效性计算
	 * @param timeI 待计算有效性的数据获取时间，较早
	 * @param timeD 设定的“当前”预测时的时间，较晚
	 * @return 基于半衰期的有效性系数
	 */
	public static double EfficiencyBasedOnHalflife(String timeI, String timeD){
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
	}
}
