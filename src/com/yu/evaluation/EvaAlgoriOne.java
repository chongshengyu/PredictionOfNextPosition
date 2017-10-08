package com.yu.evaluation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.Parameter;
import com.yu.draw.entity.Region;
import com.yu.draw.entity.RegionModel;
import com.yu.draw.entity.RegionTime;
import com.yu.draw.util.GridUtil;
import com.yu.draw.util.JdbcUtil;
import com.yu.draw.util.StringUtil;
import com.yu.draw.util.TraFilter;
import com.yu.evaluation.entity.ResultScores;
import com.yu.evaluation.entity.TestUser;
import com.yu.evaluation.util.TraUtil;

public class EvaAlgoriOne {

	/**
	 * 已知一个位置，进行预测的实验过程
	 * 确定一个满足条件（有效轨迹（点数大于阈值）条数大于阈值）的待测试用户id集
	 * 对于每一个用户，得到其满足条件（点数大于阈值）的轨迹号集，使用前9成的轨迹做样本集，1成做测试集
	 * 对于测试集中的每一条轨迹，进行稀疏化，每隔30秒取一个样本点。对稀疏化后的轨迹中的每个点，依次进行预测。预测时，使用9成轨迹构成的样本空间，进行建模
	 * 对于每个点的预测结果，可能出现的情况有：
	 * 	1，根据模型得不到该点对应的region，可能原因是用户之前没有到过这里。则为：无历史数据可用
	 * 	2，可以得到该点对应的region，然后预测下一个region，怎么进行对错判断：
	 * 		从当前点开始，依次计算后面点所在的region，遇到的下一个region与预测的相同则正确，不同则错误。
	 */
	public static void testOne(){
		//计数
		int totalSuccess = 0;
		int totalFailed = 0;
		long beginTime = System.currentTimeMillis();
		
		//测试用户id区间
		int userIdBgn = 13;//含
		int userIdEnd = 14;//不含
		//可用于实验的有效用户及其轨迹，有效用户：有效轨迹条数达到阈值。有效轨迹：点数达到阈值
		ArrayList<TestUser> testUserList = new ArrayList<TestUser>();
		for(int i=userIdBgn;i<userIdEnd;i++){
			String userId = StringUtil.padLeft(String.valueOf(i), 3, '0');
			ArrayList<String> effectiveTraNo = new ArrayList<String>();
			Connection conn = null;
			Statement stmt = null;
			String sql = "SELECT TraNum,PointNum from trajectory WHERE UserId='"+userId+"' ORDER BY StartTime";
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				while(rs.next()){
					if(Integer.parseInt(rs.getString("PointNum")) > Parameter.TRA_POINTS_MIN){//有效点数
						effectiveTraNo.add(rs.getString("TraNum"));
					}
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(effectiveTraNo.size() > Parameter.TRA_COUNT_MIN){//有效轨迹数
				testUserList.add(new TestUser(userId,effectiveTraNo));
			}
			JdbcUtil.close(conn, stmt);
		}
		//对于测试集中的每个用户
		for(TestUser testUser:testUserList){
			//计数
			System.out.println("user："+testUser.getUserId());
			long userSuccess = 0;
			long userFailed = 0;
			long userError = 0;
			long userUnablePredictin = 0;
			
			int countOfTrainTra = (int)(testUser.getEffectiveTraNo().size() * Parameter.PROP_OF_SAMPLE_SET);//该用户的样本集大小
			//得到训练集的轨迹号
			ArrayList<String> trainTraNoList = new ArrayList<String>();
			for(int indexOfTrainTra=0;indexOfTrainTra<countOfTrainTra;indexOfTrainTra++){
				trainTraNoList.add(testUser.getEffectiveTraNo().get(indexOfTrainTra));
			}
			//对于每个测试集中的轨迹进行测试
			for(int indexOfTestTra = countOfTrainTra;indexOfTestTra<testUser.getEffectiveTraNo().size();indexOfTestTra ++ ){
				String traNo = testUser.getEffectiveTraNo().get(indexOfTestTra);
				String userId = testUser.getUserId();
				//计数
				System.out.println("traNo:"+traNo);
				long traSuccess = 0;
				long traFailed = 0;
				long traError = 0;
				long traUnablePredictin = 0;
				
				Connection conn = null;
				Statement stmt = null;
				String sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM location WHERE UserId='"+userId+"' AND TraNum='"+traNo+"'";
				ArrayList<GPSPoint> gpsPoints = new ArrayList<GPSPoint>();
				try {
					conn = JdbcUtil.getConnection();
					stmt = conn.createStatement();
					ResultSet rs = stmt.executeQuery(sql);
					while(rs.next()){
						gpsPoints.add(new GPSPoint(rs.getString("Long_gcj"), rs.getString("Lat_gcj"), rs.getString("DateTime")));
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				JdbcUtil.close(conn, stmt);
				
				//对测试轨迹进行预处理。。。。。。。。。。。。
				gpsPoints = TraFilter.getSparsedTra(gpsPoints);//得到系数后的轨迹
				for(GPSPoint gpsPoint:gpsPoints){
					//对于每个gps点进行测试
					//得到Grid左上角和右下角坐标
					GPS gpsCenter = new GPS(gpsPoint.getLang(), gpsPoint.getLat());
					String lu_lng = GridUtil.getGridLUGpsByCenter(gpsCenter).getLongitude();
					String lu_lat = GridUtil.getGridLUGpsByCenter(gpsCenter).getLatitude();
					String rd_lng = GridUtil.getGridRDGpsByCenter(gpsCenter).getLongitude();
					String rd_lat = GridUtil.getGridRDGpsByCenter(gpsCenter).getLatitude();
					//得到Grid原点坐标
					GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);//边长可以改小一点
					
					//grid内的训练轨迹集，也做了同样的稀疏
					ArrayList<ArrayList<GPSPoint>> traList = TraUtil.getTraInGrid(userId, lu_lng, lu_lat, rd_lng, rd_lat,trainTraNoList);
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
					HashMap<Region, ResultScores> scoreMap = TraUtil.getScoreMap(knownGpsPoint, originGps, time, modelMap, 1.2, 1.0);
					if(scoreMap == null){
//						System.out.println("无法预测");
						traUnablePredictin ++;
						continue;
					}
					//找到分值最大的region
					Region predictionRegion = null;
					double scoreMax = 0.0;
					for(Region region:scoreMap.keySet()){
						if((double)scoreMap.get(region).getMyScore() > scoreMax){
							predictionRegion = region;
							scoreMax = (double)scoreMap.get(region).getMyScore();
						}
					}
					//判断预测是否正确
					//截取得到从当前gpsPoint到当前轨迹结束的轨迹，转换成regions，判断第二个region是否与预测的region相同
					ArrayList<GPSPoint> restGpsPoints = new ArrayList<GPSPoint>();
					for(int i=gpsPoints.indexOf(gpsPoint);i<gpsPoints.size();i++){
						//判断接下来的点还在grid内部，否则结束
						String lng = gpsPoints.get(i).getLang();
						String lat = gpsPoints.get(i).getLat();
						if(Double.parseDouble(lng) > Double.parseDouble(lu_lng) && Double.parseDouble(lat) < Double.parseDouble(lu_lat) && Double.parseDouble(lng) < Double.parseDouble(rd_lng) && Double.parseDouble(lat) > Double.parseDouble(rd_lat)){
							restGpsPoints.add(gpsPoints.get(i));
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
//						System.out.println("error");
						userError ++;
						traError ++;
						continue;
					}
					if(regionTra.size() < 2){
//						System.out.println("error1");
						userError ++;
						traError ++;
						continue;
					}
					if(regionTra.get(1).getRegion().equals(predictionRegion)){//预测成功
						totalSuccess ++;
						userSuccess ++;
						traSuccess ++;
//						System.out.println("成功++++++++++++");
					}else{
						totalFailed ++;
						userFailed ++;
						traFailed ++;
//						System.out.println("预测错误---------");
//						System.out.println(predictionRegion.getLabel());
//						System.out.println(regionTra.get(1).getRegion().getLabel());
					}
				}
				System.out.println("tra:"+traNo+",success:"+traSuccess+",failed:"+traFailed+",error:"+traError+",unable:"+traUnablePredictin);
			}
			long currentTime = System.currentTimeMillis();
			System.out.println("当前总计用时："+new Date(currentTime - beginTime));
			System.out.println("user:"+testUser.getUserId()+",success:"+userSuccess+",failed:"+userFailed+",unable:"+userUnablePredictin);
		}
		long endTime = System.currentTimeMillis();
	}
	
	public static void main(String[] args) {
//		testOne();
	}

}
