package com.yu.evaluation;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSON;
import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.Parameter;
import com.yu.draw.entity.RectangleZone;
import com.yu.draw.entity.RectangleZoneWithScore;
import com.yu.draw.entity.Region;
import com.yu.draw.entity.RegionModel;
import com.yu.draw.entity.RegionTime;
import com.yu.draw.util.GridUtil;
import com.yu.draw.util.TraFilter;
import com.yu.evaluation.entity.ResultScores;
import com.yu.evaluation.util.TraUtil;
import com.yu.prepare.util.JdbcUtil;

public class EvaAlgoriTestServlet2 extends HttpServlet {

	/**
	 * -----------------------------------------------------------该servlet中使用的训练集是全部轨迹，，，测试集是手工选择出的用户及其部分轨迹
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/plain");
		String type = request.getParameter("type");
		
		/*使用筛选出的用户及轨迹*/
		if("getUserId".equals(type)){//请求用户id
			Connection conn = null;
			Statement stmt = null;
			StringBuilder sb = new StringBuilder();
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
				String sql = "SELECT DISTINCT UserId FROM goodtra";//使用手工筛选的用户测试
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {
					sb.append(rs.getString("UserId") + ",");
				}
				JdbcUtil.close(conn, stmt);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			if (sb.length() > 0)
				sb = sb.deleteCharAt(sb.length() - 1);
			 response.getWriter().write(sb.toString());
		}else if("userTestTraNo".equals(type)){//请求测试轨迹号
			String userId = request.getParameter("userId");
			Connection conn = null;
			Statement stmt = null;
			String sql = "";
			int traCount = 0;
			ArrayList<String> testSetTraNo = new ArrayList<String>();
			//该用户轨迹总数
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
				sql = "SELECT COUNT(*) AS TraCount FROM goodtra WHERE UserId='"+userId+"'";
				ResultSet rs = stmt.executeQuery(sql);
				while(rs.next()){
					traCount = rs.getInt("TraCount");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			//得到测试集轨迹号
			sql = "SELECT TraNum FROM goodTra WHERE UserId='"+userId+"'";
//			sql = "select TraNum from (select TraNum from goodtra where UserId='"+userId+"' order by TraNum DESC limit "+(int)(traCount *(1- Parameter.PROP_OF_SAMPLE_SET))+" ) as t ORDER BY TraNum";
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				while(rs.next()){
					testSetTraNo.add(rs.getString("TraNum"));
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
			StringBuilder sb = new StringBuilder();
			for(String s:testSetTraNo){
				sb.append(s + ",");
			}
			if(sb.length() > 0){
				sb = sb.deleteCharAt(sb.length() - 1);
			}
			response.getWriter().write(sb.toString());
		}else if("getTraPoints".equals(type)){//请求用户轨迹点
			String userId = request.getParameter("userId");
			String traNo = request.getParameter("traNo");
			Connection conn = null;
			Statement stmt = null;
			String sql = "SELECT Long_gcj,Lat_gcj,DateTime FROM Location WHERE UserId='"+userId+"' AND TraNum='"+traNo+"' ORDER BY DateTime";
			ResultSet rs;
			ArrayList<GPSPoint> gpsList = new ArrayList<GPSPoint>();
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					GPSPoint point = new GPSPoint(rs.getString("Long_gcj"),
							rs.getString("Lat_gcj"), rs.getString("DateTime"));
					gpsList.add(point);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JdbcUtil.close(conn, stmt);
			
			gpsList = TraFilter.getSparsedTra(gpsList);//过滤
			String jsonString = JSON.toJSONString(gpsList);
			response.getWriter().write(jsonString);
		}else if("getRegion".equals(type)){//请求region
			String userId = request.getParameter("userId");
			String center_lng = request.getParameter("lng");
			String center_lat = request.getParameter("lat");
			String testTraNo = request.getParameter("testTraNo");
			GPS gpsCenter = new GPS(center_lng, center_lat);
			// 得到Grid左上角和右下角坐标
			String lu_lng = GridUtil.getGridLUGpsByCenter(gpsCenter).getLongitude();
			String lu_lat = GridUtil.getGridLUGpsByCenter(gpsCenter).getLatitude();
			String rd_lng = GridUtil.getGridRDGpsByCenter(gpsCenter).getLongitude();
			String rd_lat = GridUtil.getGridRDGpsByCenter(gpsCenter).getLatitude();
			// 得到Grid原点坐标
			GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);
			HttpSession session = request.getSession();
			session.setAttribute("originGps", originGps);
			session.setAttribute("gpsCenter", gpsCenter);
			
			ArrayList<String> effectiveTraNo = new ArrayList<String>();//该用户的全部训练轨迹（包括训练集和测试集）
			Connection conn = null;
			Statement stmt = null;
			String sql = "SELECT TraNum,PointNum from trajectory WHERE UserId='"+userId+"' and TraNum<'"+testTraNo+"' ORDER BY TraNum";
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
			JdbcUtil.close(conn, stmt);
			
			ArrayList<String> trainTraNoList = effectiveTraNo;//训练集轨迹号
			
			// 得到区域内的所有训练集轨迹
			ArrayList<ArrayList<GPSPoint>> traList = TraUtil.getTraInGrid(userId, lu_lng, lu_lat, rd_lng, rd_lat, trainTraNoList);
			if(traList.size() == 0){
				System.out.println("区域内没有训练集");
				return ;
			}
			System.out.println("使用全部数据训练，训练集大小："+traList.size());
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
			
			//测试输出
			/*for(int i=0;i<numArr.length;i++){
				for(int j=0;j<numArr.length;j++){
					System.out.print(numArr[i][j] + " ");
				}
				System.out.println();
			}*/
			
			//得到regions
			LinkedList<Region> regionList = GridUtil.getRegionListByPointNum(numArr, originGps);
			//每条轨迹对应的regionTra
			ArrayList<ArrayList<RegionTime>> regionTraList = GridUtil.cellTraList2RegionTraList(cellTraList, regionList);
			//regionTraList => modelMap
			HashMap<Region, ArrayList<RegionModel>> modelMap = GridUtil.getModelMap(regionList, regionTraList);
			session.setAttribute("modelMap", modelMap);
			// grid区域
			RectangleZone rz = new RectangleZone(lu_lng, lu_lat, rd_lng, rd_lat);
			ArrayList<RectangleZone> zoneList = new ArrayList<RectangleZone>();
			zoneList.add(rz);
			//画出每个region
			for(Region region:regionList){
				RectangleZone rzc = new RectangleZone(region.getLuGps().getLongitude(), region.getLuGps().getLatitude(), region.getRdGps().getLongitude(), region.getRdGps().getLatitude());
				zoneList.add(rzc);
			}
			String jsonStr = JSON.toJSONString(zoneList);
			response.getWriter().write(jsonStr);
		}else if("getScore".equals(type)){
			String userId = request.getParameter("userId");
			//预测时间
			String time = request.getParameter("time");
			//已知的位置点，这里用点击的marker的位置
			ArrayList<GPS> gpsList = new ArrayList<GPS>();
			gpsList.add((GPS) request.getSession().getAttribute("gpsCenter"));
			//session中的grid原点
			GPS originGps = (GPS) request.getSession().getAttribute("originGps");
			//session中的modelMap
			@SuppressWarnings("unchecked")
			HashMap<Region, ArrayList<RegionModel>> modelMap = (HashMap<Region, ArrayList<RegionModel>>)request.getSession().getAttribute("modelMap");
			HashMap<Region, ResultScores> scoreMap = TraUtil.getScoreMap(gpsList, originGps, time, modelMap, 1.2, 1.0);
			ArrayList<RectangleZoneWithScore> rectangleScoreList = new ArrayList<RectangleZoneWithScore>();
			if(scoreMap !=null){
				for(Region region:scoreMap.keySet()){
					rectangleScoreList.add(new RectangleZoneWithScore(region.getLuGps().getLongitude(), region.getLuGps().getLatitude(), region.getRdGps().getLongitude(), region.getRdGps().getLatitude(), ""+scoreMap.get(region).getMyScore()));
				}
			}//scoreMap为空，无法预测
			String result = JSON.toJSONString(rectangleScoreList);
			response.getWriter().write(result);
		}
	}

}
