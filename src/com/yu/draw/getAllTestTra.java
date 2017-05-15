package com.yu.draw;

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

public class getAllTestTra extends HttpServlet {

	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		System.out.println("in");
		response.setContentType("text/plain");
		Connection conn = null;
		Statement stmt = null;
		String sql = "";
		String jsonString = "";
		ArrayList<ArrayList<GPSPoint>> traAll = new ArrayList<ArrayList<GPSPoint>>();
		ArrayList<GPSPoint> traPointsList;
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sql = "SELECT TraNum FROM goodtratotest";
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
		System.out.println("轨迹总数："+traIdList.size());
		for(String tid : traIdList){
			sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM Location where TraNum='"+tid+"' ORDER BY DateTime";
			traPointsList = new ArrayList<GPSPoint>();
			try {
				rs = stmt.executeQuery(sql);
				while(rs.next()){
					GPSPoint point = new GPSPoint(rs.getString("Long_gcj"), rs.getString("Lat_gcj"), rs.getString("DateTime"));
					traPointsList.add(point);
				}
				traPointsList = TraFilter.getSparsedTra(traPointsList);//过滤
				traAll.add(traPointsList);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		JdbcUtil.close(conn, stmt);
		jsonString = JSON.toJSONString(traAll);
		response.getWriter().write(jsonString);
	}

}
