package com.yu.draw.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import com.yu.draw.entity.Cell;
import com.yu.draw.entity.GPS;
import com.yu.draw.entity.GPSCell;
import com.yu.draw.entity.GPSPoint;
import com.yu.draw.entity.GridCell;
import com.yu.draw.entity.Parameter;
import com.yu.draw.entity.Region;
import com.yu.draw.entity.RegionModel;
import com.yu.draw.entity.RegionTime;

public class GridUtil {
	private static ArrayList<GridCell> cellList;
	public static GPS originGPS;

	// 正北为0度，顺时针度数增加。
	// 得到Grid左上角坐标
	public static GPS getGridLUGpsByCenter(GPS center) {
		GPS luGps = ConvertDistGPS
				.ConvertDistanceToLogLat(center, Math.sqrt(2)
						* (double) (Parameter.cellWidth * Parameter.gridWidth)
						/ 2, -45);
		return luGps;
	}

	// 得到Grid右上角坐标
	public static GPS getGridRDGpsByCenter(GPS center) {
		GPS luGps = ConvertDistGPS
				.ConvertDistanceToLogLat(center, Math.sqrt(2)
						* (double) (Parameter.cellWidth * Parameter.gridWidth)
						/ 2, 135);
		return luGps;
	}

	// 得到Grid原点坐标，即左下角坐标
	public static GPS getGridOriginGpsByCenter(GPS center) {
		GPS originGps = ConvertDistGPS.ConvertDistanceToLogLat(center,
				Math.sqrt(2)
						* (double) (Parameter.cellWidth * Parameter.gridWidth)
						/ 2, 225);
		originGPS = originGps;
		return originGps;
	}

	// 得到grid cells list，list中，cell按行主序排列，即(0,0),(1,0),(2,0)
	// ……,(0,1),(1,1),(2,1)……
	public static ArrayList<GridCell> getGridCellsListByCenter(GPS center) {
		GPS gridOrigin = ConvertDistGPS.ConvertDistanceToLogLat(center,
				Math.sqrt(2)
						* (double) (Parameter.cellWidth * Parameter.gridWidth)
						/ 2, 225);
		// ArrayList<GridCell> cellList = new ArrayList<GridCell>();
		cellList = new ArrayList<GridCell>();
		for (int j = 0; j < Parameter.gridWidth; j++) {
			for (int i = 0; i < Parameter.gridWidth; i++) {
				GridCell cell = new GridCell(i, j, gridOrigin);// cell内点数默认为0
				cellList.add(cell);
			}
		}
		return cellList;
	}

	// 获取cellList
	public static ArrayList<GridCell> getGridCellsListByCenter() {
		return cellList;
	}

	// 由GPS得到GPSCell
	public static GPSCell GPSToGPSCell(GPSPoint gps, GPS originGps) {
		String gpsLng = gps.getLang();
		String gpsLat = gps.getLat();
		String originGpsLng = originGps.getLongitude();
		String originGpsLat = originGps.getLatitude();
		double distanceInY = ConvertDistGPS.ConvertLogLatToDistance(new GPS(
				originGpsLng, originGpsLat), new GPS(originGpsLng, gpsLat));
		double distanceInX = ConvertDistGPS.ConvertLogLatToDistance(new GPS(
				originGpsLng, originGpsLat), new GPS(gpsLng, originGpsLat));
		int y = (int) (distanceInY / Parameter.cellWidth);
		int x = (int) (distanceInX / Parameter.cellWidth);
		if (x >= Parameter.gridWidth || y >= Parameter.gridWidth)
			return null;
		return new GPSCell(x, y, gps.getDateTime());
	}

