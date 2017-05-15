package com.yu.prepare;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.yu.prepare.util.JdbcUtil;

public class GoodTra2Db {

	public static void main(String[] args) {
		//将筛选出的合适轨迹插入到goodtra表
		File goodDir = new File("F:\\Paper\\2017NextPosition\\实验\\GoodTraNum");
		File[] goodTraFiles = goodDir.listFiles();
		
		for(File file:goodTraFiles){
			if(file.isDirectory()){
				continue;
			}
			Connection conn = null;
			Statement stmt = null;
			BufferedReader reader = null;
			String line = "";
			String sql = "";
			try {
				conn = JdbcUtil.getConnection();
				stmt = conn.createStatement();
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				while((line = reader.readLine()) != null){
					String userId = line.substring(0, 3);
					String traNum = line;
					sql = "insert into goodtra(UserId, TraNum) values ('"+userId+"','"+traNum+"')";
					stmt.execute(sql.toString());
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally{
				if(reader != null){
					try {
						reader.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				JdbcUtil.close(conn, stmt);
			}
			System.out.println("结束一个");
		}
	}

}
