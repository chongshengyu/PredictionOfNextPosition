package com.yu.draw.entity;

public class Parameter {
	/*public static final double grid_width = 0.13;//0.1
	public static final double grid_height = 0.1;//0.04*/	
	//参数改为格子的边长
	//增加grid的边上有几个格子这个参数，为奇数。
	public static final double cellWidth = 0.5;//cell边长，单位公里，cell是正方形
	public static final int gridWidth = 7;//grid每个边的cell数，grid是正方形。大于0小于100
	/*					(lng,lat+0.02)
	 * 					
	 * 
	 * (lng-0.05,lat)      (lng,lat)		(lng+0.05,lat)
	 * 
	 * 
	 * 					 (lng,lat-0.02)
	 */
	public static final int leastPointNumInCell = 1;//聚合为region时，cell内的最小有效点数（含）。小于这个值将被舍弃
	public static final int leastPointNumInTra = 5;//轨迹最少点数，小于此值的轨迹将被舍弃
}
