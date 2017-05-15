package com.yu.evaluation.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import com.yu.draw.entity.Cell;
import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.GridCell;
import com.yu.draw.entity.Parameter;
import com.yu.draw.entity.Region;
import com.yu.draw.entity.RegionModel;
import com.yu.draw.entity.RegionTime;
import com.yu.draw.util.GridUtil;
import com.yu.draw.util.JdbcUtil;
import com.yu.draw.util.PredictionUtil;
import com.yu.draw.util.TraFilter;
import com.yu.evaluation.entity.ResultScores;

public class TraUtil {
	/**
	 * 将轨迹稀疏，训练集和样本集都使用了
	 * @param traOld稀疏前的轨迹
	 * @return 稀疏后的轨迹
	 */
	/*public static ArrayList<GPSPoint> getSparsedTra(ArrayList<GPSPoint> traOld){
		final int para = Parameter.FILTER_STEP_LENGTH;//每隔para个点取一次
		ArrayList<GPSPoint> tra = new ArrayList<GPSPoint>();
		int i = 0;
		for(GPSPoint p:traOld){
			if(i % para == 0){
				tra.add(p);
			}
			i++;
		}
		traOld = null;
		return tra;
	}*/
	/**
	 * 得到指定用户在指定区域，指定轨迹号的轨迹片段
	 * @param userId
	 * @param lu_lng 指定的区域
	 * @param lu_lat
	 * @param rd_lng
	 * @param rd_lat
	 * @param trainTraNoList 指定的轨迹号
	 * @return
	 */
	public static ArrayList<ArrayList<GPSPoint>> getTraInGrid(String userId,
			String lu_lng, String lu_lat, String rd_lng, String rd_lat,ArrayList<String> trainTraNoList) {
		ArrayList<ArrayList<GPSPoint>> traList = new ArrayList<ArrayList<GPSPoint>>();
		ArrayList<GPSPoint> pointList = new ArrayList<GPSPoint>();
		Connection conn = null;
		Statement stmt = null;
		StringBuilder sb = new StringBuilder();
		for(String traNo:trainTraNoList){
			sb.append(traNo+",");
		}
		String sql = "SELECT Long_gcj,Lat_gcj,DateTime,TraNum FROM location WHERE UserId='"
				+ userId
				+ "' AND Long_gcj>'"
				+ lu_lng
				+ "' AND Long_gcj<'"
				+ rd_lng
				+ "' AND Lat_gcj>'"
				+ rd_lat
				+ "' AND Lat_gcj<'"
				+ lu_lat
				+ "' AND TraNum in("+sb.subSequence(0, sb.length()-1)+") ORDER BY DateTime";
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			String traNumTmp = "0";
			while (rs.next()) {
				if (traNumTmp.equals(rs.getString("TraNum"))) {// 轨迹号相同，同一条轨迹
					GPSPoint point = new GPSPoint(rs.getString("Long_gcj"),
							rs.getString("Lat_gcj"), rs.getString("DateTime"));
					pointList.add(point);
				} else {// 轨迹号不同，不同轨迹
					if(pointList.size() < Parameter.leastPointNumInTra){
						pointList = new ArrayList<GPSPoint>();
						traNumTmp = rs.getString("TraNum");
						continue;
					}
					pointList = TraFilter.getSparsedTra(pointList);// 轨迹过滤
					traList.add(pointList);
					pointList = new ArrayList<GPSPoint>();
					traNumTmp = rs.getString("TraNum");
				}
			}
			traList.add(pointList);//加入最后一条
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JdbcUtil.close(conn, stmt);
		return traList;
	}
	/**
	 * 论文的打分算法，得到每个region的分值，已知的是gps位置点
	 * @param gpsList 已知的几个待预测用户的gps位置，最多两个，第一个为上一个位置，第二个为当前位置
	 * @param originGps 视图中心点
	 * @param time 选定预测时间
	 * @param modelMap 该用户的模型
	 * @return 打分结果
	 */
	public static HashMap<Region, ResultScores> getScoreMap(ArrayList<GPS> gpsList,GPS originGps, String time,HashMap<Region, ArrayList<RegionModel>> modelMap){
		GPSCell cellFirst = null;
		GPSCell cellSecond = null;
		Region regionFirst = null;
		Region regionSecond = null;//当前所在的region
		//得到两个region，第二个region为当前所在的region，第一个为上一个region
		if(gpsList.size() == 1){//只知道一个点，gpsList中只有一个点
			cellSecond = GridUtil.GPSToGPSCell(new GPSPoint(gpsList.get(0).getLongitude(),gpsList.get(0).getLatitude(),time), originGps);
			//GPSCell还需要转成GridCell
			GridCell gridCellSecond = new GridCell(cellSecond.getGridX(), cellSecond.getGridY(), originGps);
			Set<Region> keySet = modelMap.keySet();
			for(Region region:keySet){
				if(region.getCellsContainedList().contains(gridCellSecond)){//LinkedList<GridCell>
					regionSecond = region;
					break;
				}
			}
			if(regionSecond == null){//当前点不在由历史数据得到的region中
				return null;
			}
		}else if(gpsList.size() == 2){//知道两点
			/*cellFirst = GridUtil.GPSToGPSCell(new GPSPoint(gpsList.get(0).getLongitude(),gpsList.get(0).getLatitude(),time), originGps);
			cellSecond = GridUtil.GPSToGPSCell(new GPSPoint(gpsList.get(1).getLongitude(),gpsList.get(1).getLatitude(),time), originGps);
			//GPSCell还需要转成GridCell
			GridCell gridCellSecond = new GridCell(cellSecond.getGridX(), cellSecond.getGridY(), originGps);
			GridCell gridCellFirst = new GridCell(cellFirst.getGridX(), cellFirst.getGridY(), originGps);
			Set<Region> keySet = modelMap.keySet();
			for(Region region:keySet){
				if(region.getCellsContainedList().contains(gridCellFirst)){//LinkedList<GridCell>
					regionFirst = region;
				}
				if(region.getCellsContainedList().contains(gridCellSecond)){//LinkedList<GridCell>
					regionSecond = region;
				}
				if(regionFirst != null && regionSecond != null){
					break;
				}
			}*/
		}
		//regionSecond的regionModelList
		ArrayList<RegionModel> regionModelList = modelMap.get(regionSecond);//regionSecond是当前预测时所在的region
		HashMap<Region, ResultScores> scoreMap = new HashMap<Region, ResultScores>();//下一个region的scoreMap
		//先把所有region的打分初始化为0
		for(Region region:modelMap.keySet()){
			scoreMap.put(region, new ResultScores(0.0, 0.0));
		}
		if(regionFirst == null){//只知道当前region，不知道上一个region
			for(RegionModel rm:regionModelList){
				Region nextRegion = rm.getNext();
				double myScore = PredictionUtil.getScoreByTwoTime(rm.getNextTime(), time);
				if(scoreMap.containsKey(nextRegion)){
					//PredictionUtil.keep2bit(scoreMap.get(nextRegion) + score)
					scoreMap.put(nextRegion, new ResultScores(PredictionUtil.keep2bit(scoreMap.get(nextRegion).getMyScore()+myScore), scoreMap.get(nextRegion).getRefScore()+1));//加上新的分数
				}else{
					scoreMap.put(nextRegion, new ResultScores(myScore, 1));
				}
			}
		}else{//知道当前region和上一个region
			/*for(RegionModel rm:regionModelList){
				Region nextRegion = rm.getNext();
				Region preRegion = rm.getPre();
				if(preRegion != null && preRegion.equals(regionFirst)){//两次region都匹配
					double score = PredictionUtil.getScoreByTwoTime(rm.getNextTime(), time);
					if(scoreMap.containsKey(nextRegion)){
						scoreMap.put(nextRegion, scoreMap.get(nextRegion) + score);//加上新的分数
					}else{
						scoreMap.put(nextRegion, score);
					}
				}else{//不计算只匹配一次的情况
					
				}
			}*/
		}
		return scoreMap;
	}
	
