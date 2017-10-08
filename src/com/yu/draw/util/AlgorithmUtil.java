package com.yu.draw.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.GridCell;
import com.yu.draw.entity.Region;
import com.yu.draw.entity.RegionModel;

public class AlgorithmUtil {
	public static <T extends Comparable<T>> LinkedList<T> InsertSort(LinkedList<T> list){
		int j = 0;
		for(int p = 1;p<list.size();p++){
			T tmp = list.get(p);
			for(j = p;j>0&&tmp.compareTo(list.get(j-1))>0;j--){
				list.set(j, list.get(j-1));
			}
			list.set(j, tmp);
		}
		return list;
	}
	@Test
	public void test(){
		LinkedList<GridCell> list = new LinkedList<GridCell>();
		GridCell cell1 = new GridCell(0, 1, new GPS("0","0"));
		GridCell cell2 = new GridCell(0, 2, new GPS("0","0"));
		GridCell cell3 = new GridCell(0, 3, new GPS("0","0"));
		cell1.setPointNumInCell(20);
		cell2.setPointNumInCell(10);
		cell3.setPointNumInCell(30);
		list.add(cell1);
		list.add(cell2);
		list.add(cell3);
		list = InsertSort(list);
		for(GridCell gc:list){
			System.out.println(gc);
		}
	}
	/**
	 * 论文的打分算法，得到每个region的分值
	 * @param gpsList 已知的几个待预测用户的gps位置，最多两个，第一个为上一个位置，第二个为当前位置
	 * @param originGps 视图中心点
	 * @param time 选定预测时间
	 * @param modelMap 该用户的模型
	 * @param tH tao_H
	 * @param tL tao_L
	 * @return 预测结果
	 */
	public static HashMap<Region, Double> getScoreMap(ArrayList<GPS> gpsList,GPS originGps, String time,HashMap<Region, ArrayList<RegionModel>> modelMap, double tH, double tL){
		Region resultRegion = null;
		GPSCell cellFirst = null;
		GPSCell cellSecond = null;
		Region regionFirst = null;
		Region regionSecond = null;//当前所在的region
		//得到两个region，第二个region为当前所在的region，第一个为上一个region
		if(gpsList.size() == 1){//只知道一个点
			cellSecond = GridUtil.GPSToGPSCell(new GPSPoint(gpsList.get(0).getLongitude(),gpsList.get(0).getLatitude(),time), originGps);
			//GPSCell还需要转成GridCell
			GridCell gridCellSecond = new GridCell(cellSecond.getGridX(), cellSecond.getGridY(), originGps);
			
			
			System.out.println("cell位置:"+cellSecond.getGridX()+","+cellSecond.getGridY());//6,3
			Set<Region> keySet = modelMap.keySet();
			for(Region region:keySet){
				System.out.println("cell list size"+region.getCellsContainedList().size());
				//调试输出
				for(int i=0;i<region.getCellsContainedList().size();i++){
					
				}
				
				if(region.getCellsContainedList().contains(gridCellSecond)){//LinkedList<GridCell>
					System.out.println("find region:"+region);
					regionSecond = region;
					break;
				}
			}
		}else if(gpsList.size() == 2){//知道两点
			cellFirst = GridUtil.GPSToGPSCell(new GPSPoint(gpsList.get(0).getLongitude(),gpsList.get(0).getLatitude(),time), originGps);
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
			}
		}
		System.out.println("当前region："+regionSecond);//null?
		ArrayList<RegionModel> regionModelList = modelMap.get(regionSecond);//regionSecond是当前预测时所在的region
		HashMap<Region, Double> scoreMap = new HashMap<Region, Double>();//下一个region的scoreMap
		//先把所有region的打分初始化为0
		for(Region region:modelMap.keySet()){
			scoreMap.put(region, 0.0);
		}
		if(regionFirst == null){//只知道当前region，不知道上一个region
			for(RegionModel rm:regionModelList){
				Region nextRegion = rm.getNext();
				double score = PredictionUtil.getScoreByTwoTime(rm.getNextTime(), time, tH, tL);
				if(scoreMap.containsKey(nextRegion)){
					scoreMap.put(nextRegion, scoreMap.get(nextRegion) + score);//加上新的分数
				}else{
					scoreMap.put(nextRegion, score);
				}
			}
		}else{//知道当前region和上一个region
			for(RegionModel rm:regionModelList){
				Region nextRegion = rm.getNext();
				Region preRegion = rm.getPre();
				if(preRegion != null && preRegion.equals(regionFirst)){//两次region都匹配
					double score = PredictionUtil.getScoreByTwoTime(rm.getNextTime(), time, tH, tL);
					if(scoreMap.containsKey(nextRegion)){
						scoreMap.put(nextRegion, scoreMap.get(nextRegion) + score);//加上新的分数
					}else{
						scoreMap.put(nextRegion, score);
					}
				}else{//不计算只匹配一次的情况
					
				}
			}
		}
		//得到scoreMap中得分最高的region，即为预测结果
		/*double maxScore = 0.0;
		for(Region region:scoreMap.keySet()){
			if(scoreMap.get(region) > maxScore){
				maxScore = scoreMap.get(region);
				resultRegion = region;
			}
		}*/
		return scoreMap;
	}
}
