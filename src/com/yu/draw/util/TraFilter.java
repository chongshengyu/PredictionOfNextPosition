package com.yu.draw.util;

import java.util.ArrayList;
import java.util.Map;

import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.Parameter;

public class TraFilter {
	/**
	 * 轨迹点平滑
	 * 测试，使用朝向线性回归，角度滑动窗口得到已平滑的subtra
	 * @param traOld
	 * @return
	 */
	public static ArrayList<GPSPoint> getSparsedTra(ArrayList<GPSPoint> traOld){
		//最好是线性时间
		ArrayList<GPSPoint> tra = traOld;//结果轨迹
		//---------------------------------------------------期刊论文
		//step1.根据朝向过滤
		tra = getFilterdTraByOrientation(tra);
		
		//step2.根据距离过滤
		tra = getFilteredTraByDistance(tra);
		
		//step3.尖点过滤
		tra = getFilteredTraByTriangle(tra);
		
		/*----------------------------------------------------会议论文
		//step1.尖点过滤
		tra = getFilteredTraByTriangle(tra);
		//step2.距离过滤
		tra = getFilteredTraByDistance2(tra);
		//again
		tra = getFilteredTraByTriangle(tra);
		*/
		return tra;
	}
	
	/**
	 * 去除尖点或尖区域，A->B->C->D，若 K * AD < AB + BC + CD，则去除BC
	 * @param traOld
	 * @return
	 */
	private static ArrayList<GPSPoint> getFilteredTraByTriangle(ArrayList<GPSPoint> traOld){
		ArrayList<GPSPoint> tra = new ArrayList<GPSPoint>();//结果轨迹
		//A->B->C过滤B类型
		int i=0;
		for(i = 0;i<traOld.size() - 2;i++){
			tra.add(traOld.get(i));
			if(Parameter.LAMDA_TRIANGLE_FILTER_TIMES * getDistance(traOld.get(i), traOld.get(i+2)) < (getDistance(traOld.get(i), traOld.get(i+1)) + getDistance(traOld.get(i+1), traOld.get(i+2)))){
				//尖点
				i++;
			}
		}
		for(;i<traOld.size();i++){
			tra.add(traOld.get(i));
		}
		//A->B->C->D过滤B,C类型
		
		return tra;
	}
	
	/**
	 * 按照两点距离取出重复点
	 * @param traOld
	 * @return
	 */
	private static ArrayList<GPSPoint> getFilteredTraByDistance(ArrayList<GPSPoint> traOld){
		ArrayList<GPSPoint> tra = new ArrayList<GPSPoint>();//结果轨迹
		tra.add(traOld.get(0));//起始点
		//step1.根据距离过滤 
		int pointer = 0;
		for(int j = pointer + 1;j<traOld.size();j++){
			if(getDistance(traOld.get(pointer), traOld.get(j)) > Parameter.LAMDA_DISTANCE){
				pointer = j;
				tra.add(traOld.get(j));
			}else{
				
			}
		}
		return tra;
	}
	