	// 由GPS轨迹的到cell轨迹
	public static ArrayList<GPSCell> GPSTraToCellTra(
			ArrayList<GPSPoint> pointList, GPS originGps) {
		LinkedList<GPSCell> cellList = new LinkedList<GPSCell>();
		for (GPSPoint point : pointList) {
			GPSCell cell = GPSToGPSCell(point, originGps);
			if (cell == null) {// 超出grid范围，舍弃
				continue;
			}
			if (cellList.size() == 0) {
				cellList.add(cell);
			} else {
				if (cell.getGridX() == cellList.get(cellList.size() - 1)
						.getGridX()
						&& cell.getGridY() == cellList.get(cellList.size() - 1)
								.getGridY()) {
					// 还在同一个cell，舍弃
				} else {
					// 不在同一个cell，插入
					cellList.add(cell);
				}
			}
		}
		// 还要通过插值法，保证只能横向或纵向移动？需要吗？
		/*
		 * int i = 0; for (GPSCell cell : cellList) { if(i != 0){//从第一个开始 int
		 * preX = cellList.get(i -1).getGridX(); int preY = cellList.get(i
		 * -1).getGridY(); int x = cellList.get(i).getGridX(); int y =
		 * cellList.get(i).getGridY(); } i++; }
		 */
		// 转为ArrayList
		ArrayList<GPSCell> arrayList = new ArrayList<GPSCell>();
		for (GPSCell cell : cellList) {
			arrayList.add(cell);
		}
		return arrayList;
	}

	/**
	 * 根据用二维数组表示的cell内点数，得到region list
	 * 
	 * @param numArr
	 * @param originGps
	 *            grid原点gps
	 * @return region list
	 */
	public static LinkedList<Region> getRegionListByPointNum(int[][] numArr,
			GPS originGps) {
		boolean[][] usedFlag = new boolean[numArr.length][numArr.length];// cell是否已使用的标记数组
		for (int i = 0; i < usedFlag.length; i++) {
			for (int j = 0; j < usedFlag.length; j++) {
				usedFlag[i][j] = false;
			}
		}
		LinkedList<GridCell> validCellList = new LinkedList<GridCell>();
		LinkedList<Region> regionList = new LinkedList<Region>();
		// 遍历数组，将有效cell插入validCellList
		for (int i = 0; i < numArr.length; i++) {
			for (int j = 0; j < numArr[0].length; j++) {
				if (numArr[i][j] >= Parameter.leastPointNumInCell) {
					GridCell gridCell = new GridCell(i, j, originGps);
					gridCell.setPointNumInCell(numArr[i][j]);
					validCellList.add(gridCell);
				}
			}
		}
		// 递减序排序validCellList
		validCellList = AlgorithmUtil.InsertSort(validCellList);
		// 临时存储当前被扩充进来的cells
		ArrayList<GridCell> cells = new ArrayList<GridCell>();
		// 记录当前cells是否还能扩充
		boolean canBeExtended = false;
		// 开始聚合
		for (int i = 0; i < validCellList.size(); i++) {
			int x = validCellList.get(i).getGridX();
			int y = validCellList.get(i).getGridY();
			if (usedFlag[x][y] == false) {
				cells.clear();
				canBeExtended = true;
				cells.add(validCellList.get(i));
				usedFlag[validCellList.get(i).getGridX()][validCellList.get(i).getGridY()] = true;
				while (canBeExtended) {
					canBeExtended = extend(cells, validCellList, usedFlag);
				}
				// 扩展结束，将cell里的cell生成region，保存至regionList
				if (cells.size() != 0) {
					Region region = getRegionByCells(cells);
					regionList.add(region);
				}
			}
		}
		return regionList;
	}

