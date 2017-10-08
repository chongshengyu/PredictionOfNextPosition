package com.yu.evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.taglibs.standard.lang.jstl.test.StaticFunctionTests;

import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.Parameter;
import com.yu.draw.entity.Region;
import com.yu.draw.entity.RegionModel;
import com.yu.draw.entity.RegionTime;
import com.yu.draw.util.ConvertDistGPS;
import com.yu.draw.util.GridUtil;
import com.yu.draw.util.TraFilter;
import com.yu.evaluation.entity.Para;
import com.yu.evaluation.entity.ResultScores;
import com.yu.evaluation.entity.ResultTypeQiao;
import com.yu.evaluation.entity.TestUser;
import com.yu.evaluation.util.TraUtil;
import com.yu.hmm.Forward;
import com.yu.hmm.Learn;
import com.yu.hmm.Pair;
import com.yu.hmm.Viterbi;
import com.yu.prepare.util.JdbcUtil;

public class EvaAlgori_Qiao {
	/**
	 * 找出point的para_epsilon邻域内的所有点，不包括point自己
	 * @param point 中心点
	 * @param para_epsilon 半径
	 * @param allPoints 点集
	 * @return 点集中距离中心点在半径内的所有点
	 */
	private static ArrayList<GPSPoint> neighbours(GPSPoint point, double para_epsilon, ArrayList<GPSPoint> allPoints){
		ArrayList<GPSPoint> results = new ArrayList<GPSPoint>();
		GPS g1 = new GPS(point.getLang(),point.getLat());
		for(GPSPoint thisPoint:allPoints){
			GPS g2 = new GPS(thisPoint.getLang(),thisPoint.getLat());
			double dis = ConvertDistGPS.ConvertLogLatToDistance(g1,g2);
			if(dis<para_epsilon && dis>0){
				results.add(thisPoint);
			}
		}
		return results;
	}
	/**
	 * 扩展以point为中心的cluster然后添加到clusters中
	 * @param point 中心
	 * @param results 中心点的邻域
	 * @param cluster 当前簇，中心点的邻域扩展而来
	 * @param allPoints 该用户所有位置点
	 * @param para_epsilon 半径参数
	 * @param para_theta 点个数参数
	 */
	private static void ExpandCluster(GPSPoint point, 
									  ArrayList<GPSPoint> neighbours, 
									  ArrayList<GPSPoint> cluster,
									  ArrayList<GPSPoint> allPoints,
									  double para_epsilon, 
									  int para_theta,
									  Para pa){
		cluster.add(point);
		for(GPSPoint point2:neighbours){
			//会出现某一个类别点数过高，其他过低的现象。可以在聚类过程中判断当前类别点数，提前终止扩张操作。以下。
			if(cluster.size()>8){
				return;
			}
			//会出现某一个类别点数过高，其他过低的现象。可以在聚类过程中判断当前类别点数，提前终止扩张操作。以上。
			if(point2.visited == true){
//				pa.con2 = pa.con2+1;
				continue;
			}
			point2.visited = true;
			ArrayList<GPSPoint> nei2 = neighbours(point2, para_epsilon, allPoints);
			if(nei2.size()>=para_theta){
				ExpandCluster(point2, nei2, cluster, allPoints, para_epsilon, para_theta, pa);
			}else{
				pa.noize++;
			}
		}
	}
	
	/**
	 * 获得轨迹序列对应的簇标号序列。
	 * 注意：有的位置点可能没有对应的簇，则认为和前面的点对应的簇一样。如果第一个点没有对应的簇，则标号为-1
	 * @param pointList 位置点序列
	 * @param clusters 簇
	 * @param para_epsilon 聚簇半径
	 * @return 位置点序列对应的簇标号
	 */
	private static ArrayList<Integer> getClusterNoList(ArrayList<GPSPoint> pointList, 
													   ArrayList<ArrayList<GPSPoint>> clusters,
													   double para_epsilon){
		ArrayList<Integer> clusterNoList = new ArrayList<Integer>();
		//1. 遍历pointList，找到在clusters中的标号，若找不到，设为-1.
		for(GPSPoint point:pointList){
			clusterNoList.add(getClusterNo(point, clusters, para_epsilon));
		}
		//2. 遍历clusterNoList，将-1标号改为和前面标号一样的。
		for(int i=0;i<clusterNoList.size();i++){
			if(i>0 && clusterNoList.get(i) == -1){
				clusterNoList.set(i, clusterNoList.get(i-1));
			}
		}
		return clusterNoList;
	}
	
