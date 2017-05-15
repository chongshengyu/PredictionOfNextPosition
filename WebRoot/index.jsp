<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <base href="<%=basePath%>">
    
    <title>导航</title>
	<meta http-equiv="pragma" content="no-cache">
	<meta http-equiv="cache-control" content="no-cache">
	<meta http-equiv="expires" content="0">    
	<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
	<meta http-equiv="description" content="This is my page">
	<!--
	<link rel="stylesheet" type="text/css" href="styles.css">
	-->
  </head>
  
  <body>
    <a href="predictionTest.html" target="_blank">准确性测试（训练集和测试集全部使用手工筛选的数据）</a></br>
    <a href="predictionTest2.html" target="_blank">准确性测试（训练集使用全部轨迹，测试集使用手工筛选的数据）</a></br>
    <a href="showTra.html" target="_blank">显示轨迹（纠偏，平滑）</a></br>
    <a href="showAllTestTra.html" target="_blank">显示全部测试用轨迹，未完报错</a></br>
    <a href="heatmap.html" target="_blank">热力图</a>
  </body>
</html>