	/**
	 * 将cells依次向四个方向extend，若都不能扩展则返回false，否则返回true
	 * 
	 * @param cells
	 *            当前工作区域包含的cells，因为参数是引用类型，在这做的修个可以保存。
	 * @param validCellList
	 *            按点数递减序的cell list
	 * @param usedFlag
	 *            标志cell是否被聚合过的标志数据
	 * @return 若4个方向均不能扩展，返回false，否则返回true
	 */
	private static boolean extend(ArrayList<GridCell> cells,
			LinkedList<GridCell> validCellList, boolean[][] usedFlag) {
		int numOfDirectionExtended = 0;// 可以扩展的方向数，若为0，则不能扩展，返回false
		// 建立cell到pointNumber的映射
		LinkedHashMap<Cell, GridCell> pointNumMap = new LinkedHashMap<Cell, GridCell>();
		for (GridCell gc : validCellList) {
			Cell cell = new Cell(gc.getGridX(), gc.getGridY());
			pointNumMap.put(cell, gc);
		}
		// Up
		ArrayList<Cell> upCells = findCellsOnUp(cells);// 上边（外部）的cells
		if (upCells == null) {
			// 到达最上部，不能向上合并
		} else {
			boolean pointNumSameUnused = true;
			for (Cell uc : upCells) {
				// 在map中get的时候，需要重写Cell的hashCode和equals方法来进行值的比较，否则使用的是地址比较
				if (pointNumMap.get(uc) == null || pointNumMap.get(uc).getPointNumInCell() != cells.get(0)
						.getPointNumInCell()) {
					pointNumSameUnused = false;//出界或点数不同
					break;
				} else {
					// 点数相同，再看有没有被用过
					int x = uc.getGridX();
					int y = uc.getGridY();
					if (usedFlag[x][y] == true) {// 被用过
						pointNumSameUnused = false;
						break;
					}
				}
			}
			if (pointNumSameUnused) {
				// 点数相同并且都没有被用过，可以合并
				numOfDirectionExtended += 1;
				// 把upCells中对应的validCellList中的cell加入到cells，并设置usedFlag
				for (GridCell gc : validCellList) {
					for (Cell c : upCells) {
						if (gc.getGridX() == c.getGridX()
								&& gc.getGridY() == c.getGridY()) {
							cells.add(gc);
							usedFlag[c.getGridX()][c.getGridY()] = true;
						}
					}
				}
			} else {
				// 点数不同或者有被用过的，不能合并
			}
		}
		// Down
		ArrayList<Cell> downCells = findCellsOnDown(cells);// 下边（外部）的cells
		if (downCells == null) {
			// 到达最下部，不能向下合并
		} else {
			boolean pointNumSameUnused = true;
			for (Cell uc : downCells) {
				// 在map中get的时候，需要重写Cell的hashCode和equals方法来进行值的比较，否则使用的是地址比较
				if (pointNumMap.get(uc) == null || pointNumMap.get(uc).getPointNumInCell() != cells.get(0)
						.getPointNumInCell()) {
					pointNumSameUnused = false;//出界或点数不同
					break;
				} else {
					// 点数相同，再看有没有被用过
					int x = uc.getGridX();
					int y = uc.getGridY();
					if (usedFlag[x][y] == true) {// 被用过
						pointNumSameUnused = false;
						break;
					}
				}
			}
			if (pointNumSameUnused) {
				// 点数相同并且都没有被用过，可以合并
				numOfDirectionExtended += 1;
				// 把downCells中对应的validCellList中的cell加入到cells，并设置usedFlag
				for (GridCell gc : validCellList) {
					for (Cell c : downCells) {
						if (gc.getGridX() == c.getGridX()
								&& gc.getGridY() == c.getGridY()) {
							cells.add(gc);
							usedFlag[c.getGridX()][c.getGridY()] = true;
						}
					}
				}
			} else {
				// 点数不同或者有被用过的，不能合并
			}
		}
		// Left
		ArrayList<Cell> leftCells = findCellsOnLeft(cells);// 左边（外部）的cells
		if (leftCells == null) {
			// 到达最下部，不能向下合并
		} else {
			boolean pointNumSameUnused = true;
			for (Cell uc : leftCells) {
				// 在map中get的时候，需要重写Cell的hashCode和equals方法来进行值的比较，否则使用的是地址比较
				if (pointNumMap.get(uc) == null || pointNumMap.get(uc).getPointNumInCell() != cells.get(0)
						.getPointNumInCell()) {
					pointNumSameUnused = false;//出界或点数不同
					break;
				} else {
					// 点数相同，再看有没有被用过
					int x = uc.getGridX();
					int y = uc.getGridY();
					if (usedFlag[x][y] == true) {// 被用过
						pointNumSameUnused = false;
						break;
					}
				}
			}
			if (pointNumSameUnused) {
				// 点数相同并且都没有被用过，可以合并
				numOfDirectionExtended += 1;
				// 把leftCells中对应的validCellList中的cell加入到cells，并设置usedFlag
				for (GridCell gc : validCellList) {
					for (Cell c : leftCells) {
						if (gc.getGridX() == c.getGridX()
								&& gc.getGridY() == c.getGridY()) {
							cells.add(gc);
							usedFlag[c.getGridX()][c.getGridY()] = true;
						}
					}
				}
			} else {
				// 点数不同或者有被用过的，不能合并
			}
		}
		// Right
		ArrayList<Cell> rightCells = findCellsOnRight(cells);// 右边（外部）的cells
		if (rightCells == null) {
			// 到达最右部，不能向右合并
		} else {
			boolean pointNumSameUnused = true;
			for (Cell uc : rightCells) {
				// 在map中get的时候，需要重写Cell的hashCode和equals方法来进行值的比较，否则使用的是地址比较
				if (pointNumMap.get(uc) == null || pointNumMap.get(uc).getPointNumInCell() != cells.get(0)
						.getPointNumInCell()) {
					pointNumSameUnused = false;//出界或点数不同
					break;
				} else {
					// 点数相同，再看有没有被用过
					int x = uc.getGridX();
					int y = uc.getGridY();
					if (usedFlag[x][y] == true) {// 被用过
						pointNumSameUnused = false;
						break;
					}
				}
			}
			if (pointNumSameUnused) {
				// 点数相同并且都没有被用过，可以合并
				numOfDirectionExtended += 1;
				// 把rightCells中对应的validCellList中的cell加入到cells，并设置usedFlag
				for (GridCell gc : validCellList) {
					for (Cell c : rightCells) {
						if (gc.getGridX() == c.getGridX()
								&& gc.getGridY() == c.getGridY()) {
							cells.add(gc);
							usedFlag[c.getGridX()][c.getGridY()] = true;
						}
					}
				}
			} else {
				// 点数不同或者有被用过的，不能合并
			}
		}
		if (numOfDirectionExtended == 0)
			return false;
		return true;
	}