	private static int getClusterNo(GPSPoint point, 
			   ArrayList<ArrayList<GPSPoint>> clusters,
			   double para_epsilon){
		//如果point距某个簇中某个点的距离，小于para_epsilon，则认为属于该簇
		GPS g1 = new GPS(point.getLang(), point.getLat());
		for(int i=0;i<clusters.size();i++){
			for(GPSPoint p:clusters.get(i)){
				GPS g2 = new GPS(p.getLang(), p.getLat());
				if(ConvertDistGPS.ConvertLogLatToDistance(g1, g2)<para_epsilon){
					return i;
				}
			}
		}
		return -1;
	}
	
	/**
	 * 获得训练对应的pair序列
	 * 注意：clusterNoList前面部分有可能都没有对应的簇，即标号为-1。这部分要舍弃，对应的cellList相应部分也要舍弃
	 * @param cellList cell序列，还要进一步转化为integer序列
	 * @param clusterNoList 簇标号序列
	 * @return 一条轨迹对应的用于训练hmm的pair序列.pair<hiddenNo, observationNo>
	 */
	private static ArrayList<Pair> getPairList(ArrayList<GPSCell> cellList, ArrayList<Integer> clusterNoList){
		ArrayList<Pair> pairList = new ArrayList<Pair>();
		//cellNo = X * Parameter.gridWidth + Y
		for(int clusterIndex=0;clusterIndex<clusterNoList.size();clusterIndex++){
			if(clusterNoList.get(clusterIndex) == -1) continue;
			int hiddenNo = clusterNoList.get(clusterIndex);
			int obsNo = cellList.get(clusterIndex).getGridX() * Parameter.gridWidth + cellList.get(clusterIndex).getGridY();
			pairList.add(new Pair(hiddenNo, obsNo));
		}
		return pairList;
	}
	
	
	public static void testOne() throws FileNotFoundException{
		Parameter.cellWidth = 0.2;
		Parameter.gridWidth = 200;//保证grid边长为40km
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = "";
		ArrayList<String> testUserIdList = new ArrayList<String>();
		sql = "SELECT DISTINCT UserId FROM goodtratotest";
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next()){
				testUserIdList.add(rs.getString("UserId"));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			JdbcUtil.close(conn, stmt);
		}
		//得到每个测试用户的全部轨迹号。
		ArrayList<TestUser> testUserList = new ArrayList<TestUser>();
		for(String userId:testUserIdList){
			ArrayList<String> userTraNo = new ArrayList<String>();
			sql = "SELECT TraNum FROM goodTra WHERE UserId='"+userId+"'";
			try{
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next()){
					userTraNo.add(rs.getString("TraNum"));
				}
			}catch(SQLException e){
				e.printStackTrace();
			}finally{
				JdbcUtil.close(conn, stmt);
			}
			testUserList.add(new TestUser(userId, userTraNo));
		}
		System.setOut(new PrintStream(new File("F:\\OneDrive\\NextPositionPrediction\\实验\\1006\\tmp1")));
