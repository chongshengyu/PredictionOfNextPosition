package com.yu.draw;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSON;
import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.GridCell;
import com.yu.draw.entity.Parameter;
import com.yu.draw.entity.RectangleZone;
import com.yu.draw.entity.Region;
import com.yu.draw.entity.RegionModel;
import com.yu.draw.entity.RegionTime;
import com.yu.draw.util.GridUtil;

public class predictionServlet extends HttpServlet {

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

		response.setContentType("text/plain");
		String type = request.getParameter("type");
		String userId = "";
		String center_lng = "";
		String center_lat = "";
		if("getRegion".equals(type)){
			userId = request.getParameter("userId");
			center_lng = request.getParameter("lng");
			center_lat = request.getParameter("lat");
			GPS gpsCenter = new GPS(center_lng, center_lat);
			// 得到Grid左上角和右下角坐标
			String lu_lng = GridUtil.getGridLUGpsByCenter(gpsCenter).getLongitude();
			String lu_lat = GridUtil.getGridLUGpsByCenter(gpsCenter).getLatitude();
			String rd_lng = GridUtil.getGridRDGpsByCenter(gpsCenter).getLongitude();
			String rd_lat = GridUtil.getGridRDGpsByCenter(gpsCenter).getLatitude();
			// 得到Grid原点坐标
			GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);
			// 得到cellList
			ArrayList<GridCell> cellList = GridUtil.getGridCellsListByCenter(gpsCenter);
			// 得到区域内的所有轨迹
			ArrayList<ArrayList<GPSPoint>> traList = GridUtil.getTraInGrid(userId, lu_lng, lu_lat, rd_lng, rd_lat);
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
			//测试输出modelMap
			System.out.println("modelMap:"+modelMap.size());
			for(Region region:modelMap.keySet()){
				System.out.print(region+">>");
				ArrayList<RegionModel> regionModelList = modelMap.get(region);
				System.out.print("models:"+regionModelList.size()+">>");
				for(RegionModel rm:regionModelList){
					System.out.print(rm);
				}
				System.out.println("\n");
			}
			// grid区域
			RectangleZone rz = new RectangleZone(lu_lng, lu_lat, rd_lng, rd_lat);
			ArrayList<RectangleZone> zoneList = new ArrayList<RectangleZone>();
			zoneList.add(rz);
			/*// 画出细分的每个cell
			for (GridCell gridCell : cellList) {
				RectangleZone rzc = new RectangleZone(gridCell.getLuGps()
						.getLongitude(), gridCell.getLuGps().getLatitude(),
						gridCell.getRdGps().getLongitude(), gridCell.getRdGps()
								.getLatitude());
				zoneList.add(rzc);
			}*/
			//画出每个region
			for(Region region:regionList){
				RectangleZone rzc = new RectangleZone(region.getLuGps().getLongitude(), region.getLuGps().getLatitude(), region.getRdGps().getLongitude(), region.getRdGps().getLatitude());
				zoneList.add(rzc);
			}
			// 将grid cells保存到session中
			
			String jsonStr = JSON.toJSONString(zoneList);
			// System.out.println(jsonStr);
			response.getWriter().write(jsonStr);
		}else if("prediction".equals(request.getParameter("type"))){
			userId = request.getParameter("userId");
			//time=" + time + "&markerPositions=" + markerPositions,
			String time = request.getParameter("time");
			String markerPosition = request.getParameter("markerPositions");
			ArrayList<GPS> gpsList = new ArrayList<GPS>();
			String[] points = markerPosition.split(";");
			for(String ss:points){
				gpsList.add(new GPS(ss.split(",")[0],ss.split(",")[1]));
				System.out.println(ss);
			}
			System.out.println(time);
		}
	}

}
