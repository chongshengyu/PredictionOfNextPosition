package com.yu.draw;

import java.awt.RadialGradientPaint;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.interfaces.RSAKey;
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
import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.Parameter;
import com.yu.draw.entity.RectangleZone;
import com.yu.draw.util.ConvertDistGPS;
import com.yu.draw.util.GridUtil;
import com.yu.draw.util.TraFilter;
import com.yu.prepare.util.JdbcUtil;

public class getTraInZone extends HttpServlet {

	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/html");
		String type = request.getParameter("type");
		String userId = request.getParameter("userid");

		String jsonStr = "";
		if ("getTraNums".equals(type)) {
			/*double center_lng = Double.parseDouble(request.getParameter("lng"));
			double center_lat = Double.parseDouble(request.getParameter("lat"));
			String lu_lng = String.valueOf(center_lng - Parameter.grid_width
					/ 2);
			String lu_lat = String.valueOf(center_lat + Parameter.grid_height
					/ 2);
			String rd_lng = String.valueOf(center_lng + Parameter.grid_width
					/ 2);
			String rd_lat = String.valueOf(center_lat - Parameter.grid_height
					/ 2);*/
			String centerLng = request.getParameter("lng");
			String centerLat = request.getParameter("lat");
			GPS gpsCenter = new GPS(centerLng,centerLat);
			String lu_lng = GridUtil.getGridLUGpsByCenter(gpsCenter).getLongitude();
			String lu_lat = GridUtil.getGridLUGpsByCenter(gpsCenter).getLatitude();
			String rd_lng = GridUtil.getGridRDGpsByCenter(gpsCenter).getLongitude();
			String rd_lat = GridUtil.getGridRDGpsByCenter(gpsCenter).getLatitude();
			jsonStr = getTraNums(userId, lu_lng, lu_lat, rd_lng, rd_lat);
		} else if ("showTra".equals(type)) {
			String traId = request.getParameter("traNum");
			String centerLng = request.getParameter("lng");
			String centerLat = request.getParameter("lat");
			GPS gpsCenter = new GPS(centerLng,centerLat);
			String lu_lng = GridUtil.getGridLUGpsByCenter(gpsCenter).getLongitude();
			String lu_lat = GridUtil.getGridLUGpsByCenter(gpsCenter).getLatitude();
			String rd_lng = GridUtil.getGridRDGpsByCenter(gpsCenter).getLongitude();
			String rd_lat = GridUtil.getGridRDGpsByCenter(gpsCenter).getLatitude();
			jsonStr = showTra(userId, traId, lu_lng, lu_lat, rd_lng, rd_lat);
		} else if ("showAllTra".equals(type)) {
			/*jsonStr = showAllTra(userId);*/
		}

		// System.out.println(jsonStr);
		response.getWriter().write(jsonStr);
	}

	private String getTraNums(String userId, String lu_lng, String lu_lat,
			String rd_lng, String rd_lat) {
		Connection conn = null;
		Statement stmt = null;
		String sql = "SELECT DISTINCT TraNum FROM Location where UserId='"
				+ userId + "' and Long_gcj>'" + lu_lng + "' and Long_gcj<'"
				+ rd_lng + "' and Lat_gcj>'" + rd_lat + "' and Lat_gcj<'"
				+ lu_lat + "'";
		System.out.println(sql);
		StringBuilder sb = new StringBuilder();
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				sb.append(rs.getString("TraNum") + ",");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JdbcUtil.close(conn, stmt);
		String result = sb.substring(0, sb.length() - 1);
		return result;
	}

	private String showTra(String userId, String traId, String lu_lng,
			String lu_lat, String rd_lng, String rd_lat) {
		Connection conn = null;
		Statement stmt = null;
		// String sql =
		// "SELECT Long_gcj,Lat_gcj,DateTime FROM Location WHERE UserId='"+userId+"' AND TraNum='"+traId+"' ORDER BY DateTime";
		String sql = "SELECT Long_gcj,Lat_gcj,DateTime,TraNum FROM Location WHERE UserId='"
				+ userId
				+ "' AND TraNum='"
				+ traId
				+ "' AND Long_gcj>'"
				+ lu_lng
				+ "' AND Long_gcj<'"
				+ rd_lng
				+ "' AND Lat_gcj>'"
				+ rd_lat + "' AND Lat_gcj<'" + lu_lat + "' ORDER BY DateTime";
		// System.out.println(sql);
		ArrayList<GPSPoint> tra = new ArrayList<GPSPoint>();
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				GPSPoint point = new GPSPoint(rs.getString("Long_gcj"),
						rs.getString("Lat_gcj"), rs.getString("DateTime"));
				tra.add(point);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JdbcUtil.close(conn, stmt);
		tra = TraFilter.getSparsedTra(tra);
		//将轨迹转换成cell序列
		ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(tra, GridUtil.originGPS);
		for(GPSCell cell:cellTra){
			System.out.println(cell);
		}
		String jsonStr = JSON.toJSONString(tra);
		return jsonStr;
	}

	private String showAllTra(String userId) {
		ArrayList<GPSPoint> tra = null;
		ArrayList<ArrayList<GPSPoint>> traAll = new ArrayList<ArrayList<GPSPoint>>();
		Connection conn = null;
		Statement stmt = null;
		String sql = "SELECT Long_gcj,Lat_gcj,DateTime,TraNum from Location where UserId='"
				+ userId + "' ORDER BY DateTime";
		System.out.println(sql);
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			String traNumPre = "";
			String traNum = "";
			String lng = "";
			String lat = "";
			String dateTime = "";
			while (rs.next()) {
				lng = rs.getString("Long_gcj");
				lat = rs.getString("Lat_gcj");
				traNum = rs.getString("TraNum");
				dateTime = rs.getString("DateTime");
				if (!traNumPre.equals(traNum)) {// 新轨迹
					if (tra != null) {
						traAll.add(tra);
					}
					tra = new ArrayList<GPSPoint>();
					tra.add(new GPSPoint(lng, lat, dateTime));
					traNumPre = traNum;
				} else {// 还是当前轨迹
					tra.add(new GPSPoint(lng, lat, dateTime));
					traNumPre = traNum;
				}
			}
			traAll.add(tra);// 加入最后一条轨迹
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JdbcUtil.close(conn, stmt);
		String jsonStr = JSON.toJSONString(traAll);
		return jsonStr;
	}
}
