package com.yu.draw.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.junit.Test;

import com.yu.draw.entity.GPS;
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
	 * 论文的预测算法
	 * @param gpsList 已知的几个待预测用户的gps位置，最多三个
	 * @param time 选定预测时间
	 * @param modelMap 该用户的模型
	 * @return 预测结果
	 */
	public static Region myPrediction(ArrayList<GPS> gpsList,String time,HashMap<Region, ArrayList<RegionModel>> modelMap){
		Region nextRegion = null;
		
		return nextRegion;
	}
}