	/**
	 * 根据距离，距离得到过滤后的轨迹。HPCC论文使用
	 * @param traOld
	 * @return
	 */
	private static ArrayList<GPSPoint> getFilteredTraByDistance2(ArrayList<GPSPoint> traOld){
		ArrayList<GPSPoint> resultList = new ArrayList<GPSPoint>();//函数返回结果
		ArrayList<GPSPoint> getCenterList;//待求质心集合
		int index = 0;
		GPSPoint startPoint;
		while(true){
			getCenterList = new ArrayList<GPSPoint>();
			if(index<traOld.size()-1){
				startPoint = traOld.get(index);
				if(getDistance(startPoint, traOld.get(index+1)) > Parameter.LAMDA_HPCC_DISTANCE){
					resultList.add(traOld.get(index));
					index ++;
					continue;
				}else{//距离小于阈值
					int offset = 2;
					getCenterList.add(startPoint);
					getCenterList.add(traOld.get(index+1));
					while((index+offset<traOld.size())&&(getDistance(startPoint, traOld.get(index+offset)) <= Parameter.LAMDA_HPCC_DISTANCE)){
						getCenterList.add(traOld.get(index+offset));
						offset ++;
					}
					//求质心，加入结果集
					double totalLng = 0.0;
					double totalLat = 0.0;
					for(GPSPoint point:getCenterList){
						totalLng += Double.parseDouble(point.getLang());
						totalLat += Double.parseDouble(point.getLat());
					}
					String cntLng = ""+totalLng/(getCenterList.size());
					String cntLat = ""+totalLat/(getCenterList.size());
					String cntDateTime = getCenterList.get(0).getDateTime();
					resultList.add(new GPSPoint(cntLng, cntLat, cntDateTime));
					//更新index值，继续循环
					index = index + offset + 1;
					continue;
				}
			}else{
				if(index == traOld.size()-1)
					resultList.add(traOld.get(index));
				else {
//					System.out.println("索引越界");
				}
				break;
			}
				
		}
		return resultList;
	}
	/**
	 * 滑动窗口平滑轨迹
	 * @param traOld
	 * @return
	 */
	private static ArrayList<GPSPoint> getFilterdTraByOrientation(ArrayList<GPSPoint> traOld){
		ArrayList<GPSPoint> resultTra = new ArrayList<GPSPoint>();//过滤后的结果
		ArrayList<Double> angleList = new ArrayList<Double>();//完整轨迹的angleList
		ArrayList<Double> anglesInWindow = new ArrayList<Double>();//滑动窗内的angels
		boolean flagWindowExt = false;//窗口是否扩展过
		
		int avgBegionIndex = 1;//需要求均值的部分的索引
		int avgEndIndex = 1;
		int windowIndex = 1;//窗口位置
		int windowCurrentLength = Parameter.LAMDA_WINDOW_INIT_LENGTH;//窗口长度
		
		resultTra.add(traOld.get(0));
		angleList.add(0.0);//第一个点角度定为0
		
		for(int i=1;i<traOld.size();i++){//得到角度list
			angleList.add(ConvertDistGPS.GetAngle(new GPS(traOld.get(i-1).getLang(),traOld.get(i-1).getLat()), new GPS(traOld.get(i).getLang(),traOld.get(i).getLat())));
		}
		
		for(windowIndex = 1;windowIndex<=traOld.size()-windowCurrentLength;windowIndex++){//移动窗口
			anglesInWindow.clear();
			for(int i = windowIndex;i<windowIndex + windowCurrentLength;i++){//填充窗
				anglesInWindow.add(angleList.get(i));
			}
			if(getMaxDiffAngleInWindow(anglesInWindow)>Parameter.LAMDA_WINDOW_HEIGHT){//大于窗高
				if(flagWindowExt){//刚刚扩展了窗
					windowCurrentLength --;//回退到扩展前
					for(int i=windowIndex;i<windowIndex+windowCurrentLength;i++){
						resultTra.add(traOld.get(i));//输出
					}
					avgBegionIndex = windowIndex + windowCurrentLength;//移动到窗后，开始下一轮
					avgEndIndex = avgBegionIndex;
					windowIndex = avgBegionIndex - 1;//for循环会加1
					windowCurrentLength = Parameter.LAMDA_WINDOW_INIT_LENGTH;//窗长恢复默认
					flagWindowExt = false;
				}else{//刚刚移动了窗，或轨迹起始处
					avgEndIndex = windowIndex + windowCurrentLength - 1;//更新均值尾部
				}
			}else{//小于窗高
				if(avgEndIndex > avgBegionIndex){
					avgEndIndex = windowIndex - 1;//更新均值尾部
					resultTra.add(getAvgPoints(traOld, avgBegionIndex, avgEndIndex));//输出均值点
					avgBegionIndex = avgEndIndex;
				}
				windowCurrentLength ++;
				flagWindowExt = true;
				windowIndex --;//for循环会加1
			}
		}
		
		if(avgEndIndex != avgBegionIndex){//最后在窗外
			resultTra.add(getAvgPoints(traOld, avgBegionIndex, avgEndIndex));
			anglesInWindow.clear();
		}
		
		if(!anglesInWindow.isEmpty()){
			for(int i=windowIndex;i<windowIndex+windowCurrentLength-1-1;i++){
				if(i<traOld.size())
					resultTra.add(traOld.get(i));//输出
			}
		}
		
		return resultTra;
	}
	/**
	 * 得到轨迹中指定索引段的平均位置点
	 * @param traOld
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	private static GPSPoint getAvgPoints(ArrayList<GPSPoint> traOld, int startIndex, int endIndex){
		String dateTime = traOld.get(startIndex).getDateTime();//以第一个点的时间作为“平均时间”
		double totalLng = 0.0;
		double totalLat = 0.0;
		for(int i=startIndex;i<=endIndex;i++){
			totalLng += Double.parseDouble(traOld.get(i).getLang());
			totalLat += Double.parseDouble(traOld.get(i).getLat());
		}
		String lng = String.valueOf(totalLng / (endIndex - startIndex + 1));
		String lat = String.valueOf(totalLat / (endIndex - startIndex + 1));
		return new GPSPoint(lng, lat, dateTime);
	}
	/**
	 * 求窗口中的所有点的最大差值，以判断是否超过阈值
	 * @param points
	 * @return
	 */
	private static double getMaxDiffAngleInWindow(ArrayList<Double> angles){
		double maxAngle = -1;
		double minAngle = 361;
		for(double angle : angles){
			if(angle > maxAngle){
				maxAngle = angle;
			}else if(angle < minAngle){
				minAngle = angle;
			}
		}
//		System.out.println("max:"+maxAngle+";min:"+minAngle+";差值："+(maxAngle - minAngle));
		return maxAngle - minAngle;
	}
	/**
	 * 计算两个gps之间的距离，单位m
	 * @param p1
	 * @param p2
	 * @return
	 */
	private static int getDistance(GPSPoint p1, GPSPoint p2){
		double distance = ConvertDistGPS.ConvertLogLatToDistance(new GPS(p1.getLang(),p1.getLat()), new GPS(p2.getLang(),p2.getLat()));//单位公里
		return (int)(distance * 1000);
	}
	
	/**
	 * 计算两个gps之间的距离，单位m
	 * @param g1
	 * @param g2
	 * @return
	 */
	private static int getDistance(GPS g1, GPS g2){
		double distance = ConvertDistGPS.ConvertLogLatToDistance(g1,g2);//单位公里
		return (int)(distance * 1000);
	}
}
