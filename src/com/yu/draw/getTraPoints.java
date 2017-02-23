package com.yu.draw;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.util.TraFilter;
import com.yu.prepare.util.JdbcUtil;

public class getTraPoints extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		String userId = request.getParameter("userid");
		String traId = request.getParameter("traid");
		boolean corredtedAll = request.getParameter("correctedAll").equals("1") ? true : false;//纠偏后的所有轨迹
		boolean corrected = request.getParameter("corrected").equals("1") ? true
				: false;// 纠偏后的数据
		boolean uncorrected = request.getParameter("uncorrected").equals("1") ? true
				: false;// 纠偏前的数据
		Connection conn = null;
		Statement stmt = null;
		String sql = "";
		String jsonString = "";
		ArrayList<ArrayList<GPSPoint>> traAll = new ArrayList<ArrayList<GPSPoint>>();
		ArrayList<GPSPoint> traPoint_corrected;
		ArrayList<GPSPoint> points_corrected = new ArrayList<GPSPoint>();
		ArrayList<GPSPoint> points_uncorrected = new ArrayList<GPSPoint>();
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(corredtedAll){
			sql = "SELECT TraNum FROM trajectory WHERE UserId='"+userId+"' ORDER BY TraNum";
			System.out.println(sql);
			ArrayList<String> traIdList = new ArrayList<String>();
			ResultSet rs;
			try {
				rs = stmt.executeQuery(sql);
				while(rs.next()){
					traIdList.add(rs.getString("TraNum"));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(String tid : traIdList){
				sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM Location WHERE UserId='"+userId+"' AND TraNum='"+tid+"' ORDER BY DateTime";
//				traPoint_corrected.clear();
				traPoint_corrected = new ArrayList<GPSPoint>();
				try {
					rs = stmt.executeQuery(sql);
					while(rs.next()){
						GPSPoint point = new GPSPoint(rs.getString("Long_gcj"), rs.getString("Lat_gcj"), rs.getString("DateTime"));
						traPoint_corrected.add(point);
					}
					traAll.add(traPoint_corrected);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			jsonString = JSON.toJSONString(traAll);
		}else{
			if (corrected) {
				sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM Location WHERE UserId='"+userId+"' AND TraNum='"+traId+"' ORDER BY DateTime";
				ResultSet rs;
				try {
					rs = stmt.executeQuery(sql);
					while (rs.next()) {
						GPSPoint point = new GPSPoint(rs.getString("Long_gcj"),
								rs.getString("Lat_gcj"), rs.getString("DateTime"));
						points_corrected.add(point);
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (uncorrected) {
				sql = "SELECT Long_wgs,Lat_wgs,DateTime FROM Location WHERE UserId='"+userId+"' AND TraNum='"+traId+"' ORDER BY DateTime";
				ResultSet rs;
				try {
					rs = stmt.executeQuery(sql);
					while (rs.next()) {
						GPSPoint point = new GPSPoint(rs.getString("Long_wgs"),
								rs.getString("Lat_wgs"), rs.getString("DateTime"));
						points_uncorrected.add(point);
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			ArrayList<ArrayList<GPSPoint>> arrayList = new ArrayList<ArrayList<GPSPoint>>();
			arrayList.add(points_uncorrected);
			points_corrected = TraFilter.getSparsedTra(points_corrected);//过滤
			arrayList.add(points_corrected);
//			System.out.println("过滤后长度："+points_corrected.size());//281->19
			jsonString = JSON.toJSONString(arrayList);
		}
		
		JdbcUtil.close(conn, stmt);
//		System.out.println("请求轨迹点输出："+jsonString);
//		LogToFile(jsonString);
		response.getWriter().write(jsonString);
	}
	private void LogToFile(String s) throws IOException {
		System.out.println("进入");
		File file = new File(this.getServletContext().getRealPath("/WEB-INF/classes/log.txt"));
		if(!file.exists()){
			file.createNewFile();
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
		writer.write(s + "\r\n");
		writer.close();
	}

}
