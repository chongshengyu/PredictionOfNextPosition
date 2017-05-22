package com.yu.evaluation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import com.alibaba.fastjson.JSON;
import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.GridCell;
import com.yu.draw.entity.Parameter;
import com.yu.draw.entity.Region;
import com.yu.draw.entity.RegionModel;
import com.yu.draw.entity.RegionTime;
import com.yu.draw.util.GridUtil;
import com.yu.draw.util.TraFilter;
import com.yu.evaluation.entity.HeatMapCell;
import com.yu.evaluation.entity.ResultScores;
import com.yu.evaluation.entity.TestUser;
import com.yu.evaluation.util.TraUtil;
import com.yu.prepare.util.JdbcUtil;

public class EvaAlgoriOne_1 {
	/**
	 * -----------------------------------------------------该实验，训练集和测试集全部使用的手工选择出的用户及其部分轨迹
	 * 
	 * --测试集数量1/10,从后1/2的轨迹中随机选择(实际是手工选择出的)1/10的轨迹作为测试集，使用测试轨迹之前的所有轨迹作为训练集。训练集大小不固定
	 * 
	 * 对于每个点的预测结果，可能出现的情况有：
	 */
	public static void testOne(){
		Connection conn = null;
		Statement stmt = null;
		String sql = "";
		//得到数据库中存储的，手工筛选出的用户
		ArrayList<String> testUserIdList = new ArrayList<String>();
		//使用013做测试·······························································································
//		testUserIdList.add("013");
		sql = "SELECT DISTINCT UserId FROM goodTra";
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
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
				ResultSet rs = stmt.executeQuery(sql);
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
		//对于测试集中的每个用户
		for(TestUser testUser:testUserList){
			//计数
			System.out.println("user："+testUser.getUserId()+";traCnt:"+testUser.getEffectiveTraNo().size());
			int userSuccessMy = 0;//预测正确
			int userWrongMy = 0;//预测错误
			int userSuccessRef = 0;//预测正确
			int userWrongRef = 0;//预测错误
			int userUnablePredictin = 0;//无法预测
			
			//随机选取测试集
			ArrayList<String> testTraNoList = new ArrayList<String>();//随机选出的测试轨迹号
			/*//随机
			for(int i=0;i<testUser.getEffectiveTraNo().size() * (1 - Parameter.PROP_OF_SAMPLE_SET);i++){//调试时可以指定，随机种子26，以生成同一个轨迹
				//可以指定随机数种子26+i*2
				int testTraIndex = new Random(26+i*2).nextInt(testUser.getEffectiveTraNo().size() / 2) + (testUser.getEffectiveTraNo().size() / 2) - 1;//选中的测试集索引
				testTraNoList.add(testUser.getEffectiveTraNo().get(testTraIndex));
			}*/
			//数据库中指定的测试轨迹
			sql = "SELECT TraNum FROM goodtratotest WHERE UserId='"+testUser.getUserId()+"'";
			try{
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				while(rs.next()){
					testTraNoList.add(rs.getString("TraNum"));
				}
			}catch(SQLException e){
				e.printStackTrace();
			}finally{
				JdbcUtil.close(conn, stmt);
			}
			System.out.println("testTraCnt:"+testTraNoList.size());
			/*for(String testTraNo:testTraNoList)
				System.out.println(testTraNo);*/
			//对每个测试轨迹进行测试
			for(String testTraNo:testTraNoList){
				//得到与当前测试轨迹对应的训练集轨迹号
				ArrayList<String> trainTraNoList = new ArrayList<String>();
				for(String testTraThis:testUser.getEffectiveTraNo()){
					if(testUser.getEffectiveTraNo().indexOf(testTraThis) < testUser.getEffectiveTraNo().indexOf(testTraNo)){
						trainTraNoList.add(testTraThis);
					}else{
						break;//testTraNo之前的轨迹作为训练轨迹，已经全部找出
					}
				}
				//以01320081209230723为第一条测试·································································
//				testTraNo = "01320081209230723";
				System.out.println("current tra:"+testTraNo);
				int traSuccessMy = 0;//预测正确
				int traWrongMy = 0;//预测错误
				int traSuccessRef = 0;//预测正确
				int traWrongRef = 0;//预测错误
				int traUnablePredictin = 0;//无法预测
				conn = null;
				stmt = null;
				sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM location WHERE UserId='"+testUser.getUserId()+"' AND TraNum='"+testTraNo+"'";
				ArrayList<GPSPoint> testTraGpsPoints = new ArrayList<GPSPoint>();
				try {
					conn = JdbcUtil.getConnection();
					stmt = conn.createStatement();
					ResultSet rs = stmt.executeQuery(sql);
					while(rs.next()){
						testTraGpsPoints.add(new GPSPoint(rs.getString("Long_gcj"), rs.getString("Lat_gcj"), rs.getString("DateTime")));
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				JdbcUtil.close(conn, stmt);
				testTraGpsPoints = TraFilter.getSparsedTra(testTraGpsPoints);//得到稀疏后的轨迹
				for(GPSPoint gpsPoint:testTraGpsPoints){
					//对于测试轨迹中的每个gps点进行测试
					//得到Grid左上角和右下角坐标
					GPS gpsCenter = new GPS(gpsPoint.getLang(), gpsPoint.getLat());
					String lu_lng = GridUtil.getGridLUGpsByCenter(gpsCenter).getLongitude();
					String lu_lat = GridUtil.getGridLUGpsByCenter(gpsCenter).getLatitude();
					String rd_lng = GridUtil.getGridRDGpsByCenter(gpsCenter).getLongitude();
					String rd_lat = GridUtil.getGridRDGpsByCenter(gpsCenter).getLatitude();
					//得到Grid原点坐标
					GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);
					//grid内的训练轨迹集，内部也做了同样的轨迹过滤
					ArrayList<ArrayList<GPSPoint>> traList = TraUtil.getTraInGrid(testUser.getUserId(), lu_lng, lu_lat, rd_lng, rd_lat,trainTraNoList);
					if(traList.size() == 0){
						System.out.println("该点附近没有历史轨迹");
						continue;
					}
					//将轨迹转换成cell序列
					ArrayList<ArrayList<GPSCell>> cellTraList = new ArrayList<ArrayList<GPSCell>>();
					for(ArrayList<GPSPoint> tra:traList){
						ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(tra, originGps);//这里没有实现插值，是否有影响？？
						cellTraList.add(cellTra);
					}
					
					//得到regions
					int[][] numArr = new int[Parameter.gridWidth][Parameter.gridWidth];
					for(ArrayList<GPSCell> cellTra:cellTraList){
						for(GPSCell cell:cellTra){
							int x = cell.getGridX();
							int y = cell.getGridY();
							numArr[x][y] += 1;
						}
					}
					LinkedList<Region> regionList = GridUtil.getRegionListByPointNum(numArr, originGps);
					
					//每条轨迹对应的regionTra
					ArrayList<ArrayList<RegionTime>> regionTraList = GridUtil.cellTraList2RegionTraList(cellTraList, regionList);
					//regionTraList => modelMap
					HashMap<Region, ArrayList<RegionModel>> modelMap = GridUtil.getModelMap(regionList, regionTraList);
					String time = gpsPoint.getDateTime();
					ArrayList<GPS> knownGpsPoint = new ArrayList<GPS>();
					knownGpsPoint.add(new GPS(gpsPoint.getLang(),gpsPoint.getLat()));

					//根据历史数据得到的regions里不能包含当前待预测的点怎么办????????????????????????????????
					HashMap<Region, ResultScores> scoreMap = TraUtil.getScoreMap(knownGpsPoint, originGps, time, modelMap);//设置不同算法，得到不同分数
					if(scoreMap == null){
						traUnablePredictin ++;
						continue;
					}
					//找到分值最大的region????这个地方，有时候不连续？？？？？
					Region predictionRegionMy = null;
					Region predictionRegionRef = null;
					double scoreMaxMy = 0.0;
					double scoreMaxRef = 0.0;
					for(Region region:scoreMap.keySet()){
						//我的算法得分
						if((double)scoreMap.get(region).getMyScore() > scoreMaxMy){
							predictionRegionMy = region;
							scoreMaxMy = (double)scoreMap.get(region).getMyScore();
						}
						//参考算法得分
						if((double)scoreMap.get(region).getRefScore() > scoreMaxRef){
							predictionRegionRef = region;
							scoreMaxRef = (double)scoreMap.get(region).getRefScore();
						}
					}
					//判断预测是否正确
					//截取得到从当前gpsPoint到当前轨迹结束的轨迹，转换成regions，判断第二个region是否与预测的region相同
					ArrayList<GPSPoint> restGpsPoints = new ArrayList<GPSPoint>();
					for(int i=testTraGpsPoints.indexOf(gpsPoint);i<testTraGpsPoints.size();i++){
						//判断接下来的点还在grid内部，否则结束
						String lng = testTraGpsPoints.get(i).getLang();
						String lat = testTraGpsPoints.get(i).getLat();
						if(Double.parseDouble(lng) > Double.parseDouble(lu_lng) && Double.parseDouble(lat) < Double.parseDouble(lu_lat) && Double.parseDouble(lng) < Double.parseDouble(rd_lng) && Double.parseDouble(lat) > Double.parseDouble(rd_lat)){
							restGpsPoints.add(testTraGpsPoints.get(i));
						}else{//已经越出grid
							break;
						}
					}
					//安全转成region
					//将轨迹转换成cell序列
					ArrayList<GPSCell> restCellTra = GridUtil.GPSTraToCellTra(restGpsPoints, originGps);
					//每条轨迹对应的regionTra
					ArrayList<RegionTime> regionTra = TraUtil.cellTra2RegionTra(restCellTra, regionList);
					if(regionTra == null){
						userUnablePredictin ++;
						traUnablePredictin ++;
						continue;
					}
					if(regionTra.size() < 2){
						userUnablePredictin ++;
						traUnablePredictin ++;
						continue;
					}
					if(regionTra.get(1).getRegion().equals(predictionRegionMy)){//正确
						userSuccessMy ++;
						traSuccessMy ++;
					}else{//错误
						userWrongMy ++;
						traWrongMy ++;
					}
					if(regionTra.get(1).getRegion().equals(predictionRegionRef)){//正确
						userSuccessRef ++;
						traSuccessRef ++;
					}else{//错误
						userWrongRef ++;
						traWrongRef ++;
					}
				}
//				System.out.println("traMy:"+testTraNo+",successMy:"+traSuccessMy+",wrongMy:"+traWrongMy+",unable:"+traUnablePredictin);
//				System.out.println("traRef:"+testTraNo+",successRef:"+traSuccessRef+",wrongRef:"+traWrongRef+",unable:"+traUnablePredictin);
			}
			System.out.println("userMy:"+testUser.getUserId()+",successMy:"+userSuccessMy+",wrongMy:"+userWrongMy+",unable:"+userUnablePredictin);
			System.out.println("userRef:"+testUser.getUserId()+",successRef:"+userSuccessRef+",wrongRef:"+userWrongRef+",unable:"+userUnablePredictin);
			System.out.println((double)userSuccessMy/(userSuccessMy+userWrongMy+userUnablePredictin));
			System.out.println((double)userSuccessRef/(userSuccessRef+userWrongRef+userUnablePredictin));
		}
	}
	
	//每个用户轨迹过滤前后的轨迹点数
	public static void testTwo(){
		Connection conn = null;
		Statement stmt = null;
		String sql = "";
		//得到数据库中存储的，手工筛选出的用户
		ArrayList<String> testUserIdList = new ArrayList<String>();
		sql = "SELECT DISTINCT UserId FROM goodTra";
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
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
				ResultSet rs = stmt.executeQuery(sql);
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
		//对于测试集中的每个用户
		for(TestUser testUser:testUserList){
			System.out.println("UserId:"+testUser.getUserId());
			int pointsBefore = 0;
			int pointsAfter = 0;
			//每条轨迹
			for(String traNo:testUser.getEffectiveTraNo()){
				ArrayList<GPSPoint> pointsList = new ArrayList<GPSPoint>();
				sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM location WHERE UserId='"+testUser.getUserId()+"' AND TraNum='"+traNo+"'";
				try{
					conn = JdbcUtil.getConnection();
					stmt = conn.createStatement();
					ResultSet rs = stmt.executeQuery(sql);
					while(rs.next()){
						pointsList.add(new GPSPoint(rs.getString("Long_gcj"), rs.getString("Lat_gcj"), rs.getString("DateTime")));
					}
				}catch(SQLException e){
					e.printStackTrace();
				}finally{
					JdbcUtil.close(conn, stmt);
				}
				pointsBefore += pointsList.size();
				pointsAfter += TraFilter.getSparsedTra(pointsList).size();
			}
			System.out.println("Before:"+pointsBefore);
			System.out.println("After:"+pointsAfter+"\n");
		}
	}
	
	//region总数统计
	public static void testThree(){
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = "";
		ArrayList<String> testUserIdList = new ArrayList<String>();
		sql = "SELECT DISTINCT UserId FROM goodTra";
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
		//对于测试集中的每个用户
		for(TestUser testUser:testUserList){
			int userTotalRegion1 = 0;
			int userTotalRegion2 = 0;
			int userTotalRegion3 = 0;
			int userTotalRegion4 = 0;
			int userTotalRegion5 = 0;
			//计数
			System.out.println("user："+testUser.getUserId()+";traCnt:"+testUser.getEffectiveTraNo().size());
			String lngStr = "116.39719";
			String latStr = "39.916538";
			Parameter.cellWidth = 0.2;
			Parameter.gridWidth = 200;//保证grid边长为40km
			
			int[][] numArr = new int[Parameter.gridWidth][Parameter.gridWidth];
			
			GPS gpsCenter = new GPS(lngStr, latStr);
			//得到Grid原点坐标，左下角坐标
			GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);
			//grid内的训练轨迹集，内部也做了同样的轨迹过滤
			ArrayList<ArrayList<GPSPoint>> traList = new ArrayList<ArrayList<GPSPoint>>();
			int traCount=0;
			
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			for(String traNo:testUser.getEffectiveTraNo()){
				traCount++;
				ArrayList<GPSPoint> traPointsList = new ArrayList<GPSPoint>();
				sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM Location where TraNum='"+traNo+"' ORDER BY DateTime";
				try {
					rs = stmt.executeQuery(sql);
					while(rs.next()){
						GPSPoint point = new GPSPoint(rs.getString("Long_gcj"), rs.getString("Lat_gcj"), rs.getString("DateTime"));
						traPointsList.add(point);
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally{
					//JdbcUtil.close(conn, stmt);
				}
				traPointsList = TraFilter.getSparsedTra(traPointsList);//过滤
//				traList.add(traPointsList);
				ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(traPointsList, originGps);
				for(GPSCell cell:cellTra){
					int x = cell.getGridX();
					int y = cell.getGridY();
					numArr[x][y] += 1;
				}
//				System.out.println("traCount:"+traCount);
			}
			JdbcUtil.close(conn, stmt);
			
			Parameter.MAXREGIONWIDTH = 1;
			LinkedList<Region> regionList1 = GridUtil.getRegionListByPointNum(numArr, originGps);
			userTotalRegion1 += regionList1.size();
			Parameter.MAXREGIONWIDTH = 2;
			LinkedList<Region> regionList2 = GridUtil.getRegionListByPointNum(numArr, originGps);
			userTotalRegion2 += regionList2.size();
			Parameter.MAXREGIONWIDTH = 3;
			LinkedList<Region> regionList3 = GridUtil.getRegionListByPointNum(numArr, originGps);
			userTotalRegion3 += regionList3.size();
			Parameter.MAXREGIONWIDTH = 4;
			LinkedList<Region> regionList4 = GridUtil.getRegionListByPointNum(numArr, originGps);
			userTotalRegion4 += regionList4.size();
			Parameter.MAXREGIONWIDTH = 5;
			LinkedList<Region> regionList5 = GridUtil.getRegionListByPointNum(numArr, originGps);
			userTotalRegion5 += regionList5.size();
			
			System.out.println("======================================");
			System.out.println("userTotalRegion1:"+userTotalRegion1);
			System.out.println("----------------------");
			System.out.println("userTotalRegion2:"+userTotalRegion2);
			System.out.println("----------------------");
			System.out.println("userTotalRegion3:"+userTotalRegion3);
			System.out.println("----------------------");
			System.out.println("userTotalRegion4:"+userTotalRegion4);
			System.out.println("----------------------");
			System.out.println("userTotalRegion5:"+userTotalRegion5);
			System.out.println("======================================");
		}
	}
	
	//预测结果评价，使用已知region的方式
	public static void testFour(){
		Parameter.cellWidth = 0.2;
		Parameter.gridWidth = 200;//保证grid边长为40km
		Parameter.MAXREGIONWIDTH = 1;
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = "";
		ArrayList<String> testUserIdList = new ArrayList<String>();
		sql = "SELECT DISTINCT UserId FROM goodTra";
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
		
		System.out.println("Parameter.cellWidth="+Parameter.cellWidth);
		System.out.println("Parameter.gridWidth="+Parameter.gridWidth);
		System.out.println("Parameter.MAXREGIONWIDTH="+Parameter.MAXREGIONWIDTH);
		System.out.println();
		
		//对于测试集中的每个用户
		for(TestUser testUser:testUserList){
			System.gc();
			int userSuccessMy_1 = 0;
			int userFailMy_1 = 0;
			int userSuccessRef_1 = 0;
			int userFailRef_1 = 0;
			int userSuccessMy_2 = 0;
			int userFailMy_2 = 0;
			int userSuccessRef_2 = 0;
			int userFailRef_2 = 0;
			int userUnable = 0;
			//计数
			System.out.println("user："+testUser.getUserId()+";traCnt:"+testUser.getEffectiveTraNo().size());
			String lngStr = "116.39719";
			String latStr = "39.916538";
			
			int[][] numArr = new int[Parameter.gridWidth][Parameter.gridWidth];
			
			GPS gpsCenter = new GPS(lngStr, latStr);
			//得到Grid原点坐标，左下角坐标
			GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);
			//grid内的训练轨迹集，内部也做了同样的轨迹过滤
			ArrayList<ArrayList<GPSCell>> cellTraList = new ArrayList<ArrayList<GPSCell>>();
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			int traCount=0;
			for(String traNo:testUser.getEffectiveTraNo()){//只用前1/2的轨迹做训练集
				if(traCount++>testUser.getEffectiveTraNo().size()*0.75){
					break;
				}
				ArrayList<GPSPoint> traPointsList = new ArrayList<GPSPoint>();
				sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM Location where TraNum='"+traNo+"' ORDER BY DateTime";
				try {
					rs = stmt.executeQuery(sql);
					while(rs.next()){
						GPSPoint point = new GPSPoint(rs.getString("Long_gcj"), rs.getString("Lat_gcj"), rs.getString("DateTime"));
						traPointsList.add(point);
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				traPointsList = TraFilter.getSparsedTra(traPointsList);//过滤
				ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(traPointsList, originGps);
				cellTraList.add(cellTra);
				for(GPSCell cell:cellTra){
					int x = cell.getGridX();
					int y = cell.getGridY();
					numArr[x][y] += 1;
				}
				System.out.println("读入第？轨迹:"+traCount);
			}
			JdbcUtil.close(conn, stmt);
			System.out.println("轨迹读入结束，训练集大小："+traCount);
			
			LinkedList<Region> regionList = GridUtil.getRegionListByPointNum(numArr, originGps);
			System.out.println("regionListSize:"+regionList.size());
			ArrayList<ArrayList<RegionTime>> regionTraList = GridUtil.cellTraList2RegionTraList(cellTraList, regionList);
			HashMap<Region, ArrayList<RegionModel>> modelMap = GridUtil.getModelMap(regionList, regionTraList);
			System.out.println("训练结束");
			
			//以上训练，以下测试

			ArrayList<String> testTraNoList = new ArrayList<String>();//测试轨迹号
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

			for(String testTraNo : testTraNoList){//对于每条测试轨迹
				int traSuccessMy_1 = 0;
				int traFailMy_1 = 0;
				int traSuccessRef_1 = 0;
				int traFailRef_1 = 0;
				int traSuccessMy_2 = 0;
				int traFailMy_2 = 0;
				int traSuccessRef_2 = 0;
				int traFailRef_2 = 0;
				int traUnable = 0;
				
				ArrayList<GPSPoint> traPointsList = new ArrayList<GPSPoint>();
				try{
					conn = JdbcUtil.getConnection();
					stmt = conn.createStatement();
				}catch(SQLException e){
					e.printStackTrace();
				}
				sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM Location where TraNum='"+testTraNo+"' ORDER BY DateTime";
				try{
					rs = stmt.executeQuery(sql);
					while(rs.next()){
						GPSPoint point = new GPSPoint(rs.getString("Long_gcj"), rs.getString("Lat_gcj"), rs.getString("DateTime"));
						traPointsList.add(point);
					}
				}catch(SQLException e){
					e.printStackTrace();
				}
				JdbcUtil.close(conn, stmt);
				traPointsList = TraFilter.getSparsedTra(traPointsList);//过滤
				ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(traPointsList, originGps);//得到对应的cellTra
				ArrayList<ArrayList<GPSCell>> tmp = new ArrayList<ArrayList<GPSCell>>();
				tmp.add(cellTra);
				ArrayList<ArrayList<RegionTime>> testRegionTraList = GridUtil.cellTraList2RegionTraList(tmp, regionList);
				ArrayList<RegionTime> testRegionTra = testRegionTraList.get(0);//测试轨迹对应的regionTra
				if(testRegionTra.size()<4){
					System.out.println("当前测试轨迹region总数小于4");
					System.out.println(testTraNo);
					System.out.println(testRegionTra.size());
					continue;
				}
				//只需要测试已知一个region，和已知两个region的情况。
				ArrayList<RegionTime> knownRegionList = new ArrayList<RegionTime>();
				for(int knowRegionNum=1;knowRegionNum<3;knowRegionNum++){//已知的region数，取值为1,2
//					System.out.println("knowRegionNun:"+knowRegionNum);
					for(int regionIndex=0;regionIndex<testRegionTra.size()-knowRegionNum;regionIndex++){//遍历testRegionTra中的region进行测试
						knownRegionList.clear();
						for(int i=0;i<knowRegionNum;i++){
							knownRegionList.add(testRegionTra.get(regionIndex + i));
						}
						HashMap<Region, ResultScores> scoreMap = TraUtil.getScoreMap(knownRegionList, modelMap);
						if(null == scoreMap){
							userUnable ++;
							traUnable ++;
							continue;
						}
						Region realRegion = testRegionTra.get(regionIndex + knowRegionNum).getRegion();
						Region predictionRegionMy = null;
						Region predictionRegionRef = null;
						double scoreMaxMy = 0.0;
						double scoreMaxRef = 0.0;
						for(Region region:scoreMap.keySet()){
							//我的算法得分
							if((double)scoreMap.get(region).getMyScore() > scoreMaxMy){
								predictionRegionMy = region;
								scoreMaxMy = (double)scoreMap.get(region).getMyScore();
							}
							//参考算法得分
							if((double)scoreMap.get(region).getRefScore() > scoreMaxRef){
								predictionRegionRef = region;
								scoreMaxRef = (double)scoreMap.get(region).getRefScore();
							}
						}
						
						if(knowRegionNum == 1){//已知一点
							if(realRegion.equals(predictionRegionMy)){
								traSuccessMy_1 ++;
								userSuccessMy_1 ++;
							}else{
								traFailMy_1 ++;
								userFailMy_1 ++;
							}
							if(realRegion.equals(predictionRegionRef)){
								traSuccessRef_1++;
								userSuccessRef_1++;
							}else{
								traFailRef_1++;
								userFailRef_1++;
							}
						}else if(knowRegionNum ==2){//已知两点
							if(realRegion.equals(predictionRegionMy)){
								traSuccessMy_2 ++;
								userSuccessMy_2 ++;
							}else{
								traFailMy_2 ++;
								userFailMy_2 ++;
							}
							if(realRegion.equals(predictionRegionRef)){
								traSuccessRef_2++;
								userSuccessRef_2++;
							}else{
								traFailRef_2++;
								userFailRef_2++;
							}
						}
					}
				}//已知的region数，取值为1,2
				/*System.out.println("traSuccessMy_1:"+traSuccessMy_1+";traFailMy_1:"+traFailMy_1+";traUnable:"+traUnable);
				System.out.println("traSuccessRef_1:"+traSuccessRef_1+";traFailRef_1:"+traFailRef_1+";traUnable:"+traUnable);
				System.out.println("traSuccessMy_2:"+traSuccessMy_2+";traFailMy_2:"+traFailMy_2+";traUnable:"+traUnable);
				System.out.println("traSuccessRef_2:"+traSuccessRef_2+";traFailRef_2:"+traFailRef_2+";traUnable:"+traUnable);*/
			}//tra
			System.out.println("userSuccessMy_1:"+userSuccessMy_1+";userFailMy_1:"+userFailMy_1+";userUnable:"+userUnable);
			System.out.println("userSuccessRef_1:"+userSuccessRef_1+";userFailRef_1:"+userFailRef_1+";userUnable:"+userUnable);
			System.out.println("userSuccessMy_2:"+userSuccessMy_2+";userFailMy_2:"+userFailMy_2+";userUnable:"+userUnable);
			System.out.println("userSuccessRef_2:"+userSuccessRef_2+";userFailRef_2:"+userFailRef_2+";userUnable:"+userUnable);
			System.out.println();
			cellTraList = null;
			testTraNoList = null;
			regionList = null;
			regionTraList = null;
			modelMap = null;
			numArr = null;
		}//user
	}

	//预测结果评价，使用数据库中过滤以后的位置点
	public static void testFive(){
		Parameter.cellWidth = 0.2;
		Parameter.gridWidth = 200;//保证grid边长为40km
		Parameter.MAXREGIONWIDTH = 2;
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = "";
		ArrayList<String> testUserIdList = new ArrayList<String>();
		sql = "SELECT DISTINCT UserId FROM goodTra";
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
		
		System.out.println("Parameter.cellWidth="+Parameter.cellWidth);
		System.out.println("Parameter.gridWidth="+Parameter.gridWidth);
		System.out.println("Parameter.MAXREGIONWIDTH="+Parameter.MAXREGIONWIDTH);
		System.out.println();
		
		//对于测试集中的每个用户
		for(TestUser testUser:testUserList){
			System.gc();
			int userSuccessMy_1 = 0;
			int userFailMy_1 = 0;
			int userSuccessRef_1 = 0;
			int userFailRef_1 = 0;
			int userSuccessMy_2 = 0;
			int userFailMy_2 = 0;
			int userSuccessRef_2 = 0;
			int userFailRef_2 = 0;
			int userUnable = 0;
			//计数
			System.out.println("user："+testUser.getUserId()+";traCnt:"+testUser.getEffectiveTraNo().size());
			String lngStr = "116.39719";
			String latStr = "39.916538";
			
			int[][] numArr = new int[Parameter.gridWidth][Parameter.gridWidth];
			
			GPS gpsCenter = new GPS(lngStr, latStr);
			//得到Grid原点坐标，左下角坐标
			GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);
			//grid内的训练轨迹集，内部也做了同样的轨迹过滤
			ArrayList<ArrayList<GPSCell>> cellTraList = new ArrayList<ArrayList<GPSCell>>();
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			int traCount=0;
			for(String traNo:testUser.getEffectiveTraNo()){//只用前1/2的轨迹做训练集
				if(traCount++>testUser.getEffectiveTraNo().size()*0.75){
					break;
				}
				ArrayList<GPSPoint> traPointsList = new ArrayList<GPSPoint>();
				sql = "SELECT Longitude,Latitude,DateTime FROM filteredpoints where TraNum='"+traNo+"' ORDER BY DateTime";
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
				ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(traPointsList, originGps);
				cellTraList.add(cellTra);
				for(GPSCell cell:cellTra){
					int x = cell.getGridX();
					int y = cell.getGridY();
					numArr[x][y] += 1;
				}
//				System.out.println("读入第？轨迹:"+traCount);
			}
			JdbcUtil.close(conn, stmt);
			System.out.println("轨迹读入结束，训练集大小："+traCount);
			
			LinkedList<Region> regionList = GridUtil.getRegionListByPointNum(numArr, originGps);
			System.out.println("regionListSize:"+regionList.size());
			ArrayList<ArrayList<RegionTime>> regionTraList = GridUtil.cellTraList2RegionTraList(cellTraList, regionList);
			HashMap<Region, ArrayList<RegionModel>> modelMap = GridUtil.getModelMap(regionList, regionTraList);
			System.out.println("训练结束");
			
			//实验时发现这里有错
			/*System.out.println("========================");
			for(Region region:modelMap.keySet()){
				System.out.println(region);
				System.out.println("------");
				for(RegionModel rm:modelMap.get(region)){
					System.out.println(rm);
				}
			}
			System.out.println("========================");*/
			
			//以上训练，以下测试

			ArrayList<String> testTraNoList = new ArrayList<String>();//测试轨迹号
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

			for(String testTraNo : testTraNoList){//对于每条测试轨迹
				int traSuccessMy_1 = 0;
				int traFailMy_1 = 0;
				int traSuccessRef_1 = 0;
				int traFailRef_1 = 0;
				int traSuccessMy_2 = 0;
				int traFailMy_2 = 0;
				int traSuccessRef_2 = 0;
				int traFailRef_2 = 0;
				int traUnable = 0;
				
				ArrayList<GPSPoint> traPointsList = new ArrayList<GPSPoint>();
				try{
					conn = JdbcUtil.getConnection();
					stmt = conn.createStatement();
				}catch(SQLException e){
					e.printStackTrace();
				}
				sql = "SELECT Longitude,Latitude,DateTime FROM filteredpoints where TraNum='"+testTraNo+"' ORDER BY DateTime";
				try{
					rs = stmt.executeQuery(sql);
					while(rs.next()){
						GPSPoint point = new GPSPoint(rs.getString("Longitude"), rs.getString("Latitude"), rs.getString("DateTime"));
						traPointsList.add(point);
					}
				}catch(SQLException e){
					e.printStackTrace();
				}
				JdbcUtil.close(conn, stmt);
				//使用数据库中过滤后的数据
//				traPointsList = TraFilter.getSparsedTra(traPointsList);//不用过滤了
				ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(traPointsList, originGps);//得到对应的cellTra
				ArrayList<ArrayList<GPSCell>> tmp = new ArrayList<ArrayList<GPSCell>>();
				tmp.add(cellTra);
				ArrayList<ArrayList<RegionTime>> testRegionTraList = GridUtil.cellTraList2RegionTraList(tmp, regionList);
				ArrayList<RegionTime> testRegionTra = testRegionTraList.get(0);//测试轨迹对应的regionTra
				if(testRegionTra.size()<4){
					System.out.println("当前测试轨迹region总数小于4");
					System.out.println(testTraNo);
					System.out.println(testRegionTra.size());
					continue;
				}
				//只需要测试已知一个region，和已知两个region的情况。
				ArrayList<RegionTime> knownRegionList = new ArrayList<RegionTime>();
				for(int knowRegionNum=1;knowRegionNum<3;knowRegionNum++){//已知的region数，取值为1,2
//						System.out.println("knowRegionNun:"+knowRegionNum);
					for(int regionIndex=0;regionIndex<testRegionTra.size()-knowRegionNum;regionIndex++){//遍历testRegionTra中的region进行测试
						knownRegionList.clear();
						for(int i=0;i<knowRegionNum;i++){
							knownRegionList.add(testRegionTra.get(regionIndex + i));
						}
						HashMap<Region, ResultScores> scoreMap = TraUtil.getScoreMap(knownRegionList, modelMap);
						if(null == scoreMap){
							userUnable ++;
							traUnable ++;
							continue;
						}
						Region realRegion = testRegionTra.get(regionIndex + knowRegionNum).getRegion();
						Region predictionRegionMy = null;
						Region predictionRegionRef = null;
						double scoreMaxMy = 0.0;
						double scoreMaxRef = 0.0;
						for(Region region:scoreMap.keySet()){
							//我的算法得分
							if((double)scoreMap.get(region).getMyScore() > scoreMaxMy){
								predictionRegionMy = region;
								scoreMaxMy = (double)scoreMap.get(region).getMyScore();
							}
							//参考算法得分
							if((double)scoreMap.get(region).getRefScore() > scoreMaxRef){
								predictionRegionRef = region;
								scoreMaxRef = (double)scoreMap.get(region).getRefScore();
							}
						}
						
						if(knowRegionNum == 1){//已知一点
							if(realRegion.equals(predictionRegionMy)){
								traSuccessMy_1 ++;
								userSuccessMy_1 ++;
							}else{
								traFailMy_1 ++;
								userFailMy_1 ++;
							}
							if(realRegion.equals(predictionRegionRef)){
								traSuccessRef_1++;
								userSuccessRef_1++;
							}else{
								traFailRef_1++;
								userFailRef_1++;
							}
						}else if(knowRegionNum ==2){//已知两点
							if(realRegion.equals(predictionRegionMy)){
								traSuccessMy_2 ++;
								userSuccessMy_2 ++;
							}else{
								traFailMy_2 ++;
								userFailMy_2 ++;
							}
							if(realRegion.equals(predictionRegionRef)){
								traSuccessRef_2++;
								userSuccessRef_2++;
							}else{
								traFailRef_2++;
								userFailRef_2++;
							}
						}
					}
				}//已知的region数，取值为1,2
				/*System.out.println("traSuccessMy_1:"+traSuccessMy_1+";traFailMy_1:"+traFailMy_1+";traUnable:"+traUnable);
				System.out.println("traSuccessRef_1:"+traSuccessRef_1+";traFailRef_1:"+traFailRef_1+";traUnable:"+traUnable);
				System.out.println("traSuccessMy_2:"+traSuccessMy_2+";traFailMy_2:"+traFailMy_2+";traUnable:"+traUnable);
				System.out.println("traSuccessRef_2:"+traSuccessRef_2+";traFailRef_2:"+traFailRef_2+";traUnable:"+traUnable);*/
			}//tra
			System.out.println("userSuccessMy_1:"+userSuccessMy_1+";userFailMy_1:"+userFailMy_1+";userUnable:"+userUnable);
			System.out.println("userSuccessRef_1:"+userSuccessRef_1+";userFailRef_1:"+userFailRef_1+";userUnable:"+userUnable);
			System.out.println("userSuccessMy_2:"+userSuccessMy_2+";userFailMy_2:"+userFailMy_2+";userUnable:"+userUnable);
			System.out.println("userSuccessRef_2:"+userSuccessRef_2+";userFailRef_2:"+userFailRef_2+";userUnable:"+userUnable);
			System.out.println();
			cellTraList = null;
			testTraNoList = null;
			regionList = null;
			regionTraList = null;
			modelMap = null;
			numArr = null;
		}//user
	}

	//热力图
	public static void testSex(){
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = "";
		ArrayList<String> testUserIdList = new ArrayList<String>();
		sql = "SELECT DISTINCT UserId FROM goodTra";
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
		
		String lngStr = "116.39719";
		String latStr = "39.916538";
		Parameter.cellWidth = 0.2;
		Parameter.gridWidth = 200;//保证grid边长为40km

		GPS gpsCenter = new GPS(lngStr, latStr);
		GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);
		int[][] numArr = new int[Parameter.gridWidth][Parameter.gridWidth];//全部用户
		int totalTra = 0;
		//对于测试集中的每个用户
		for(TestUser testUser:testUserList){
			
			/*if(!testUser.getUserId().equals("001")){
				continue;
			}*/
			
			//计数
			System.out.println("user："+testUser.getUserId()+";traCnt:"+testUser.getEffectiveTraNo().size());
			
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			for(String traNo:testUser.getEffectiveTraNo()){
				/*if(!traNo.equals("00120081023055305")){
					continue;
				}*/
				ArrayList<GPSPoint> traPointsList = new ArrayList<GPSPoint>();
				sql = "SELECT Longitude,Latitude,DateTime FROM filteredpoints where TraNum='"+traNo+"' ORDER BY DateTime";
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
				ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(traPointsList, originGps);
				
				totalTra ++;
				for(GPSCell cell:cellTra){
					int x = cell.getGridX();
					int y = cell.getGridY();
					numArr[x][y] += 1;
				}
//				System.out.println("读入第？轨迹:"+traCount);
			}
			JdbcUtil.close(conn, stmt);
		}
		ArrayList<HeatMapCell> heatMapCellList = new ArrayList<HeatMapCell>();
		for (int i = 0; i < numArr.length; i++) {
			for (int j = 0; j < numArr[0].length; j++) {
				GridCell gridCell = new GridCell(i, j, originGps);
				double centerLng = 0.5 * (Double.parseDouble(gridCell.getLuGps().getLongitude()) + Double.parseDouble(gridCell.getRdGps().getLongitude()));
				double centerLat = 0.5 * (Double.parseDouble(gridCell.getLuGps().getLatitude()) + Double.parseDouble(gridCell.getRdGps().getLatitude()));
//				double arr[] = {centerLng,centerLat};
				if(numArr[i][j]!=0){
					heatMapCellList.add(new HeatMapCell(centerLng,centerLat, numArr[i][j]));
				}
			}
		}
		String jsonString = JSON.toJSONString(heatMapCellList);
		System.out.print(jsonString);
	}
	
   public static void insertGoodPoint(){
	    Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = "";
		ArrayList<String> testUserIdList = new ArrayList<String>();
		sql = "SELECT DISTINCT UserId FROM goodTra";
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
//			JdbcUtil.close(conn, stmt);
		}
		//得到每个测试用户的全部轨迹号。
		ArrayList<TestUser> testUserList = new ArrayList<TestUser>();
		for(String userId:testUserIdList){
			if(Integer.parseInt(userId)<85)
				continue;
			ArrayList<String> userTraNo = new ArrayList<String>();
			sql = "SELECT TraNum FROM goodTra WHERE UserId='"+userId+"'";
			try{
				rs = stmt.executeQuery(sql);
				while(rs.next()){
					userTraNo.add(rs.getString("TraNum"));
				}
			}catch(SQLException e){
				e.printStackTrace();
			}finally{
//				JdbcUtil.close(conn, stmt);
			}
			testUserList.add(new TestUser(userId, userTraNo));
		}
		
		//对于测试集中的每个用户
		for(TestUser testUser:testUserList){
			System.out.println("current:"+testUser.getUserId());
			for(String traNo:testUser.getEffectiveTraNo()){
				
				ArrayList<GPSPoint> traPointsList = new ArrayList<GPSPoint>();
				sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM Location where TraNum='"+traNo+"' ORDER BY DateTime";
				try {
					rs = stmt.executeQuery(sql);
					while(rs.next()){
						GPSPoint point = new GPSPoint(rs.getString("Long_gcj"), rs.getString("Lat_gcj"), rs.getString("DateTime"));
						traPointsList.add(point);
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				traPointsList = TraFilter.getSparsedTra(traPointsList);//过滤
				for(GPSPoint point:traPointsList){
					sql = "insert into filteredpoints(UserId,TraNum,DateTime,Longitude,Latitude) values('"+testUser.getUserId()+"','"+traNo+"','"+point.getDateTime()+"','"+point.getLang()+"','"+point.getLat()+"')";
					try {
						stmt.execute(sql);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}//user
		JdbcUtil.close(conn, stmt);
   }
   
	public static void main(String[] args) {
//		insertGoodPoint();
//		testOne();//预测测试
//		testTwo();//轨迹过滤效果，各个用户轨迹过滤前后轨迹点数
		testThree();//每个用户的region数，model数
//		testFive();//预测测试，使用已知region的方式，使用数据库中保存的过滤后的点
//		testSex();
	}

}
