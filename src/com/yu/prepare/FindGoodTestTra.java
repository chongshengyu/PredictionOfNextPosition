package com.yu.prepare;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.junit.runners.ParentRunner;
import org.omg.CORBA.COMM_FAILURE;

import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.Parameter;
import com.yu.draw.entity.Region;
import com.yu.draw.entity.RegionModel;
import com.yu.draw.entity.RegionTime;
import com.yu.draw.util.GridUtil;
import com.yu.draw.util.TraFilter;
import com.yu.evaluation.entity.ResultScores;
import com.yu.evaluation.entity.TestUser;
import com.yu.evaluation.util.TraUtil;
import com.yu.prepare.util.JdbcUtil;

public class FindGoodTestTra {

	/**
	 * 从手选出的轨迹集中的后1/2中选出预测效果好的轨迹作为实验轨迹，使用未过滤的位置点
	 * 
	 */
	public static void testOne(){
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
			//计数
			System.out.println("user："+testUser.getUserId()+";traCnt:"+testUser.getEffectiveTraNo().size());
			int userSuccessMy = 0;//预测正确
			int userWrongMy = 0;//预测错误
			int userSuccessRef = 0;//预测正确
			int userWrongRef = 0;//预测错误
			int userUnablePredictin = 0;//无法预测
			
			//找出后1/2的轨迹
			ArrayList<String> testTraNoList = new ArrayList<String>();
			for(int i=testUser.getEffectiveTraNo().size()/2;i<testUser.getEffectiveTraNo().size();i++){
				testTraNoList.add(testUser.getEffectiveTraNo().get(i));
			}
			System.out.println("testTraCnt:"+testTraNoList.size());
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
					HashMap<Region, ResultScores> scoreMap = TraUtil.getScoreMap(knownGpsPoint, originGps, time, modelMap, 1.2, 1.0);//设置不同算法，得到不同分数
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
				System.out.println("traMy:"+testTraNo+",successMy:"+traSuccessMy+",wrongMy:"+traWrongMy+",unable:"+traUnablePredictin);
				System.out.println("traRef:"+testTraNo+",successRef:"+traSuccessRef+",wrongRef:"+traWrongRef+",unable:"+traUnablePredictin);
			}
			System.out.println("userMy:"+testUser.getUserId()+",successMy:"+userSuccessMy+",wrongMy:"+userWrongMy+",unable:"+userUnablePredictin);
			System.out.println("userRef:"+testUser.getUserId()+",successRef:"+userSuccessRef+",wrongRef:"+userWrongRef+",unable:"+userUnablePredictin);
		}
	}
	
	/**
	 * 用所有轨迹的前3/4做训练，后1/2做测试，使用数据库中过滤后的位置点，找出哪些测试集轨迹比较好
	 * @throws IOException 
	 */
	public static void testTwo() throws IOException{
		//参照EvaAlgoriOne_1.testFive()
		Parameter.cellWidth = 0.2;
		Parameter.gridWidth = 200;//保证grid边长40km
		Parameter.MAXREGIONWIDTH = 2;
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = "";
		ArrayList<String> testUserIdList = new ArrayList<String>();
		
		//start 用户id列表
		sql = "SELECT DISTINCT UserId FROM goodTra";
		try{
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next()){
				testUserIdList.add(rs.getString("UserId"));
			}
		} catch (SQLException e){
			e.printStackTrace();
		} finally{
			JdbcUtil.close(conn, stmt);
		}
		//end
		
		//start 每个用户的全部轨迹号
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
			} finally {
				JdbcUtil.close(conn, stmt);
			}
			testUserList.add(new TestUser(userId, userTraNo));
		}
		//end
		PrintStream ps = new PrintStream(new File("F:\\OneDrive\\NextPositionPrediction\\实验\\0619新一轮数据\\0624simweek"));
		System.setOut(ps);
		
		System.out.println("Parameter.cellWidth="+Parameter.cellWidth);
		System.out.println("Parameter.gridWidth="+Parameter.gridWidth);
		System.out.println("Parameter.MAXREGIONWIDTH="+Parameter.MAXREGIONWIDTH);
		System.out.println();  
		
		//测试每个用户
		for(TestUser testUser:testUserList){
			System.gc();
			//用户数据计数
			int userSuccessMy_1 = 0;
			int userFailMy_1 = 0;
			int userSuccessRef_1 = 0;
			int userFailRef_1 = 0;
			int userSuccessMy_2 = 0;
			int userFailMy_2 = 0;
			int userSuccessRef_2 = 0;
			int userFailRef_2 = 0;
			int userUnable = 0;
			
			System.out.println("user:"+testUser.getUserId()+";traCnt:"+testUser.getEffectiveTraNo().size());
//			bw.write("user:"+testUser.getUserId()+";traCnt:"+testUser.getEffectiveTraNo().size());
			//GRID中心点
			String lngStr = "116.39719";
			String latStr = "39.916538";
			//cell密度数组
			int[][] numArr = new int[Parameter.gridWidth][Parameter.gridWidth];
			
			GPS gpsCenter = new GPS(lngStr, latStr);
			//得到Grid原点坐标，即Grid左下角坐标
			GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);
			//grid内的训练轨迹集
			ArrayList<ArrayList<GPSCell>> cellTraList = new ArrayList<ArrayList<GPSCell>>();
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			int traCount = 0;
			//遍历每条轨迹，获得cell密度
			for(String traNo:testUser.getEffectiveTraNo()){
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
				ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(traPointsList, originGps);
				cellTraList.add(cellTra);
				for(GPSCell cell:cellTra){
					int x = cell.getGridX();
					int y = cell.getGridY();
					numArr[x][y] += 1;
				}
			}
			JdbcUtil.close(conn, stmt);
			System.out.println("轨迹读入结束，训练集大小"+traCount);
			
			LinkedList<Region> regionList = GridUtil.getRegionListByPointNum(numArr, originGps);
			System.out.println("RegionListSize:"+regionList.size());
			ArrayList<ArrayList<RegionTime>> regionTraList = GridUtil.cellTraList2RegionTraList(cellTraList, regionList);
			HashMap<Region, ArrayList<RegionModel>> modelMap = GridUtil.getModelMap(regionList, regionTraList);
			
			//start 输出regionMap
			/*System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++");
			for(Region region:modelMap.keySet()){
				System.out.println(region);
				ArrayList<RegionModel> arrList = modelMap.get(region);
				for(RegionModel rm:arrList){
					System.out.println(rm);
				}
			}
			System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++");*/
			//end
			System.out.println("训练结束");
			
			//以上训练，以下测试

			ArrayList<String> testTraNoList = new ArrayList<String>();//测试轨迹号
			//后1/2的轨迹作为测试集测试
			for(int i=(int)(testUser.getEffectiveTraNo().size()*0.5);i<testUser.getEffectiveTraNo().size();i++){
				testTraNoList.add(testUser.getEffectiveTraNo().get(i));
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
						HashMap<Region, ResultScores> scoreMap = TraUtil.getScoreMap(knownRegionList, modelMap, 1.2, 1.0);
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
				//begion type1 输出
				/*System.out.printf("%-20s",testTraNo);
				System.out.println();
				System.out.printf("%-20s%-20s%-20s%.4f",
						"traSuccessMy_1:"+traSuccessMy_1+";",
						"traFailMy_1:"+traFailMy_1+";",
						"traUnable:"+traUnable,
						((double)traSuccessMy_1)/(traSuccessMy_1+traFailMy_1+traUnable));
				System.out.println();
				System.out.printf("%-20s%-20s%-20s%.4f",
						"traSuccessRef_1:"+traSuccessRef_1+";",
						"traFailRef_1:"+traFailRef_1+";",
						"traUnable:"+traUnable,
						((double)traSuccessRef_1)/(traSuccessRef_1+traFailRef_1+traUnable));
				System.out.println();
				System.out.printf("%-20s%-20s%-20s%.4f",
						"traSuccessMy_2:"+traSuccessMy_2+";",
						"traFailMy_2:"+traFailMy_2+";",
						"traUnable:"+traUnable,
						((double)traSuccessMy_2)/(traSuccessMy_2+traFailMy_2+traUnable));                                                       
				System.out.println();
				System.out.printf("%-20s%-20s%-20s%.4f",
						"traSuccessRef_2:"+traSuccessRef_2+";",
						"traFailRef_2:"+traFailRef_2+";",
						"traUnable:"+traUnable,
						((double)traSuccessRef_2)/(traSuccessRef_2+traFailRef_2+traUnable));
				System.out.println();*/
				//end
				
				System.out.printf("%-20s","'"+testTraNo);
				double d1 = ((double)traSuccessMy_1)/(traSuccessMy_1+traFailMy_1+traUnable)-
				  ((double)traSuccessRef_1)/(traSuccessRef_1+traFailRef_1+traUnable);
				double d2 = ((double)traSuccessMy_2)/(traSuccessMy_2+traFailMy_2+traUnable)-
						  ((double)traSuccessRef_2)/(traSuccessRef_2+traFailRef_2+traUnable);
				System.out.printf("\t%.4f\t%.4f\t%.4f",d1,d2,d1+d2);
				System.out.println();
			}//for each tra
			System.out.println("-------------------------------");
			System.out.printf("%-20s%-20s%-20s%.4f",
					"userSuccessMy_1:"+userSuccessMy_1+";",
					"userFailMy_1:"+userFailMy_1+";",
					"userUnable:"+userUnable,
					((double)userSuccessMy_1)/(userSuccessMy_1+userFailMy_1+userUnable));
			System.out.println();
			System.out.printf("%-20s%-20s%-20s%.4f",
					"userSuccessRef_1:"+userSuccessRef_1+";",
					"userFailRef_1:"+userFailRef_1+";",
					"userUnable:"+userUnable,
					((double)userSuccessRef_1)/(userSuccessRef_1+userFailRef_1+userUnable));
			System.out.println();
			System.out.printf("%-20s%-20s%-20s%.4f",
					"userSuccessMy_2:"+userSuccessMy_2+";",
					"userFailMy_2:"+userFailMy_2+";",
					"userUnable:"+userUnable,
					((double)userSuccessMy_2)/(userSuccessMy_2+userFailMy_2+userUnable));
			System.out.println();
			System.out.printf("%-20s%-20s%-20s%.4f",
					"userSuccessRef_2:"+userSuccessRef_2+";",
					"userFailRef_2:"+userFailRef_2+";",
					"userUnable:"+userUnable,
					((double)userSuccessRef_2)/(userSuccessRef_2+userFailRef_2+userUnable));
			System.out.println("\n");
			
			cellTraList = null;
			testTraNoList = null;
			regionList = null;
			regionTraList = null;
			modelMap = null;
			numArr = null;
		}//for each user
	}

	/**
	 * @throws SQLException 
	 * 将手选出的测试轨迹插入数据库goodtratotest
	 * @throws  
	 */
	public static void testThree() throws IOException, SQLException{
		System.out.println("start");
		Connection conn = null;
		Statement stmt = null;
		String sql = "";
		//得到数据库中存储的，手工筛选出的用户
		ArrayList<String> testUserIdList = new ArrayList<String>();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("F:\\OneDrive\\NextPositionPrediction\\实验\\0619新一轮数据\\goodtratotest2"))));
		String s;
		while((s = br.readLine()) != null){
			String uid = s.trim().substring(0, 3);
			String tid = s.trim();
			sql = "INSERT INTO goodtratotest VALUES('"+uid+"','"+tid+"')";
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		}
		JdbcUtil.close(conn, stmt);
		System.out.println("end");
	}
	
	public static void main(String[] args) throws IOException, SQLException {
		testThree();
	}

}
