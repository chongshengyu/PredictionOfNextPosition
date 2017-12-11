package com.yu.prepare;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.yu.draw.entity.GPSPoint;
import com.yu.draw.util.StringUtil;
import com.yu.draw.util.TraFilter;
import com.yu.evaluation.entity.TestUser;
import com.yu.prepare.util.JdbcUtil;

public class File2DatabaseRoma {

	public static void main(String[] args) throws IOException {
		//从文件中将原始罗马数据集导入到数据库
//		FileImport();//已完全导入
		//将位置点分段成轨迹
//		TraSplit();//已完全分段
		//每个taxi的后一半轨迹插入roma_goodtratotest
//		InsertGoodTraToTest();//已完全插入
		//过滤后的位置点插入
//		insertGoodPoint();//只过滤了 taxiId < 80的部分，插入到了roma_filteredpoints
	}
	
	static class GPSRecord{
		String taxiNo = "";
		String lng = "";
		String lat = "";
		String createTime = "";
	}
	
	static class StartEndTime{
		String startTime = "";
		String endTime = "";
		
		StartEndTime(String s, String e){
			this.startTime = s;
			this.endTime = e;
		}
		
		@Override
		public String toString() {
			//
			return "startT:"+startTime+";endT:"+endTime;
		}
	}
	
	/*********************************************导入数据*************************************************/
	
	/**
	 * 将一行数据转换为对象
	 * @param line
	 * @return
	 */
	private static GPSRecord DataLineToObject(String line){
		//156;2014-02-01 00:00:00.739166+01;POINT(41.8836718276551 12.4877775603346)
		String taxiNo = line.split(";")[0];
		String lat = line.split("\\(")[1].split("\\s+")[0];
		String lng = line.split("\\(")[1].split("\\s+")[1].split("\\)")[0];
//		String createTime = line.split(";")[1].split("\\.")[0];
		String createTime = line.split(";")[1].split("\\+")[0];
		GPSRecord record = new File2DatabaseRoma.GPSRecord();
		record.taxiNo = taxiNo;
		record.lng = lng;
		record.lat = lat;
		record.createTime = createTime;
		return record;
	}
	
