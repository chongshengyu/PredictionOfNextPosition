package com.yu.prepare;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.yu.draw.entity.GPSPoint;
import com.yu.draw.util.TraFilter;
import com.yu.evaluation.entity.TestUser;
import com.yu.prepare.util.JdbcUtil;

public class GoodTra2Db {


	//过滤后的轨迹数据插入到数据库filteredpoints
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
			/*if(Integer.parseInt(userId)<85)
				continue;*/
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
				System.out.println("current:"+traNo);
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
			System.out.println("结束: "+testUser.getUserId());
		}//user
		JdbcUtil.close(conn, stmt);
   }
   
   public static void GoodTra2DB(){
	//将筛选出的合适轨迹插入到goodtra表
	File goodDir = new File("F:\\0TencetCloud\\NextPositionPrediction\\实验\\GoodTraNum");
	File[] goodTraFiles = goodDir.listFiles();
	
	for(File file:goodTraFiles){
		if(file.isDirectory()){
			continue;
		}
		Connection conn = null;
		Statement stmt = null;
		BufferedReader reader = null;
		String line = "";
		String sql = "";
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			while((line = reader.readLine()) != null){
				String userId = line.substring(0, 3);
				String traNum = line;
				sql = "insert into goodtra(UserId, TraNum) values ('"+userId+"','"+traNum+"')";
				stmt.execute(sql.toString());
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			JdbcUtil.close(conn, stmt);
		}
		System.out.println("结束一个");
	}
   }
	
   public static void main(String[] args) {
	   insertGoodPoint();
	}

}