	/**
	 * 得到位于cells上边（外部）的upCells
	 * 
	 * @param cells
	 * @return 位于cells上边（外部）的upCells，若返回null，则说明已经到了Grid最上方
	 */
	private static ArrayList<Cell> findCellsOnUp(ArrayList<GridCell> cells) {
		ArrayList<Cell> upCells = new ArrayList<Cell>();
		// 先找到cells内部最上方的cells
		for (GridCell gc : cells) {
			Cell c = new Cell(gc.getGridX(), gc.getGridY());
			if (upCells.size() == 0) {
				upCells.add(c);
			} else if (gc.getGridY() > upCells.get(0).getGridY()) {// Y坐标更大
				upCells.clear();
				upCells.add(c);
			} else if (gc.getGridY() == upCells.get(0).getGridY()) {
				upCells.add(c);
			}
		}
		// 此时upCells里是cells中最上方的cell，判断是否会越出Grid
		if (upCells.get(0).getGridY() == Parameter.gridWidth - 1) {
			// 已经位于Grid最上方，上边已没有cell
			return null;
		}
		// 不会越出Grid,将最上方的cell替换为上边（外部）的cell
		//为了解决并发修改异常，不使用增强for，而用迭代器，并且用迭代器的增删方法
		/*for (Cell c : upCells) {//并发修改异常
			int x = c.getGridX();
			int y = c.getGridY() + 1;
			upCells.remove(c);
			upCells.add(new Cell(x, y));
		}*/
		ListIterator<Cell> iterator = upCells.listIterator();
		while(iterator.hasNext()){
			Cell c= iterator.next();
			int x = c.getGridX();
			int y = c.getGridY() + 1;
			iterator.remove();
			iterator.add(new Cell(x, y));
		}
		return upCells;
	}

