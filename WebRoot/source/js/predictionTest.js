var map = new AMap.Map("container", {
	resizeEnable : true,
	zoom : 17,
});

// 填充useId下拉框
$.ajax({
	type : "GET",
	async : false,
	charset : "UTF-8",
	url : '/Paper/EvaAlgoriTestServlet',
	data : "type=getUserId",
	dataType : "text",
	error : function() {
		alert("失败");
	},
	success : function(data) {
		var userids = data.split(',');// userid数组
		// 获取第一个用户的所有轨迹号
		$.ajax({
			type : "GET",
			async : true,
			charset : "UTF-8",
			url : '/Paper/EvaAlgoriTestServlet',
			data : "type=userTestTraNo&userId="+userids[0],
			dataType : "text",
			error : function() {
				alert("失败");
			},
			success : function(data) {
				var userTras = data.split(',');
				for (var i = 0; i < userTras.length; i++) {
					$('#dropTid').append(
							"<option value='" + userTras[i] + "'>"
									+ userTras[i] + "</option>");
				}
			}
		});
		// 填充userId列表
		for (var i = 0; i < userids.length; i++) {
			$('#dropUid').append(
					"<option value='" + userids[i] + "'>" + userids[i]
							+ "</option>");
		}

	}
});

// useId下拉框变化的响应，填充traNum下拉框
function useridOnchange() {
	$('#dropTid').empty();
	var userid = $('#dropUid').val();
	$.ajax({
		type : "GET",
		async : false,
		charset : "UTF-8",
		url : '/Paper/EvaAlgoriTestServlet',
		data : "type=userTestTraNo&userId="+userid,
		dataType : "text",
		error : function() {
			alert("失败");
		},
		success : function(data) {
			var userTras = data.split(',');
			for (var i = 0; i < userTras.length; i++) {
				$('#dropTid').append(
						"<option value='" + userTras[i] + "'>" + userTras[i]
								+ "</option>");
			}
		}
	});
}
// 下一条轨迹
function nextOnClick(){
	var nextTid = $("#dropTid option:selected").next().val();
	$("#dropTid").find("option[value='"+nextTid+"']").attr("selected",true);
	submitOnClick();
}