	/**
	 * 论文打分算法，得到每个region的分值，已知的是knownRegionList
	 * @param knownRegionList 测试用户的已知regionTime序列，有1-2个。每个regionTime包含一个Region和到达该Region的时间
	 * @param modelMap 用户模型
	 * @return 打分结果
	 */
	public static HashMap<Region, ResultScores> getScoreMap(ArrayList<RegionTime> knownRegionList,HashMap<Region, ArrayList<RegionModel>> modelMap){
		Region firstRegion = null;//前一个
		Region secondRegion = null;//当前
		String firstTime = "";
		String secondTime = "";

		//scoreMap初始化为0
		HashMap<Region, ResultScores> scoreMap = new HashMap<Region, ResultScores>();
		for(Region r:modelMap.keySet()){
			scoreMap.put(r, new ResultScores(0.0, 0.0));
		}
		
		if(knownRegionList.size() == 1){
			secondRegion = knownRegionList.get(0).getRegion();
			secondTime = knownRegionList.get(0).getTime();
			ArrayList<RegionModel> regionModelList = modelMap.get(secondRegion);
			if(null == regionModelList){//历史数据里没有这个region
				return null;
			}
			for(RegionModel rm:regionModelList){
				Region nextRegion = rm.getNext();
				double myScore = PredictionUtil.getScoreByTwoTime(rm.getNextTime(),secondTime);
				if(scoreMap.containsKey(nextRegion)){
					scoreMap.put(nextRegion, new ResultScores(scoreMap.get(nextRegion).getMyScore()+myScore, scoreMap.get(nextRegion).getRefScore()+1));//加上新的分数
				}else{
					scoreMap.put(nextRegion, new ResultScores(myScore, 1));
				}
			}
		}else if(knownRegionList.size() == 2){
			secondTime = knownRegionList.get(0).getTime();
			firstRegion = knownRegionList.get(0).getRegion();
			secondRegion = knownRegionList.get(1).getRegion();
			ArrayList<RegionModel> regionModelList = modelMap.get(secondRegion);
			if(null == regionModelList){//历史轨迹里没有这个region
				return null;
			}
			boolean matchBoolean = false;
			for(RegionModel rm:regionModelList){
				if(firstRegion.equals(rm.getPre())){//只增加两次region都匹配的分值
					Region nextRegion = rm.getNext();
					double myScore = PredictionUtil.getScoreByTwoTime(rm.getNextTime(),secondTime);
					if(scoreMap.containsKey(nextRegion)){
						scoreMap.put(nextRegion, new ResultScores(scoreMap.get(nextRegion).getMyScore()+myScore, scoreMap.get(nextRegion).getRefScore()+1));//加上新的分数
					}else{
						scoreMap.put(nextRegion, new ResultScores(myScore, 1));
					}
					matchBoolean = true;
				}
			}
			if(!matchBoolean){//前一个Region都没有匹配，则按照只知道当前一个region的方法处理
				for(RegionModel rm:regionModelList){
					Region nextRegion = rm.getNext();
					double myScore = PredictionUtil.getScoreByTwoTime(rm.getNextTime(),secondTime);
					if(scoreMap.containsKey(nextRegion)){
						scoreMap.put(nextRegion, new ResultScores(scoreMap.get(nextRegion).getMyScore()+myScore, scoreMap.get(nextRegion).getRefScore()+1));//加上新的分数
					}else{
						scoreMap.put(nextRegion, new ResultScores(myScore, 1));
					}
				}
			}
		}else {
			System.out.println("getScoreMap中传入的knownRegionList参数错误");
		}
		return scoreMap;
	}
	
