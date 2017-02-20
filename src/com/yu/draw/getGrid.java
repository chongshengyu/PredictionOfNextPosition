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
import com.yu.draw.util.TraFilter;

public class getGrid extends HttpServlet {

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

		response.setContentType("text/plain");
		String userId = request.getParameter("userid");

		String centerLng = request.getParameter("lng");
		String centerLat = request.getParameter("lat");
		GPS gpsCenter = new GPS(centerLng, centerLat);
		// 得到Grid左上角和右下角坐标
		String lu_lng = GridUtil.getGridLUGpsByCenter(gpsCenter).getLongitude();
		String lu_lat = GridUtil.getGridLUGpsByCenter(gpsCenter).getLatitude();
		String rd_lng = GridUtil.getGridRDGpsByCenter(gpsCenter).getLongitude();
		String rd_lat = GridUtil.getGridRDGpsByCenter(gpsCenter).getLatitude();
		// 得到Grid原点坐标
		GPS originGps = GridUtil.getGridOriginGpsByCenter(gpsCenter);
		// 得到cellList
		ArrayList<GridCell> cellList = GridUtil
				.getGridCellsListByCenter(gpsCenter);
		// 怎么将gps轨迹转换成cell轨迹？
		// 使用轨迹号01320081111232229轨迹测试
		// 构建模式图，并保存到session

		// 此时应该查询该用户的所有位置轨迹
		// 测试阶段，只使用三条轨迹：01320080927120819，01320080927233805，01320081006232359

		// 得到区域内的所有轨迹
		ArrayList<ArrayList<GPSPoint>> traList = GridUtil.getTraInGrid(userId, lu_lng, lu_lat, rd_lng, rd_lat);
		System.out.println("TraLength"+traList.size());
		//将轨迹转换成cell序列
		ArrayList<ArrayList<GPSCell>> cellTraList = new ArrayList<ArrayList<GPSCell>>();
		for(ArrayList<GPSPoint> tra:traList){
			ArrayList<GPSCell> cellTra = GridUtil.GPSTraToCellTra(tra, originGps);
			System.out.println("new tra---------");
			for(GPSCell cell:cellTra){
				System.out.println(cell.getGridX()+"-"+cell.getGridY());
			}
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
		for(int i = 0;i<numArr.length;i++){
			for(int j=0;j<numArr.length;j++){
				System.out.print(numArr[i][j] +" ");
			}
			System.out.println();
		}
		//得到regions
		LinkedList<Region> regionList = GridUtil.getRegionListByPointNum(numArr, originGps);
		for(Region region:regionList){
			System.out.println(region);
		}
		//将cellTra转化为regionTra，建立cell和region的双向映射
		//建立cell到region的映射
		
		//cellTraList => regionTraList
		//每条轨迹对应的regionTra
		ArrayList<ArrayList<RegionTime>> regionTraList = GridUtil.cellTraList2RegionTraList(cellTraList, regionList);
		//测试regionTraList输出
		for(ArrayList<RegionTime> regionTra:regionTraList){
			System.out.println("new region tra.");
			for(RegionTime regionTime:regionTra){
				System.out.println(regionTime.getRegion()+"+++"+regionTime.getTime());
			}
		}
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
		response.getWriter().write(jsonStr);
	}

}
