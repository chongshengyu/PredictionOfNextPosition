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

import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.Parameter;
import com.yu.draw.entity.Region;
import com.yu.draw.entity.RegionModel;
import com.yu.draw.entity.RegionTime;
import com.yu.draw.util.GridUtil;
import com.yu.draw.util.TraFilter;
import com.yu.evaluation.entity.TestUser;
import com.yu.evaluation.util.TraUtil;
import com.yu.prepare.util.JdbcUtil;

public class EvaAlgoriOne_1 {
	/**
	 * -----------------------------------------------------该实验，训练集和测试集全部使用的手工选择出的用户及其部分轨迹
	 * 
	 * --测试集数量1/10,从后1/2的轨迹中随机选择1/10的轨迹作为测试集，使用测试轨迹之前的所有轨迹作为训练集。训练集大小不固定
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
			int userSuccess = 0;//预测正确
			int userWrong = 0;//预测错误
			int userUnablePredictin = 0;//无法预测
			
			//随机选取测试集
			ArrayList<String> testTraNoList = new ArrayList<String>();//随机选出的测试轨迹号
			for(int i=0;i<testUser.getEffectiveTraNo().size() * (1 - Parameter.PROP_OF_SAMPLE_SET);i++){//随机种子26
				int testTraIndex = new Random(26).nextInt(testUser.getEffectiveTraNo().size() / 2) + (testUser.getEffectiveTraNo().size() / 2) - 1;//选中的测试集索引
				testTraNoList.add(testUser.getEffectiveTraNo().get(testTraIndex));
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
				//以01320081209230723为第一条测试·································································
//				testTraNo = "01320081209230723";
				System.out.println("current tra:"+testTraNo);
				int traSuccess = 0;//预测正确
				int traWrong = 0;//预测错误
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
					//对于每个gps点进行测试
					//得到Grid左上角和右下角坐标
					GPS gpsCenter = new GPS(gpsPoint.getLang(), gpsPoint.getLat());
					String lu_lng = GridUtil.getGridLUGpsByCenter(gpsCenter).getLongitude();
					String lu_lat = GridUtil.getGridLUGpsByCenter(gpsCenter).getLatitude();
					String rd_lng = GridUtil.getGridRDGpsByCenter(gpsCenter).getLongitude();
					String rd_lat = GridUtil.getGridRDGpsByCenter(gpsCenter).getLatitude();
					//得到Grid原点坐标
					GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);//边长可以改小一点
					//grid内的训练轨迹集，内部也做了同样的稀疏
					ArrayList<ArrayList<GPSPoint>> traList = TraUtil.getTraInGrid(testUser.getUserId(), lu_lng, lu_lat, rd_lng, rd_lat,trainTraNoList);
					if(traList.size() == 0){
						System.out.println("该点附近没有历史轨迹");
						continue;
					}
					//将轨迹转换成cell序列
					ArrayList<ArrayList<GPSCell>> cellTraList = new ArrayList<ArrayList<GPSCell>>();
					for(ArrayList<GPSPoint> tra:traList){
						ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(tra, originGps);
						cellTraList.add(cellTra);
					}
					//得到int[][] numArr
					int[][] numArr = new int[Parameter.gridWidth][Parameter.gridWidth];
					for(ArrayList<GPSCell> cellTra:cellTraList){
						for(GPSCell cell:cellTra){
							int x = cell.getGridX();
							int y = cell.getGridY();
							numArr[x][y] += 1;
						}
					}
					//得到regions
					LinkedList<Region> regionList = GridUtil.getRegionListByPointNum(numArr, originGps);
					//每条轨迹对应的regionTra
					ArrayList<ArrayList<RegionTime>> regionTraList = GridUtil.cellTraList2RegionTraList(cellTraList, regionList);
					//regionTraList => modelMap
					HashMap<Region, ArrayList<RegionModel>> modelMap = GridUtil.getModelMap(regionList, regionTraList);
					String time = gpsPoint.getDateTime();
					ArrayList<GPS> knownGpsPoint = new ArrayList<GPS>();
					knownGpsPoint.add(new GPS(gpsPoint.getLang(),gpsPoint.getLat()));

					//根据历史数据得到的regions里不能包含当前待预测的点怎么办????????????????????????????????
					HashMap<Region, Double> scoreMap = TraUtil.getScoreMap(knownGpsPoint, originGps, time, modelMap);
					if(scoreMap == null){
						traUnablePredictin ++;
						continue;
					}
					//找到分值最大的region????这个地方，有时候不连续？？？？？
					Region predictionRegion = null;
					double scoreMax = 0.0;
					for(Region region:scoreMap.keySet()){
						if((double)scoreMap.get(region) > scoreMax){
							predictionRegion = region;
							scoreMax = (double)scoreMap.get(region);
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
					if(regionTra.get(1).getRegion().equals(predictionRegion)){//正确
						userSuccess ++;
						traSuccess ++;
					}else{//错误
						userWrong ++;
						traWrong ++;
					}
				}
				System.out.println("tra:"+testTraNo+",success:"+traSuccess+",wrong:"+traWrong+",unable:"+traUnablePredictin);
			}
			System.out.println("user:"+testUser.getUserId()+",success:"+userSuccess+",wrong:"+userWrong+",unable:"+userUnablePredictin);
		}
	}
	
	
	public static void main(String[] args) {
		//
		testOne();
	}

}