	/**
	 * 将cellTra转为regionTra
	 * @param cellTra 
	 * @param regionTra 通过分析所有轨迹得到的region的list
	 * @return 所有轨迹对应的regionList
	 */
	public static ArrayList<RegionTime> cellTra2RegionTra(ArrayList<GPSCell> cellTra, LinkedList<Region> regionList){
		ArrayList<RegionTime> regionTra = new ArrayList<RegionTime>();
		//建立cell=>region的映射，然后遍历所有cellTra
		HashMap<Cell, Region> cell2RegionMap = new HashMap<Cell, Region>();
		for(Region region:regionList){
			String label = region.getLabel();
			String[] subLabel = label.split("-");
			for(int i=1;i<subLabel.length;i++){
				String xy = subLabel[i];  
				String x = xy.substring(0, 2);
				String y = xy.substring(2, 4);
				Cell cell = new Cell(Integer.parseInt(x), Integer.parseInt(y));
				cell2RegionMap.put(cell, region);
			}
		}
		//遍历cellTra
		for(GPSCell gc:cellTra){
			Cell cell = new Cell(gc.getGridX(), gc.getGridY());//位置
			String time = gc.getCellTime();//时间
			if(regionTra.size() == 0){//新轨迹
				Region region = cell2RegionMap.get(cell);
				if(region == null){//gc没有对应region
					continue;
				}else{//找到gc对应的region
					regionTra.add(new RegionTime(region, time));
				}
			}else{//已有region
				Region region = cell2RegionMap.get(cell);
				if(region == null){//gc没有对应region
					continue;
				}else{//找到gc对应的region
					if(regionTra.get(regionTra.size()-1).getRegion().equals(region)){//重复
						continue;
					}else{
						regionTra.add(new RegionTime(region, time));
					}
				}
			}
		}
		return regionTra;
	}
}
