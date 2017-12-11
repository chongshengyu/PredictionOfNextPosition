package com.yu.evaluation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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
import com.yu.draw.util.TraFilter;
import com.yu.evaluation.entity.ResultScores;
import com.yu.evaluation.entity.TestUser;
import com.yu.evaluation.util.TraUtil;

public class EvaAlgori_Roma {

	public static void main(String[] args) {
		//
		testFive(2,1.2,1.0);
	}
	
	/**
	 * 
	 * @param lamda regionmax大小，可取值1/2/3/4/5
	 * @param tH 参数tao_h
	 * @param tL 参数tao_l 两组取值可为 1.5/1.0, 1.2/1.0, 1.0/1.0
	 * @throws FileNotFoundException
	 */
	public static void testFive(int lamda, double tH, double tL){
		Parameter.cellWidth = 0.2;//0.2
		Parameter.gridWidth = 400;//200,保证grid边长为40km
		Parameter.MAXREGIONWIDTH = lamda;					     //lamda_max_w可取值 1-5(最终取值2）
		String formula1 = "result3 = "+tH+"/"+tL;//核心算法方法也要改    //result3(tao)可取值 1.5/0.5, 1.2/1.0, 1.0/1.0（最终取值1.2/1.0)
		String formula2 = "result = result2 * result3";
		System.out.println("start...");
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String sql = "";
		ArrayList<String> testUserIdList = new ArrayList<String>();
		
		sql = "SELECT DISTINCT taxiId FROM roma_goodtratotest";
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next()){
				String uid = rs.getString("taxiId");
				testUserIdList.add(uid);
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
			sql = "SELECT TraNum FROM roma_trajectory WHERE taxiId='"+userId+"'";
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
		System.out.println(formula1);
		System.out.println(formula2);
		System.out.println();
		long startMillis = System.currentTimeMillis();
		
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
			String lngStr = "12.4964";
			String latStr = "41.9028";
			
			int[][] numArr = new int[Parameter.gridWidth][Parameter.gridWidth];
			
			GPS gpsCenter = new GPS(lngStr, latStr);
			//得到Grid原点坐标，即Grid左下角坐标
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
				sql = "SELECT longitude,latitude,createTime FROM roma_filteredpoints where TraNum='"+traNo+"' ORDER BY createTime";
				try {
					rs = stmt.executeQuery(sql);
					while(rs.next()){
						GPSPoint point = new GPSPoint(rs.getString("longitude"), rs.getString("latitude"), rs.getString("createTime"));
						traPointsList.add(point);
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//数据库中已是过滤结束的
				//traPointsList = TraFilter.getSparsedTra(traPointsList);//不用过滤了过滤
				//roma未过滤
				
				ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(traPointsList, originGps);
//				System.out.println("cellTra.size:"+cellTra.size());//---------------------------------
				cellTraList.add(cellTra);
				for(GPSCell cell:cellTra){
					int x = cell.getGridX();
					int y = cell.getGridY();
					numArr[x][y] += 1;
				}
//					System.out.println("读入第？轨迹:"+traCount);
			}
			JdbcUtil.close(conn, stmt);
//				System.out.println("轨迹读入结束，训练集大小："+traCount);
			
			LinkedList<Region> regionList = GridUtil.getRegionListByPointNum(numArr, originGps);
//				System.out.println("regionListSize:"+regionList.size());
			ArrayList<ArrayList<RegionTime>> regionTraList = GridUtil.cellTraList2RegionTraList(cellTraList, regionList);
			HashMap<Region, ArrayList<RegionModel>> modelMap = GridUtil.getModelMap(regionList, regionTraList);
			System.out.println("train ends.");
			
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
			sql = "SELECT TraNum FROM roma_goodtratotest WHERE taxiId='"+testUser.getUserId()+"'";
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
				sql = "SELECT longitude,latitude,createTime FROM roma_filteredpoints where TraNum='"+testTraNo+"' ORDER BY createTime";
				try{
					rs = stmt.executeQuery(sql);
					while(rs.next()){
						GPSPoint point = new GPSPoint(rs.getString("longitude"), rs.getString("latitude"), rs.getString("createTime"));
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
					System.out.println("log:total region number of current trace is less than 4.");
					System.out.println(testTraNo);
					System.out.println(testRegionTra.size());
					continue;
				}
				//只需要测试已知一个region，和已知两个region的情况。
				ArrayList<RegionTime> knownRegionList = new ArrayList<RegionTime>();
				for(int knowRegionNum=1;knowRegionNum<3;knowRegionNum++){//已知的region数，取值为1,2
//							System.out.println("knowRegionNun:"+knowRegionNum);
					for(int regionIndex=0;regionIndex<testRegionTra.size()-knowRegionNum;regionIndex++){//遍历testRegionTra中的region进行测试
						knownRegionList.clear();
						for(int i=0;i<knowRegionNum;i++){
							knownRegionList.add(testRegionTra.get(regionIndex + i));
						}
						HashMap<Region, ResultScores> scoreMap = TraUtil.getScoreMap(knownRegionList, modelMap, tH, tL);/////////////核心算法
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
			/*System.out.println("userSuccessMy_1:"+userSuccessMy_1+";userFailMy_1:"+userFailMy_1+";userUnable:"+userUnable);
			System.out.println("userSuccessRef_1:"+userSuccessRef_1+";userFailRef_1:"+userFailRef_1+";userUnable:"+userUnable);
			System.out.println("userSuccessMy_2:"+userSuccessMy_2+";userFailMy_2:"+userFailMy_2+";userUnable:"+userUnable);
			System.out.println("userSuccessRef_2:"+userSuccessRef_2+";userFailRef_2:"+userFailRef_2+";userUnable:"+userUnable);
			System.out.println();*/
			
			System.out.printf("%-20s%-20s%-20s%.4f",
					"userSuccessMy_1:"+userSuccessMy_1+";",
					"userFailMy_1:"+userFailMy_1+";",
					"userUnable:"+userUnable,
					((double)userSuccessMy_1)/(userSuccessMy_1+userFailMy_1+userUnable));
			System.out.println();
			System.out.printf("%-20s%-20s%-20s%.4f",
					"userSuccessRef_1:"+userSuccessRef_1+";",
					"userFailMy_1:"+userFailRef_1+";",
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
			System.out.println("\n++++++++++++++++++++++++++++++++++++++");
			cellTraList = null;
			testTraNoList = null;
			regionList = null;
			regionTraList = null;
			modelMap = null;
			numArr = null;
		}//user
		//总计时
		long endMillis = System.currentTimeMillis();
		System.out.println("total time:"+(endMillis-startMillis)/1000+"s");
	}


}
