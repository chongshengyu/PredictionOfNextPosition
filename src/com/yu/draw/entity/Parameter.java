package com.yu.draw.entity;

public class Parameter {
	/*public static final double grid_width = 0.13;//0.1
	public static final double grid_height = 0.1;//0.04*/	
	//参数改为格子的边长
	//增加grid的边上有几个格子这个参数，为奇数。
	public static double cellWidth = 0.3;//cell边长，单位公里，cell是正方形
	public static int gridWidth = 11;//grid每个边的cell数，grid是正方形。大于0小于100。前台展示和后台测试都是用的这个参数
	public static int MAXREGIONWIDTH = 5;//region的最大长度
	/*					(lng,lat+0.02)
	 * 					
	 * 
	 * (lng-0.05,lat)      (lng,lat)		(lng+0.05,lat)
	 * 
	 * 
	 * 					 (lng,lat-0.02)
	 */
	//轨迹过滤参数
	public static final double LAMDA_HPCC_DISTANCE = 25;//hpcc,过滤轨迹点用
	public static final double LAMDA_DISTANCE = 25;//过滤重复点的参数，单位m，小于此值的点认为是重复点 
	public static final int LAMDA_WINDOW_INIT_LENGTH = 10;//轨迹朝向滑动窗口初始宽度，单位点数
	public static final int LAMDA_WINDOW_HEIGHT = 90;//滑动窗口高度，单位度,60
	public static final int LAMDA_TRIANGLE_FILTER_TIMES = 2;//三角去尖点，倍数参数
	
	//核心算法参数
	public static final int leastPointNumInCell = 1;//聚合为region时，cell内的最小有效点数（含）。小于这个值将被舍弃
	public static final int leastPointNumInTra = 5;//轨迹最少点数，小于此值的轨迹将被舍弃
	public static final int periodOfHalfLift = 180;//半衰期有效系数的周期，3天
	public static final double coefficientOfHalfLift = 0;//半衰期有效系数的系数
	public static final double valueOfHalfLift = 3.0/4.0;//衰减的速度
	
	//测试用参数
	public static final int FILTER_STEP_LENGTH = 6;//测试集中的轨迹的稀疏参数，每6个点选一个
	public static final int TRA_POINTS_MIN = 80;//样本集中的轨迹最小点数，点数小于这些的轨迹不被使用，大于这个值的轨迹作为有效轨迹
	public static final int TRA_COUNT_MIN = 50;//某用户的有效轨迹条数，大于这个值的用户将被用于实验
	public static final double PROP_OF_SAMPLE_SET = 0.9;//样本集占比
}