	/**
	 * 得到位于cells下边（外部）的downCells
	 * 
	 * @param cells
	 * @return 位于cells下边（外部）的downCells，若返回null，则说明已经到了Grid最下方
	 */
	private static ArrayList<Cell> findCellsOnDown(ArrayList<GridCell> cells) {
		ArrayList<Cell> downCells = new ArrayList<Cell>();
		// 先找到cells内部最下方的cells
		for (GridCell gc : cells) {
			Cell c = new Cell(gc.getGridX(), gc.getGridY());
			if (downCells.size() == 0) {
				downCells.add(c);
			} else if (gc.getGridY() < downCells.get(0).getGridY()) {// Y坐标更小
				downCells.clear();
				downCells.add(c);
			} else if (gc.getGridY() == downCells.get(0).getGridY()) {
				downCells.add(c);
			}
		}
		// 此时downCells里是cells中最下方的cell，判断是否会越出Grid
		if (downCells.get(0).getGridY() == 0) {
			// 已经位于Grid最下方，下边已没有cell
			return null;
		}
		// 不会越出Grid,将最下方的cell替换为下边（外部）的cell
		//为了解决并发修改异常，不使用增强for，而用迭代器，并且用迭代器的增删方法
		/*for (Cell c : downCells) {
			int x = c.getGridX();
			int y = c.getGridY() - 1;
			downCells.remove(c);
			downCells.add(new Cell(x, y));
		}*/
		ListIterator<Cell> iterator = downCells.listIterator();
		while(iterator.hasNext()){
			Cell c= iterator.next();
			int x = c.getGridX();
			int y = c.getGridY() - 1;
			iterator.remove();
			iterator.add(new Cell(x, y));
		}
		return downCells;
	}

	/**
	 * 得到位于cells左边（外部）的leftCells
	 * 
	 * @param cells
	 * @return 位于cells左边（外部）的leftCells，若返回null，则说明已经到了Grid最左方
	 */
	private static ArrayList<Cell> findCellsOnLeft(ArrayList<GridCell> cells) {
		ArrayList<Cell> leftCells = new ArrayList<Cell>();
		// 先找到cells内部最下方的cells
		for (GridCell gc : cells) {
			Cell c = new Cell(gc.getGridX(), gc.getGridY());
			if (leftCells.size() == 0) {
				leftCells.add(c);
			} else if (gc.getGridX() < leftCells.get(0).getGridX()) {// X坐标更小
				leftCells.clear();
				leftCells.add(c);
			} else if (gc.getGridX() == leftCells.get(0).getGridX()) {
				leftCells.add(c);
			}
		}
		// 此时downCells里是cells中最左方的cell，判断是否会越出Grid
		if (leftCells.get(0).getGridX() == 0) {
			// 已经位于Grid最左方，左边已没有cell
			return null;
		}
		// 不会越出Grid,将最左方的cell替换为左边（外部）的cell
		//为了解决并发修改异常，不使用增强for，而用迭代器，并且用迭代器的增删方法
		/*for (Cell c : leftCells) {
			int x = c.getGridX() - 1;
			int y = c.getGridY();
			leftCells.remove(c);
			leftCells.add(new Cell(x, y));
		}*/
		ListIterator<Cell> iterator = leftCells.listIterator();
		while(iterator.hasNext()){
			Cell c= iterator.next();
			int x = c.getGridX() - 1;
			int y = c.getGridY();
			iterator.remove();
			iterator.add(new Cell(x, y));
		}
		return leftCells;
	}

