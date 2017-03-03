package com.yu.draw;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yu.prepare.util.JdbcUtil;

public class getUserTras extends HttpServlet {

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
		String userId = request.getParameter("userId");
		Connection conn = null;
		Statement stmt = null;
		String sql = "";
		StringBuilder sb = new StringBuilder();
		try {
			conn = JdbcUtil.getConnection();
			stmt = conn.createStatement();
			sql = "SELECT TraNum FROM trajectory WHERE UserId='" + userId + "'";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				sb.append(rs.getString("TraNum") + ",");
			}
			JdbcUtil.close(conn, stmt);
			if (sb.length() > 0)
				sb = sb.deleteCharAt(sb.length() - 1);
//			System.out.println("请求" + userId + "的轨迹号，返回结果" + sb.toString());
			response.getWriter().write(sb.toString());
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JdbcUtil.close(conn, stmt);
	}

}