//		System.out.println("Parameter.cellWidth="+Parameter.cellWidth);
//		System.out.println("Parameter.gridWidth="+Parameter.gridWidth);
		System.out.println();
		
		long startMillis = System.currentTimeMillis();
		//对于测试集中的每个用户
		for(TestUser testUser:testUserList){
			System.gc();
			int userSuccess = 0;
			int userFail = 0;
			//计数
			System.out.println("user："+testUser.getUserId()+";traCnt:"+testUser.getEffectiveTraNo().size());
			String lngStr = "116.39719";
			String latStr = "39.916538";
			
			int[][] numArr = new int[Parameter.gridWidth][Parameter.gridWidth];
			
			GPS gpsCenter = new GPS(lngStr, latStr);
			//得到Grid原点坐标，即Grid左下角坐标
			GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);
			//grid内的训练轨迹集，内部也做了同样的轨迹过滤
			ArrayList<ArrayList<GPSCell>> cellTraList = new ArrayList<ArrayList<GPSCell>>();
			ArrayList<ArrayList<GPSPoint>> traPointsListList = new ArrayList<ArrayList<GPSPoint>>();
			
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			int traCount=0;
			for(String traNo:testUser.getEffectiveTraNo()){//只用前0.75的轨迹做训练集
				if(traCount++>testUser.getEffectiveTraNo().size()*0.75){
					break;
				}
				ArrayList<GPSPoint> traPointsList = new ArrayList<GPSPoint>();
				//临时注释
				sql = "SELECT Longitude,Latitude,DateTime FROM filteredpoints where TraNum='"+traNo+"' ORDER BY DateTime";
//				sql = "SELECT Longitude,Latitude,DateTime FROM filteredpoints where TraNum='"+traNo+"' and "
//						+ "Longitude<'116.34' and Longitude>'116.32' and Latitude<'40.00' and Latitude>'39.98' ORDER BY DateTime";
				try {
					rs = stmt.executeQuery(sql);
					while(rs.next()){
						GPSPoint point = new GPSPoint(rs.getString("Longitude"), rs.getString("Latitude"), rs.getString("DateTime"));
						traPointsList.add(point);
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//数据库中已是过滤结束的
				//traPointsList = TraFilter.getSparsedTra(traPointsList);//不用过滤了过滤
				ResultTypeQiao resultTypeQiao = GridUtil.GPSTraToCellTraQiao(traPointsList, originGps);
				cellTraList.add(resultTypeQiao.cellList);
				traPointsListList.add(resultTypeQiao.pointList);//cellList和pointList长度相等，cellTraLIst和trapointsListLIst长度也相等
				//在聚类过程中，每个cell只保留一个位置点。这样会减小簇计算过程中使用的位置点数，10k+降到2k。这样其实就不是基于密度聚类了。可以试一下。
				//会出现某一个类别点数过高，其他过低的现象。可以在聚类过程中判断当前类别点数，提前终止扩张操作。
			}
			JdbcUtil.close(conn, stmt);
			
			//所有轨迹轨迹点序列：traPointsListList list of list of GPSPoint
			//所有轨迹cell序列：cellTraList list of list of GPSCell
			//1.用户轨迹点聚类
			//2.用户每条轨迹对应的隐状态序列
			//3.用户每条轨迹对应的观察状态序列，2和3一一对应
			//4.测试轨迹测试
			double para_epsilon = 0.3;//聚类半径，2km
			int para_theta = 6;//最少点数，10个
			ArrayList<GPSPoint> allPoints = new ArrayList<GPSPoint>();//所有位置点
			ArrayList<ArrayList<GPSPoint>> clusters = new ArrayList<ArrayList<GPSPoint>>();
			for(ArrayList<GPSPoint> list:traPointsListList){
				for(GPSPoint point:list){
					allPoints.add(point);//浅拷贝 
				}
			}
			System.out.println("网格points总数："+allPoints.size());
			
			Para pa = new Para();//调试时计数
			for(GPSPoint point:allPoints){
				if(point.visited == true){
					continue;
				}
				point.visited = true;
				ArrayList<GPSPoint> neighbours = neighbours(point, para_epsilon, allPoints);
				if(neighbours.size()<para_theta){
					//point is a noize
					pa.noize++;
				}else{
					ArrayList<GPSPoint> cluster = new ArrayList<GPSPoint>();//新建一个簇
					ExpandCluster(point, neighbours, cluster, allPoints, para_epsilon, para_theta,pa);
					clusters.add(cluster);
				}
			}
			System.out.println("clusters.size:"+clusters.size());
			int pointInClusters = 0;
			for(ArrayList<GPSPoint> list:clusters){
//				System.out.println("size:"+list.size());
				pointInClusters += list.size();
			}
			System.out.println("noize:"+pa.noize);
			System.out.println("pointInCluster:"+pointInClusters);
			System.out.println("total:"+(pa.noize+pointInClusters)+"/"+allPoints.size());

			long endMillis = System.currentTimeMillis();
			System.out.println("cluster time:"+(endMillis-startMillis)/1000+"s");
			/*****************************************以上获得clusters***********************************************/
			/*****************************************以下获得每条训练轨迹的隐状态和观察状态***********************************/
			//cellTraList，可以用cell的xy坐标计算cell标号，作为观察状态
			//traPointsListList，和clusters一起获得隐状态标号。cellTra和traPointsList长度相等；cellTraLIst和traPointsListList长度也相等。
			//clusters
			ArrayList<ArrayList<Pair>> trainDataListList = new ArrayList<ArrayList<Pair>>();//pair<hiddenNo, observationNo>
			for(int i=0;i<traPointsListList.size();i++){//遍历每条轨迹 
				ArrayList<GPSPoint> pointList = traPointsListList.get(i);//当前轨迹点序列
				ArrayList<GPSCell> cellList = cellTraList.get(i);//当前轨迹cell序列
				ArrayList<Integer> clusterNoList = getClusterNoList(pointList, clusters, para_epsilon);//当前轨迹clusterNo序列
				ArrayList<Pair> pairList = getPairList(cellList, clusterNoList);
				trainDataListList.add(pairList);
			}
			/*****************************************以下训练hmm***********************************/
			int hiddenCnt = clusters.size();
			int obsCnt = Parameter.gridWidth * Parameter.gridWidth;
			Learn learn = new Learn(hiddenCnt, obsCnt, trainDataListList);
			/*****************************************以下对每条测试轨迹进行测试***********************************/
			Forward forward = new Forward(hiddenCnt, obsCnt);
	        forward.A=learn.A;
	        forward.B=learn.B;
	        forward.PI=learn.PI;
			
	        //测试轨迹 -> cell序列 -> cell子序列 -> hmm下一个隐状态，下一个cell -> 是否匹配
	        ArrayList<String> testTraNoList = new ArrayList<String>();
			//数据库中指定的测试轨迹
			sql = "SELECT TraNum FROM goodtratotest WHERE UserId='"+testUser.getUserId()+"'";
			try{
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next()){
					testTraNoList.add(rs.getString("TraNum"));
				}
			}catch(SQLException e){
				e.printStackTrace();
			}finally{
				JdbcUtil.close(conn, stmt);
			}
			System.out.println("testTraCnt:"+testTraNoList.size());
			//对每个测试轨迹进行测试
			for(String testTraNo:testTraNoList){
				//得到与当前测试轨迹对应的训练集轨迹号
//				System.out.println("current tra:"+testTraNo);
				conn = null;
				stmt = null;
				rs = null;
				sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM location WHERE UserId='"+testUser.getUserId()+"' AND TraNum='"+testTraNo+"'";
				ArrayList<GPSPoint> testTraGpsPoints = new ArrayList<GPSPoint>();
				try {
					conn = JdbcUtil.getConnection();
					stmt = conn.createStatement();
					rs = stmt.executeQuery(sql);
					while(rs.next()){
						testTraGpsPoints.add(new GPSPoint(rs.getString("Long_gcj"), rs.getString("Lat_gcj"), rs.getString("DateTime")));
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				JdbcUtil.close(conn, stmt);
				testTraGpsPoints = TraFilter.getSparsedTra(testTraGpsPoints);//得到稀疏后的测试轨迹
				//cell完整序列
				ArrayList<GPSCell> cellList = GridUtil.GPSTraToCellTra(testTraGpsPoints, originGps);
				//对cell序列进行切割，得到多个测试子序列
				if(cellList.size()<3) continue;
				for(int cellIndex=1;cellIndex<cellList.size()-1;cellIndex++){
					ArrayList<Integer> obsList = new ArrayList<Integer>();//观测序列
					for(int i=0;i<=cellIndex;i++){//取出i之前的所有cell作为观测序列，不知道这种和 固定观测序列长度 的方法哪个预测准确率高？
						GPSCell cell = cellList.get(i);
						int obsNo = cell.getGridX() * Parameter.gridWidth + cell.getGridY();
						obsList.add(obsNo);
					}
					int[] ob = new int[obsList.size()];
					for(int i=0;i<ob.length;i++){
						ob[i] = obsList.get(i);
					}
					/*************************以下乔算法***********************/
					double[] prLast = forward.forwardQiao(ob);
					//viterbi
					Viterbi viterbi = new Viterbi(hiddenCnt, obsCnt);
			        viterbi.A=learn.A;
			        viterbi.B=learn.B;
			        viterbi.PI=learn.PI;
			        double probability = 0;
			        List list=viterbi.viterbi(ob,probability);
			        int[] Q = (int[]) list.get(0);//返回隐藏状态序列
			        /*System.out.print("最可能的隐藏状态序列为：{");
			        for(int value:Q)
			        {
			        	System.out.print(value+" ");
			        }
			        System.out.println("}");
			        System.out.println("最大可能性为："+list.get(1));*/
			        double[] prNext = new double[hiddenCnt];
			        for(int i=0;i<hiddenCnt;i++){
			        	for(int ii:Q){
			        		if(i == ii) continue;
			        	}
			        	for(int j=0;j<hiddenCnt;j++){
			        		prNext[i] += prLast[j] * learn.A[j][i];
			        	}
			        }
			        double max = 0.0;
			        int maxHidden = -1;//预测结果，下一个隐状态
			        for(int i=0;i<prNext.length;i++){
			        	if(prNext[i] > max){
			        		max = prNext[i];
			        		maxHidden = i;
			        	}
			        }

					double avgMis = -9999;
			        if(maxHidden == -1){//失败
			        	//avgMis = -9999;
			        }else{
			        	GPSCell nextCell = cellList.get(cellIndex + 1);//下一个cell，实际结果
			        	ArrayList<GPSPoint> cluster = clusters.get(maxHidden);
			        	GPSPoint nextCellCenter = GridUtil.CellToGPS(nextCell, originGps);
			        	avgMis = 0.0;
			        	for(GPSPoint point:cluster){
			        		avgMis += ConvertDistGPS.ConvertLogLatToDistance(nextCellCenter, point);
			        	}
			        	avgMis /= cluster.size();
			        }
			        System.out.println(formatDouble(avgMis));
					/*************************以上乔算法***********************/
			        //根据误差确定对该用户的预测精度。
			        
				}//each testObsTra
			}//each testTra
		}//each user
		
	}

	private static String formatDouble(double d){
        DecimalFormat df = new DecimalFormat("0.00");     
        return df.format(d);
    }
	public static void main(String[] args) throws FileNotFoundException {
		//
//		testOne();
		
//		System.out.println(ConvertDistGPS.ConvertLogLatToDistance(new GPS("116.1","36.1"),new GPS("116.1","36.1")));
//		System.out.println(ConvertDistGPS.ConvertDistanceToLogLat(new GPS("116","36"),100,90));
/*		Parameter.cellWidth = 0.2;
		Parameter.gridWidth = 200;
		String lngStr = "116.39719";
		String latStr = "39.916538";
		GPS gpsCenter = new GPS(lngStr, latStr);
		GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);
		
		System.out.println("origin:"+originGps);//116.1622683759934, 39.73635781981982
		String lang = "116.16343990766872";
		String lat = "39.737258720720725";
		GPSCell cell = GridUtil.GPSToGPSCell(new GPSPoint(lang, lat, ""), originGps);
		System.out.println(cell);
		
		GPSPoint point1 = GridUtil.CellToGPS(new GPSCell(64, 154, ""), originGps);
		GPSPoint point2 = GridUtil.CellToGPS(new GPSCell(65, 156, ""), originGps);
//		System.out.println(point);
		
		GPS g1 = new GPS("116.91322017981707","41.12464610810811");
		GPS g2 = new GPS("116.92493549656939","41.14266412612613");
		System.out.println(ConvertDistGPS.ConvertLogLatToDistance(point1, point2));*/
	}

}