// 点击显示实际轨迹的响应，画出轨迹
function submitOnClick() {
	map.clearMap();
	var userId = $('#dropUid').val();
	var traNo = $('#dropTid').val();
	$.ajax({
		type : "GET",
		async : false,
		charset : "UTF-8",
		url : '/Paper/EvaAlgoriTestServlet',
		data : "type=getTraPoints&userId="+userId+"&traNo="+traNo,
		dataType : "json",
		error : function() {
			alert("失败");
		},
		success : function(data) {
			var lineArr = [];
			var out = data;
			//起点
			lineArr[lineArr.length] = [out[0]['lang'],out[0]['lat']];
			var marker = new AMap.Marker({
	            icon: "source/img/start.png",
	            position: [out[0]['lang'], out[0]['lat']],
				offset:new AMap.Pixel(-16, -16),
				title:out[0]['dateTime'],
				extData:out[0]['dateTime'],
	        });
	        marker.setMap(map);
			//非起始点
			for(var i=1;i<out.length-1;i++){
				lineArr[lineArr.length] = [out[i]['lang'],out[i]['lat']];
				var marker = new AMap.Marker({
		            icon: "source/img/circle.png",
		            position: [out[i]['lang'], out[i]['lat']],
					offset:new AMap.Pixel(-3, -3),
					title:out[i]['dateTime'],
					extData:out[i]['dateTime'],
		        });
		        marker.setMap(map);
		        marker.on('click',function(e){
		        	//预测
		        	var userId = $('#dropUid').val();
		        	var lng = e.lnglat.getLng();
		        	var lat = e.lnglat.getLat();
		        	var time = e.target.getExtData();
		        	// 画出网格
		        	$.ajax({
		        		type : "GET",
		        		async : false,
		        		charset : "UTF-8",
		        		url : '/Paper/EvaAlgoriTestServlet',
		        		data : 'type=getRegion&testTraNo='+$('#dropTid').val()+'&userId=' + userId + "&lng=" + lng + "&lat=" + lat,
		        		dataType : "json",
		        		error : function() {
		        			alert("失败");
		        		},
		        		success : function(data) {
		        			for (var i = 1; i < data.length; i++) {//从1开始，不画grid
		        				var zoneStr = data[i]['recZoneStr'];
		        				var pointslnglat = zoneStr.split(',');
		        				var polygonArr = new Array();// 多边形覆盖物节点坐标数组
		        				polygonArr.push([ pointslnglat[0], pointslnglat[1] ]);
		        				polygonArr.push([ pointslnglat[2], pointslnglat[3] ]);
		        				polygonArr.push([ pointslnglat[4], pointslnglat[5] ]);
		        				polygonArr.push([ pointslnglat[6], pointslnglat[7] ]);
		        				var polygon = new AMap.Polygon({
		        					path : polygonArr,// 设置多边形边界路径
		        					strokeColor : "#000000", // 线颜色
		        					strokeOpacity : 1, // 线透明度
		        					strokeWeight : 1, // 线宽
		        					fillColor : "#55BBFF", // 填充色
		        					fillOpacity : 0.35
		        				// 填充透明度
		        				});
		        				polygon.setMap(map);
		        			}
		        		}
		        	});
		        	//第二步：显示分值getScore
		        	$.ajax({
		        		type : "GET",
		        		async : false,
		        		charset : "UTF-8",
		        		url : '/Paper/EvaAlgoriTestServlet',
		        		data : 'type=getScore&userId=' + userId + "&time=" + time,//后台还要从session中获取其他信息，如中心点，regionList等。
		        		dataType : "json",
		        		error : function() {
		        			alert("失败");
		        		},
		        		success : function(data) {
		        			if(data.length == 0){
		        				alert("scoreMap空");
		        				return;
		        			}
		        			for (var i = 0; i < data.length; i++) {
		        				var score = data[i]['score'];
		        				var zoneStr = data[i]['recZoneStr'];
		        				//计算Region中心点
		        				var pointslnglat = zoneStr.split(',');
		        				var lu_lng = pointslnglat[0];
		        				var lu_lat = pointslnglat[1];
		        				var rd_lng = pointslnglat[4];
		        				var rd_lat = pointslnglat[5];
		        				var center_lng = (Number(lu_lng) + Number(rd_lng))/2;
		        				var center_lat = (Number(lu_lat) + Number(rd_lat))/2;
		        				//标注
		        				var marker = new AMap.Marker({
		        					icon : new AMap.Icon({
		        						size:new AMap.Size(1,1),
		        						image:"source/img/score.png",
		        					}),
		        					position : [center_lng,center_lat],
		        					offset:new AMap.Pixel(-6, -6),
		        				});
		        			    marker.setLabel({//label默认蓝框白底左上角显示，样式className为：amap-marker-label
		        			        offset: new AMap.Pixel(-2, 0),//修改label相对于maker的位置
		        			        content: score
		        			    });
		        			    marker.setMap(map);
		        			}
		        		}
		        	});
		        })
			}
			//终点
	        lineArr[lineArr.length] = [out[out.length-1]['lang'],out[out.length-1]['lat']];
			var marker = new AMap.Marker({
	            icon: "source/img/end.png",
	            position: [out[out.length-1]['lang'], out[out.length-1]['lat']],
				offset:new AMap.Pixel(-16, -16),
				title:out[out.length-1]['dateTime'],
				extData:out[out.length-1]['dateTime'],
	        });
	        marker.setMap(map);
	        //画线
		    var polyline = new AMap.Polyline({
		        path: lineArr,          //设置线覆盖物路径
		        strokeColor: "#3366FF", //线颜色
		        strokeOpacity: 1,       //线透明度
		        strokeWeight: 5,        //线宽
		        strokeStyle: "solid",   //线样式
		        strokeDasharray: [10, 5] //补充线样式
		    });
		    polyline.setMap(map);
		    map.setFitView();
		}
	});
}