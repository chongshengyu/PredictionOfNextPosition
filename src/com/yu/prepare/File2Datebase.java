package com.yu.prepare;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

import com.yu.prepare.dao.DataAccess;
import com.yu.prepare.entity.GPSRecord;
import com.yu.prepare.entity.TraRecord;
import com.yu.prepare.util.GtmToLocal;

public class File2Datebase {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		ClearLogFile();
//		DateImport();
		MultiImport();
	}

	/**
	 * 输出至文件
	 * 
	 * @param s
	 * @throws IOException
	 */
	public static void LogToFile(String s) throws IOException {
		File file = new File("log.txt");
		BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
		writer.write(s + "\r\n");
		writer.close();
	}

	/**
	 * 清空输出文件
	 * 
	 * @throws IOException
	 */
	public static void ClearLogFile() throws IOException {
		File file = new File("log.txt");
		file.delete();
		file.createNewFile();
	}

	public static void MultiImport() {
		new Thread() {
			public void run() {
				DataAccess dataAccess = new DataAccess();
				for (int i = 60; i < 70; i++) {
					String userId = "0" + String.valueOf(i);
					try {
						SigleFileImport(userId,dataAccess);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				dataAccess.Close();
			}
		}.start();
		new Thread(){
			public void run() {
				DataAccess dataAccess = new DataAccess();
				for (int i = 70; i < 80; i++) {
					String userId = "0" + String.valueOf(i);
					try {
						SigleFileImport(userId,dataAccess);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				dataAccess.Close();
			};
		}.start();
		new Thread(){
			public void run() {
				DataAccess dataAccess = new DataAccess();
				for (int i = 80; i < 90; i++) {
					String userId = "0" + String.valueOf(i);
					try {
						SigleFileImport(userId,dataAccess);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				dataAccess.Close();
			};
		}.start();
	}

	/**
	 * 导入单个文件
	 * 
	 * @param userId
	 *            某个文件的文件名，如000,001等
	 * @param dataAccess
	 * @throws IOException
	 */
	public static void SigleFileImport(String userId, DataAccess dataAccess)
			throws IOException {
		String userTraRootDir = "F:\\Paper\\GPS\\Geolife Trajectories 1.3\\Data\\"
				+ userId + "\\Trajectory";
		String traNum = "";// 轨迹号
		String dataLine = "";
		String[] datas = {};
		String latitude = "";
		String longitude = "";
		String altitude = "";
		String dateTime = "";
		GPSRecord gpsRecord;
		TraRecord traRecord;
		int filesNum = 0;
		System.out.println("新用户开始：" + userId);
//		LogToFile("新用户开始：" + userId);
		File userTraRoot = new File(userTraRootDir);
		for (File userTra : userTraRoot.listFiles()) {// 循环该用户的每个文件
			filesNum++;
			traNum = userId + userTra.getName().split("\\.")[0];// 轨迹号
			System.out.println(userId + "第" + filesNum + "个文件开始：" + traNum);
//			LogToFile(userId + "第" + filesNum + "个文件开始：" + traNum);
			BufferedReader bufferedReader = new BufferedReader(new FileReader(
					userTra));
			for (int i = 0; i < 6; i++) {// 跳过文件说明行
				bufferedReader.readLine();
			}
			String startTime = "";
			String endTime = "";
			int pointNum = 0;
			while ((dataLine = bufferedReader.readLine()) != null) {
				pointNum++;
				datas = dataLine.split(",");
				longitude = datas[1];
				latitude = datas[0];
				altitude = datas[2];
				dateTime = datas[5] + " " + datas[6];
				dateTime = GtmToLocal.transform(dateTime);
				if (startTime.equals("")) {
					startTime = dateTime;
				}
				endTime = dateTime;
				gpsRecord = new GPSRecord(userId, longitude, latitude,
						dateTime, traNum);
				dataAccess.InsertLocation(gpsRecord);// 插入数据库
			}
			traRecord = new TraRecord(userId, traNum, startTime, endTime,
					pointNum);
			dataAccess.InsertTra(traRecord);// 插入数据库
			bufferedReader.close();
		}
		System.out.println("用户" + userId + "结束");
//		LogToFile("用户" + userId + "结束");
	}

	public static void DateImport() throws IOException {
		/*
		 * String dataSetPath =
		 * "F:\\Paper\\GPS\\Geolife Trajectories 1.3\\Data"; File rootDir = new
		 * File(dataSetPath); File[] userDirs = rootDir.listFiles(); String
		 * userId = "";// 用户Id String traNum = "";// 轨迹号 String userTraRootDir =
		 * "";// 用户轨迹根目录 String dataLine = ""; String[] datas = {}; String
		 * latitude = ""; String longitude = ""; String altitude = ""; String
		 * dateTime = ""; GPSRecord gpsRecord; TraRecord traRecord;
		 * ClearLogFile(); for (File userDir : userDirs) {// 循环每个用户目录 int
		 * filesNum = 0; userId = userDir.getName(); LogToFile("用户ID" + userId);
		 * System.out.println("新用户开始：" + userId); userTraRootDir =
		 * userDir.getAbsolutePath() + "\\Trajectory"; File userTraRoot = new
		 * File(userTraRootDir); for (File userTra : userTraRoot.listFiles())
		 * {// 循环该用户的每个文件 filesNum++; traNum = userId +
		 * userTra.getName().split("\\.")[0];// 轨迹号 LogToFile("轨迹号" + traNum);
		 * System.out.print(userId + "第" + filesNum + "个文件开始：" + traNum);
		 * BufferedReader bufferedReader = new BufferedReader( new
		 * FileReader(userTra)); for (int i = 0; i < 6; i++) {// 跳过文件说明行
		 * bufferedReader.readLine(); } String startTime = ""; String endTime =
		 * ""; int pointNum = 0; while ((dataLine = bufferedReader.readLine())
		 * != null) { pointNum++; datas = dataLine.split(","); longitude =
		 * datas[1]; latitude = datas[0]; altitude = datas[2]; dateTime =
		 * datas[5] + " " + datas[6]; dateTime = GtmToLocal.transform(dateTime);
		 * if (startTime.equals("")) { startTime = dateTime; } endTime =
		 * dateTime; gpsRecord = new GPSRecord(userId, longitude, latitude,
		 * dateTime, traNum); DataAccess.InsertLocation(gpsRecord);// 插入数据库 }
		 * traRecord = new TraRecord(userId, traNum, startTime, endTime,
		 * pointNum); DataAccess.InsertTra(traRecord);// 插入数据库
		 * bufferedReader.close(); System.out.println("文件结束"); }
		 * LogToFile("---------------------"); System.out.println("用户" + userId
		 * + "结束"); // break;// 仅第一个用户 } DataAccess.Close();
		 */
	}

	@Test
	public void test() throws ParseException {
		Date out = new Date();
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		out = format.parse("2008-09-23 02:53:04");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String demo = sdf.format(out);
		System.out.println(demo);
	}

}