	/**
	 * 得到位于cells右边（外部）的rightCells
	 * 
	 * @param cells
	 * @return 位于cells右边（外部）的rightCells，若返回null，则说明已经到了Grid最右方
	 */
	private static ArrayList<Cell> findCellsOnRight(ArrayList<GridCell> cells) {
		ArrayList<Cell> rightCells = new ArrayList<Cell>();
		// 先找到cells内部最右方的cells
		for (GridCell gc : cells) {
			Cell c = new Cell(gc.getGridX(), gc.getGridY());
			if (rightCells.size() == 0) {
				rightCells.add(c);
			} else if (gc.getGridX() > rightCells.get(0).getGridX()) {// X坐标更大
				rightCells.clear();
				rightCells.add(c);
			} else if (gc.getGridX() == rightCells.get(0).getGridX()) {
				rightCells.add(c);
			}
		}
		// 此时rightCells里是cells中最右方的cell，判断是否会越出Grid
		if (rightCells.get(0).getGridX() == Parameter.gridWidth - 1) {
			// 已经位于Grid最右方，右边已没有cell
			return null;
		}
		// 不会越出Grid,将最右方的cell替换为右边（外部）的cell
		//为了解决并发修改异常，不使用增强for，而用迭代器，并且用迭代器的增删方法
		/*for (Cell c : rightCells) {
			int x = c.getGridX() + 1;
			int y = c.getGridY();
			rightCells.remove(c);
			rightCells.add(new Cell(x, y));
		}*/
		ListIterator<Cell> iterator = rightCells.listIterator();
		while(iterator.hasNext()){
			Cell c= iterator.next();
			int x = c.getGridX() + 1;
			int y = c.getGridY();
			iterator.remove();
			iterator.add(new Cell(x, y));
		}
		return rightCells;
	}

	private static Region getRegionByCells(ArrayList<GridCell> cells) {
		// 确定label，cellList，pointNum
		String label = "R";
		LinkedList<GridCell> list = new LinkedList<GridCell>();
		int avgPointNum = cells.get(0).getPointNumInCell();// 每个cell内的点数一样多
		for (GridCell gc : cells) {
			int x = gc.getGridX();
			int y = gc.getGridY();
			label += "-" + StringUtil.padLeft("" + x, 2, '0')
					+ StringUtil.padLeft("" + y, 2, '0');
			list.add(gc);
		}
		Region region = new Region(label, avgPointNum, list);
		return region;
	}

