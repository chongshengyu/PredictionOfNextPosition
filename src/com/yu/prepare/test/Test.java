package com.yu.prepare.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


import com.yu.prepare.util.JdbcUtil;


public class Test {

	@org.junit.Test
	public void test(){
		String s = "2008-11-19 13:27:12";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try {
			date = simpleDateFormat.parse(s);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTime(date);
		System.out.println(calendar.getTimeInMillis());
	}
	@org.junit.Test
	public void test2(){
		Connection conn = null;
		Statement stmt = null;
		String sql = "";
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			sql = "INSERT INTO dept(id,deptName) VALUES(4,'4')";
			stmt.execute(sql);
			sql = "INSERT INTO dept(id,deptName) VALUES(5,'5')";
			stmt.execute(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("error:" + sql);
			throw new RuntimeException(e);
		} finally {
			JdbcUtil.close(conn, stmt);
		}
	}
}
