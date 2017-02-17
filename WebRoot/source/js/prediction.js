var map = new AMap.Map("container", {
	resizeEnable : true,
	zoom : 14,
	center : [ 116.353888, 39.967768 ],
});

// 地图点击事件
map.on('click', function(e) {
});

$('#datetimepicker').datetimepicker({
	format : 'yyyy-mm-dd hh:ii:ss',
	minView : 0,
	autoclose : true,
});

var hisMarkNum = 0;// 记录当前已点击设置的历史位置数
var hasGetRegion = false;// 标记是否获取并画出的region，点击地图标注marker时使用
var markerPositions;// 记录所有marker的位置，格式：lng,lat;lng,lat;lng,lat;

// 填充useId下拉框
$.ajax({
	type : "GET",
	async : false,
	charset : "UTF-8",
	url : '/Paper/getUserIds',
	data : "",
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
			url : '/Paper/getUserTras',
			data : "userId=" + userids[0] + "",
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
// 用户id切换
function dropUidChg() {
	map.clearMap();
	hasGetRegion = false;
	markerPositions = '';
	hisMarkNum = 0;
}
// 请求用户Region
function buttonGetRegion() {
	// 用户id
	var userId = $('#dropUid').val();
	// 地图中心点
	var lng = map.getCenter().getLng();
	var lat = map.getCenter().getLat();
	// 画出网格
	$.ajax({
		type : "GET",
		async : false,
		charset : "UTF-8",
		url : '/Paper/predictionServlet',
		data : 'type=getRegion&userId=' + userId + "&lng=" + lng
				+ "&lat=" + lat,
		dataType : "json",
		error : function() {
			alert("失败");
		},
		success : function(data) {
			for (var i = 0; i < data.length; i++) {
				var zoneStr = data[i]['recZoneStr'];
				var pointslnglat = zoneStr.split(',');
				var polygonArr = new Array();// 多边形覆盖物节点坐标数组
				polygonArr.push([ pointslnglat[0], pointslnglat[1] ]);
				polygonArr.push([ pointslnglat[2], pointslnglat[3] ]);
				polygonArr.push([ pointslnglat[4], pointslnglat[5] ]);
				polygonArr.push([ pointslnglat[6], pointslnglat[7] ]);
				var polygon = new AMap.Polygon({
					path : polygonArr,// 设置多边形边界路径
					strokeColor : "#FF33FF", // 线颜色
					strokeOpacity : 0.2, // 线透明度
					strokeWeight : 2, // 线宽
					fillColor : "#C2D8F8", // 填充色
					fillOpacity : 0.35
				// 填充透明度
				});
				polygon.on("click",function(e) {// 区域点击响应
					if (hasGetRegion) {
						var lng = e.lnglat.getLng();
						var lat = e.lnglat.getLat();
						var positionNumSet = $('#dropPointNum').val();// 已知位置点数，可能是1,2,3
						if (hisMarkNum >= positionNumSet) {// 已经设置完成

						} else {
							if (hisMarkNum == 0) {// 设置第1个
								hisMarkNum = hisMarkNum + 1;
								markerPositions = markerPositions + lng +","+lat+";";
								var marker = new AMap.Marker({
									icon : "source/img/mark_1.png",
									position : [lng,lat]
								});
								marker.setMap(map);
							} else if (hisMarkNum == 1) {// 设置第2个
								hisMarkNum = hisMarkNum + 1;
								markerPositions = markerPositions + lng +","+lat+";";
								var marker = new AMap.Marker({
									icon : "source/img/mark_2.png",
									position : [lng,lat]
								});
								marker.setMap(map);
							} else if (hisMarkNum == 2) {// 设置第3个
								hisMarkNum = hisMarkNum + 1;
								markerPositions = markerPositions + lng +","+lat+";";
								var marker = new AMap.Marker({
									icon : "source/img/mark_3.png",
									position : [lng,lat]
								});
								marker.setMap(map);
							}
						}
					}
				});
				polygon.setMap(map);
			}
			hasGetRegion = true;
		}
	});
}

// 预测
function buttonPredictionClick() {
	// 判断region是否已获取
	// 判断点是否都已选择
	// 判断时间是否已选择
	if(!hasGetRegion){
		alert("请先计算Region！");
		return;
	}
	if(hisMarkNum < $('#dropPointNum').val()){
		alert("请先完成位置点选择！");
		return;
	}
	if($('#datetimepicker').val()==""){
		alert("请先选择时间！");
		return;
	}
	var userId = $('#dropUid').val();
	var time = $('#datetimepicker').val();
	$.ajax({
		type : "GET",
		async : false,
		charset : "UTF-8",
		url : '/Paper/predictionServlet',
		data : 'type=prediction&userId=' + userId + "&time=" + time + "&markerPositions=" + markerPositions,
		dataType : "json",
		error : function() {
			alert("失败");
		},
		success : function(data) {
		}
	});
}