	public static ArrayList<ArrayList<GPSPoint>> getTraInGrid(String userId,
			String lu_lng, String lu_lat, String rd_lng, String rd_lat) {
		ArrayList<ArrayList<GPSPoint>> traList = new ArrayList<ArrayList<GPSPoint>>();
		ArrayList<GPSPoint> pointList = new ArrayList<GPSPoint>();
		Connection conn = null;
		Statement stmt = null;
		/*String sql = "SELECT Long_gcj,Lat_gcj,DateTime,TraNum FROM location WHERE UserId='"
				+ userId
				+ "' AND Long_gcj>'"
				+ lu_lng
				+ "' AND Long_gcj<'"
				+ rd_lng
				+ "' AND Lat_gcj>'"
				+ rd_lat
				+ "' AND Lat_gcj<'"
				+ lu_lat
				+ "' AND TraNum in(01320080927120819,01320080927233805,01320081006232359) ORDER BY DateTime";*/
		String sql = "SELECT Long_gcj,Lat_gcj,DateTime,TraNum FROM location WHERE UserId='"
				+ userId
				+ "' AND Long_gcj>'"
				+ lu_lng
				+ "' AND Long_gcj<'"
				+ rd_lng
				+ "' AND Lat_gcj>'"
				+ rd_lat
				+ "' AND Lat_gcj<'"
				+ lu_lat
				+ "' ORDER BY DateTime";
		//01320080927120819,01320080927233805,01320081006232359
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			String traNumTmp = "0";
			while (rs.next()) {
				if (traNumTmp.equals(rs.getString("TraNum"))) {// 轨迹号相同，同一条轨迹
					GPSPoint point = new GPSPoint(rs.getString("Long_gcj"),
							rs.getString("Lat_gcj"), rs.getString("DateTime"));
					pointList.add(point);
				} else {// 轨迹号不同，不同轨迹
					if(pointList.size() < Parameter.leastPointNumInTra){
						pointList = new ArrayList<GPSPoint>();
						traNumTmp = rs.getString("TraNum");
						continue;
					}
					pointList = TraFilter.getSparsedTra(pointList);// 稀疏
					traList.add(pointList);
					pointList = new ArrayList<GPSPoint>();
					traNumTmp = rs.getString("TraNum");
				}
			}
			if(pointList.size() >= Parameter.leastPointNumInTra){
				traList.add(pointList);//加入最后一条
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JdbcUtil.close(conn, stmt);
		/*
		 * return new ArrayList<ArrayList<GPSPoint>>(traList.subList(1,
		 * traList.size()));
		 */
		return traList;
	}
	/**
	 * 将cell轨迹的list转为region轨迹的list
	 * @param cellTraList 
	 * @param regionList 通过分析所有轨迹得到的region的list
	 * @return 所有轨迹对应的regionList
	 */
	public static ArrayList<ArrayList<RegionTime>> cellTraList2RegionTraList(ArrayList<ArrayList<GPSCell>> cellTraList, LinkedList<Region> regionList){
		ArrayList<ArrayList<RegionTime>> regionTraList = new ArrayList<ArrayList<RegionTime>>();
		//建立cell=>region的映射，然后遍历所有cellTra
		HashMap<Cell, Region> cell2RegionMap = new HashMap<Cell, Region>();
		for(Region region:regionList){
			String label = region.getLabel();
			String[] subLabel = label.split("-");
			for(int i=1;i<subLabel.length;i++){
				String xy = subLabel[i];  
				String x = xy.substring(0, 2);
				String y = xy.substring(2, 4);
				Cell cell = new Cell(Integer.parseInt(x), Integer.parseInt(y));
				cell2RegionMap.put(cell, region);
			}
		}
		//遍历cellTraList
		for(ArrayList<GPSCell> cellList:cellTraList){
			ArrayList<RegionTime> regionTra = new ArrayList<RegionTime>();
			for(GPSCell gc:cellList){
				Cell cell = new Cell(gc.getGridX(), gc.getGridY());//位置
				String time = gc.getCellTime();//时间
				if(regionTra.size() == 0){//新轨迹
					Region region = cell2RegionMap.get(cell);
					if(region == null){//gc没有对应region
						continue;
					}else{//找到gc对应的region
						regionTra.add(new RegionTime(region, time));
					}
				}else{//已有region
					Region region = cell2RegionMap.get(cell);
					if(region == null){//gc没有对应region
						continue;
					}else{//找到gc对应的region
						if(regionTra.get(regionTra.size()-1).getRegion().equals(region)){//重复
							continue;
						}else{
							regionTra.add(new RegionTime(region, time));
						}
					}
				}
			}
			regionTraList.add(regionTra);
		}
		return regionTraList;
	}
	/**
	 * 根据所有的region和所有轨迹对应的regionTraList，得到该该用户的model map
	 * @param regionList 所有的region
	 * @param regionTraList 所有轨迹对应的regionTra组成的list
	 * @return
	 */
	public static HashMap<Region, ArrayList<RegionModel>> getModelMap(LinkedList<Region> regionList, ArrayList<ArrayList<RegionTime>> regionTraList){
		HashMap<Region, ArrayList<RegionModel>> map = new HashMap<Region, ArrayList<RegionModel>>();
		//初始化map，将所有的region映射到为空的ArrayList<RegionModel>
		for(Region region : regionList){
			map.put(region, new ArrayList<RegionModel>());
		}
		for(ArrayList<RegionTime> regionTra:regionTraList){
			for(int i=0;i<regionTra.size()-1;i++){//将轨迹中的最后一个舍弃
				if(i == 0){
					RegionModel regionModel = new RegionModel(null, regionTra.get(i+1).getRegion(), regionTra.get(i+1).getTime());
					ArrayList<RegionModel> arrayList = map.get(regionTra.get(i).getRegion());
					arrayList.add(regionModel);
					map.put(regionTra.get(i).getRegion(), arrayList);
				}else{
					RegionModel regionModel = new RegionModel(regionTra.get(i-1).getRegion(), regionTra.get(i+1).getRegion(), regionTra.get(i+1).getTime());
					ArrayList<RegionModel> arrayList = map.get(regionTra.get(i).getRegion());
					arrayList.add(regionModel);
					map.put(regionTra.get(i).getRegion(), arrayList);
				}
			}
		}
		return map;
	}
}