	private static void insertGPSRecord(List<GPSRecord> list, Statement stmt){
		String sql = "";
		String s = "";
		for(GPSRecord record:list){
			s = "INSERT INTO roma_location(taxiId,createTime,longitude,latitude) VALUES('"
					+ record.taxiNo
					+ "','"
					+ record.createTime
					+ "','"
					+ record.lng
					+ "','"
					+ record.lat
					+ "');";
			try {
				stmt.addBatch(s);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
//			System.out.println(sql);
			stmt.executeBatch();
			stmt.clearBatch();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
	}
	
	/**
	 * 导入方法，main调用
	 * @throws IOException
	 */
	private static void FileImport() throws IOException{
		String fileDir = "F:\\Paper\\GPS\\RomeData\\taxi_february.txt";
		//F:\Paper\GPS\RomeData\taxi_february.txt
		BufferedReader reader = new BufferedReader(new FileReader(fileDir));
		String dataLine = "";
		ArrayList<GPSRecord> list = new ArrayList<>();
		
		Connection conn = null;
		Statement stmt = null;
		conn = JdbcUtil.getConnection();
		try {
			stmt = conn.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("连接失败");
			e.printStackTrace();
		}
		
		int total = 0;
		int pageSize = 60;
		Date now;
		SimpleDateFormat dateFormat;
		GPSRecord record;
		while((dataLine = reader.readLine()) != null){
			if(list.size() == pageSize){//每次插入pageSize条
				if(total>12145980-pageSize*2){//因为中间断了一次。
					insertGPSRecord(list, stmt);
					System.out.println("insert............");
				}
				list = new ArrayList<GPSRecord>();
				total += pageSize;
				System.out.println("-------------------------------");
				System.out.println("total:"+total);
				now = new Date(); 
				dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");//可以方便地修改日期格式
				System.out.println(dateFormat.format(now));
			}else{
				record = DataLineToObject(dataLine);
				list.add(record);
			}
		}
		if(list.size()>0){
			insertGPSRecord(list, stmt);
		}
		
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/***********************************************轨迹分段***********************************************/
	/**
	 * 判断两个时间间隔，是否超出分段阈值，如1个小时，超过一个小时则返回false
	 * @param firstTime
	 * @param lastTime
	 * @return 间隔小于1小时，返回true
	 */
	private static boolean TwoTimesLessThanSet(String firstTime, String lastTime){
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSS");
		Date first = null;
		Date last = null;
		try {
			first = df.parse(firstTime);
			last = df.parse(lastTime);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ((last.getTime()-first.getTime())<1*60*60*1000);
	}
	
	/**
	 * oldList中存的是轨迹交接点的时间，并不是某条轨迹的起止时间，因此还需转换
	 * @param startTime 所有位置点的最早时间
	 * @param endTime 所有位置点的最晚时间
	 * @param oldList 轨迹交接点
	 * @return
	 */
	private static LinkedList<StartEndTime> getTraStartEndTimeList(String startTime, String endTime, LinkedList<StartEndTime> oldList){
		LinkedList<StartEndTime> list = new LinkedList<StartEndTime>();
		String firstTime = startTime;
		String lastTime = "";
		for(StartEndTime se:oldList){
			lastTime = se.startTime;
			list.add(new StartEndTime(firstTime, lastTime));
			firstTime = se.endTime;
		}
		list.add(new StartEndTime(firstTime, endTime));
		return list;
	}
	
	/**
	 * 根据起止时间更新 用户位置表轨迹号，轨迹表。
	 * @param taxiId
	 * @param startTime
	 * @param endTime
	 * @param traIndex
	 */
	private static void updateTraNumOfLocationAndTra(String taxiId, String startTime, String endTime, int traIndex, Statement stmt){
		//位置表，轨迹表
		String traNum = StringUtil.padLeft(taxiId, 3, '0') + StringUtil.padLeft(String.valueOf(traIndex), 3, '0');
		String sql1 = "update roma_location set traNum='"+traNum+"' where taxiId='"+taxiId+"' and createTime>='"+startTime+"' and createTime<='"+endTime+"';";
		
		String sql2 = "insert into roma_trajectory(taxiId, traNum, startTime, endTime, pointCnt) values ('"+taxiId+"','"+traNum+"','"+startTime+"','"+endTime+"',(select count(*) from roma_location where taxiId='"+taxiId+"' and createTime >='"+startTime+"' and createTime<='"+endTime+"'));";
		
		try {
			stmt.addBatch(sql1);
			stmt.addBatch(sql2);
			stmt.executeBatch();
			stmt.clearBatch();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
	}
	
	private static void TraSplit(){
		Connection conn = null;
		Statement stmt = null;
		conn = JdbcUtil.getConnection();
		try {
			stmt = conn.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("连接失败");
			e.printStackTrace();
		}
		
		String sql = "";
		
		//所有taxiId
		ArrayList<String> taxiIdList = new ArrayList<>();
		sql = "select distinct taxiId from roma_location";
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			while(rs.next()){
				taxiIdList.add(rs.getString("taxiId"));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		LinkedList<StartEndTime> startEndTimeList;
		String firstTime = "";
		String lastTime = "";
		for(String taxiId:taxiIdList){
			if(!(Integer.parseInt(taxiId)>17 && Integer.parseInt(taxiId)<100)){//18-99
				continue;
			}
			startEndTimeList = new LinkedList<>();//该用户的开始结束时间对
			int pageSize = 100;//每次查询数量
			int pointCnt = 0;
			sql = "select count(*) as cnt from roma_location where taxiId='"+taxiId+"'";
			try {
				rs = stmt.executeQuery(sql);
				if(rs.next())
					pointCnt = rs.getInt("cnt");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int pageCnt = pointCnt/pageSize + 1;//分页数
			System.out.println("pointCnt:"+pointCnt);
			System.out.println("pageCnt:"+pageCnt);
			for(int pageIndex = 0;pageIndex < pageCnt;pageIndex++){
				sql = "select createTime from roma_location where taxiId='"+taxiId+"' order by createTime limit "+pageIndex*pageSize+","+pageSize+";";
//				System.out.println(sql);
				try {
					rs = stmt.executeQuery(sql);
					if(!rs.next()){
						continue;//当前结果为空，可以直接break。因为后面也还是空
					}
					firstTime = rs.getString("createTime");
					rs.last();
					lastTime  =rs.getString("createTime");
//					System.out.println("firstTime:"+firstTime);
//					System.out.println("lastTime:"+lastTime);
					if(TwoTimesLessThanSet(firstTime, lastTime)){//间隔小于1小时
						continue;//继续判断下一段
					}else{//间隔大于1小时
						rs.first();
						firstTime = rs.getString("createTime");
						while(rs.next()){
							lastTime = rs.getString("createTime");
							if(TwoTimesLessThanSet(firstTime, lastTime)){//小于1小时
								firstTime = lastTime;
							}else{//大于1小时
								startEndTimeList.add(new StartEndTime(firstTime, lastTime));
								firstTime = lastTime;
							}
						}
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//目前startEndTimeList中存的是 轨迹交接点，并不是各条轨迹的起止时间，因此还需转换
			sql = "select createTime from roma_location where taxiId='"+taxiId+"' order by createTime limit 1;";
			try {
				rs = stmt.executeQuery(sql);
				if(rs.next())
					firstTime = rs.getString("createTime");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sql = "select createTime from roma_location where taxiId='"+taxiId+"' order by createTime desc limit 1;";
			try {
				rs = stmt.executeQuery(sql);
				if(rs.next())
					lastTime = rs.getString("createTime");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			startEndTimeList = getTraStartEndTimeList(firstTime,lastTime,startEndTimeList);
			//遍历该taxi的startEndTimeList，更新位置表，插入轨迹表
			for(int i=0;i<startEndTimeList.size();i++){
				updateTraNumOfLocationAndTra(taxiId, startEndTimeList.get(i).startTime, startEndTimeList.get(i).endTime, i, stmt);
			}
			
			System.out.println("taxiId:"+taxiId+"over.");
		}
	}
	
	private static void InsertGoodTraToTest(){
		String sql = "select distinct taxiId from roma_trajectory order by taxiId";
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		String taxiIdStr = "";
		conn = JdbcUtil.getConnection();
		try {
			stmt = conn.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("连接失败");
			e.printStackTrace();
		}
		
		ArrayList<String> taxiIdList = new ArrayList<String>();
		
		try {
			rs = stmt.executeQuery(sql);
			while(rs.next()){
				taxiIdStr = rs.getString("taxiId");
				if(!(Integer.parseInt(taxiIdStr)>17 && Integer.parseInt(taxiIdStr)<100)){//18-99
					continue;
				}
				taxiIdList.add(taxiIdStr);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<String> traNumList = null;
		String traNum = "";
		for(String taxiId:taxiIdList){//每个taxi
			traNumList = new ArrayList<String>();
			sql = "select distinct traNum from roma_trajectory where taxiId='"+taxiId+"' order by traNum";
			try {
				rs = stmt.executeQuery(sql);
				while(rs.next()){
					traNumList.add(rs.getString("traNum"));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(int i=traNumList.size()/2;i<traNumList.size();i++){
				traNum = traNumList.get(i);
				sql = "insert into roma_goodtratotest(taxiId, traNum) values('"+taxiId+"','"+traNum+"');";
				try {
					stmt.addBatch(sql);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				stmt.executeBatch();
				stmt.clearBatch();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("taxiId:"+taxiId+";over");
		}
	}
	
	//过滤后的轨迹数据插入到数据库roma_filteredpoints
	   public static void insertGoodPoint(){
		    Connection conn = null;
			Statement stmt = null;
			ResultSet rs = null;
			String sql = "";
			String taxiIdStr = "";
			ArrayList<String> testUserIdList = new ArrayList<String>();
			sql = "SELECT DISTINCT taxiId FROM roma_trajectory";
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while(rs.next()){
					taxiIdStr = rs.getString("taxiId");
					//2017-12-11 80之前的过滤插入表完成
					if(!(Integer.parseInt(taxiIdStr)>79 && Integer.parseInt(taxiIdStr)<100)){//80-99
						continue;
					}
					testUserIdList.add(taxiIdStr);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally{
//				JdbcUtil.close(conn, stmt);
			}
			//得到每个测试用户的全部轨迹号。
			ArrayList<TestUser> testUserList = new ArrayList<TestUser>();
			for(String userId:testUserIdList){
				/*if(Integer.parseInt(userId)<85)
					continue;*/
				ArrayList<String> userTraNo = new ArrayList<String>();
				sql = "SELECT TraNum FROM roma_trajectory WHERE taxiId='"+userId+"'";
				try{
					rs = stmt.executeQuery(sql);
					while(rs.next()){
						userTraNo.add(rs.getString("TraNum"));
					}
				}catch(SQLException e){
					e.printStackTrace();
				}finally{
//					JdbcUtil.close(conn, stmt);
				}
				testUserList.add(new TestUser(userId, userTraNo));
			}
			
			//对于测试集中的每个用户
			for(TestUser testUser:testUserList){
				System.out.println("current:"+testUser.getUserId());
				for(String traNo:testUser.getEffectiveTraNo()){
					System.out.println("current:"+traNo);
					ArrayList<GPSPoint> traPointsList = new ArrayList<GPSPoint>();
					sql = "SELECT longitude,latitude,createTime FROM roma_location where TraNum='"+traNo+"' ORDER BY createTime";
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
					traPointsList = TraFilter.getSparsedTra(traPointsList);//过滤
					for(GPSPoint point:traPointsList){
						sql = "insert into roma_filteredpoints(taxiId,TraNum,createTime,longitude,latitude) values('"+testUser.getUserId()+"','"+traNo+"','"+point.getDateTime()+"','"+point.getLang()+"','"+point.getLat()+"')";
						try {
							stmt.addBatch(sql);
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					try {
						stmt.executeBatch();
						stmt.clearBatch();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println("结束: "+testUser.getUserId());
			}//user
			JdbcUtil.close(conn, stmt);
	   }
}
