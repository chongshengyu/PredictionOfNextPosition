package com.yu.prepare.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.yu.prepare.entity.GPSRecord;
import com.yu.prepare.entity.TraRecord;
import com.yu.prepare.util.JdbcUtil;

public class DataAccess {
	private Connection conn;
	private Statement stmt;
	private String errorLog;
	
	public String getErrorLog() {
		return errorLog;
	}
	public void setErrorLog(String errorLog) {
		this.errorLog = errorLog;
	}
	public DataAccess() {
		conn = JdbcUtil.getConnection();
		try {
			stmt = conn.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("数据库连接失败");
			e.printStackTrace();
		}
	}
	static{
		/*conn = JdbcUtil.getConnection();
		try {
			stmt = conn.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("数据库连接失败");
			e.printStackTrace();
		}*/
	}
	public void InsertLocation(GPSRecord gpsRecord) {
		String userId = gpsRecord.getUserId();
		String dateTime = gpsRecord.getDateTime();
		String long_wgs = gpsRecord.getLongitude_wgs();
		String lat_wgs = gpsRecord.getLatitude_wgs();
		String long_gcj = gpsRecord.getLongtitude_gcj();
		String lat_gcj = gpsRecord.getLatitude_gcj();
		String traNum = gpsRecord.getTraNum();
		String sql = "";
		try {
			sql = "INSERT INTO Location(UserId,DateTime,Long_wgs,Lat_wgs,Long_gcj,Lat_gcj,TraNum) VALUES('"
					+ userId
					+ "','"
					+ dateTime
					+ "','"
					+ long_wgs
					+ "','"
					+ lat_wgs
					+ "','"
					+ long_gcj
					+ "','"
					+ lat_gcj
					+ "','"
					+ traNum + "')";
			stmt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//			System.out.println("error:" + sql);
//			errorLog += "error:" + sql + "\r\n";
//			throw new RuntimeException(e);
		}
	}
	public void InsertTra(TraRecord traRecord){
		String userId = traRecord.getUserId();
		String traNum = traRecord.getTraNum();
		String startTime = traRecord.getStartTime();
		String endTime = traRecord.getEndTime();
		String duration = traRecord.getDuration();
		int pointNum = traRecord.getPointNum();
		String sql = "";
		try {
			sql = "INSERT INTO Trajectory(UserId,TraNum,StartTime,EndTime,Duration,PointNum) VALUES('"
					+ userId
					+ "','"
					+ traNum
					+ "','"
					+ startTime
					+ "','"
					+ endTime
					+ "','"
					+ duration
					+ "','"
					+ pointNum
					+ "')";
			stmt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//			System.out.println("error:" + sql);
//			errorLog += "error:" + sql + "\r\n";
			//throw new RuntimeException(e);
		} 
	}
	public void Close(){
		try {
			stmt.close();
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
