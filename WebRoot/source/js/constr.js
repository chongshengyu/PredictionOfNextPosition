var map = new AMap.Map("container", {
	resizeEnable : true,
	zoom : 14,
    center: [116.379995, 39.968436],
//    lang:'en'
});
var ee;//调用传参
var infoWindow = new AMap.InfoWindow({
    offset: new AMap.Pixel(0, -20)
});
function markerClick(e) {
    infoWindow.setContent(e.target.content);
    infoWindow.open(map, e.target.getPosition());
}

//填充useId下拉框
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
		// 填充userId列表
		for (var i = 0; i < userids.length; i++) {
			$('#dropUid').append(
					"<option value='" + userids[i] + "'>" + userids[i]
							+ "</option>");
		}

	}
});

//清空地图
function clearOnClick(){
	map.clearMap();
}

//画出网格
function drawGrid(e){
	var userid = $('#dropUid').val();
	var lng = e.lnglat.getLng();
	var lat = e.lnglat.getLat();
	$('#txt_lng').val(lng);
	$('#txt_lat').val(lat);
	//将中心点发至后台，确定网格
	$.ajax({
		type : "GET",
		async : false,
		charset : "UTF-8",
		url : '/Paper/getGrid',
		data : "userid="+userid+"&lng="+lng+"&lat="+lat,
		dataType : "json",
		error : function() {
			alert("失败");
		},
		success : function(data) {
			for(var i=0;i<data.length;i++){
				var zoneStr = data[i]['recZoneStr'];
				var pointslnglat = zoneStr.split(',');
				var polygonArr = new Array();//多边形覆盖物节点坐标数组
			    polygonArr.push([pointslnglat[0], pointslnglat[1]]);
			    polygonArr.push([pointslnglat[2], pointslnglat[3]]);
			    polygonArr.push([pointslnglat[4], pointslnglat[5]]);
			    polygonArr.push([pointslnglat[6], pointslnglat[7]]);
			    var  polygon = new AMap.Polygon({
			        path: polygonArr,//设置多边形边界路径
			        strokeColor: "#000000", //线颜色
			        strokeOpacity: 1, //线透明度
			        strokeWeight: 1,    //线宽
			        fillColor: "#55BBFF", //填充色
			        fillOpacity: 0.35//填充透明度
			    });
			    polygon.setMap(map);
			}
		}
	});
}
//点击地图
map.on('click', function(e) {
	ee = e;
	drawGrid(ee);
});
//获取相关轨迹号
function getTraNums(){
	if($('#txt_lng').val()==""){
		alert("先点击地图以确定区域");
		return ;
	}
	var userid = $('#dropUid').val();
	var lng = $('#txt_lng').val();
	var lat = $('#txt_lat').val();
	//获取相关轨迹号
	$.ajax({
		type : "GET",
		async : false,
		charset : "UTF-8",
		url : '/Paper/getTraInZone',
		data : "userid="+userid+"&lng="+lng+"&lat="+lat+"&type=getTraNums",
		dataType : "text",
		error : function() {
			alert("失败");
		},
		success : function(data) {
			var traNums = data.split(',');
			for(var i=0;i<traNums.length;i++){
				$('#dropTid').append(
						"<option value='" + traNums[i] + "'>" + traNums[i]
								+ "</option>");
			}
		}
	});
}
//显示当前选择的轨迹
function showTra(){
	map.clearMap();
	drawGrid(ee);//画网格
	if($('#txt_lng').val()==""){
		alert("先点击地图以确定区域");
		return ;
	}
	if($('#dropTid').val()=="" || $('#dropTid').val()==null){
		alert("先选择轨迹");
		return ;
	}
	var userid = $('#dropUid').val();
	var traid = $('#dropTid').val();
	var lng = $('#txt_lng').val();
	var lat = $('#txt_lat').val();
	$.ajax({
		type : "GET",
		async : false,
		charset : "UTF-8",
		url : '/Paper/getTraInZone',
		data : "userid="+userid+"&traNum="+traid+"&lng="+lng+"&lat="+lat+"&type=showTra",
		dataType : "json",
		error : function() {
			alert("失败");
		},
		success : function(data) {
			var lineArr = [];
			for(var i=0;i<data.length;i++){
				//line
				lineArr[lineArr.length] = [data[i]['lang'],data[i]['lat']];
				//marker
			    var marker = new AMap.Marker({ //添加自定义点标记
			        map: map,
			        position: [data[i]['lang'], data[i]['lat']], //基点位置
			        icon: new AMap.Icon({            
			            size: new AMap.Size(6, 6),  //图标大小
			            image: "source/img/circle.png",
			        }),
			        offset: new AMap.Pixel( - 3, -3),
			    });
			    marker.setMap(map);
			    marker.content = data[i]['dateTime'];
			    marker.on('mouseover', markerClick);
			}
			var polyline = new AMap.Polyline({
		        path: lineArr,          //设置线覆盖物路径
		        strokeColor: "#3366FF", //线颜色
		        strokeOpacity: 1,       //线透明度
		        strokeWeight: 5,        //线宽
		        strokeStyle: "solid",   //线样式
		        strokeDasharray: [10, 5] //补充线样式
		    });
		    polyline.setMap(map);
		}
	});
}
//显示下一条轨迹
function showNextTra(){
	map.clearMap();
	drawGrid(ee);
	var traid = $("#dropTid option:selected").next().val();
	$("#dropTid").find("option[value='"+traid+"']").attr("selected",true);
	var userid = $('#dropUid').val();
	var lng = $('#txt_lng').val();
	var lat = $('#txt_lat').val();
	$.ajax({
		type : "GET",
		async : false,
		charset : "UTF-8",
		url : '/Paper/getTraInZone',
		data : "userid="+userid+"&traNum="+traid+"&lng="+lng+"&lat="+lat+"&type=showTra",
		dataType : "json",
		error : function() {
			alert("失败");
		},
		success : function(data) {
			var lineArr = [];
			for(var i=0;i<data.length;i++){
				//line
				lineArr[lineArr.length] = [data[i]['lang'],data[i]['lat']];
				//marker
				var marker = new AMap.Marker({ //添加自定义点标记
			        map: map,
			        position: [data[i]['lang'], data[i]['lat']], //基点位置
			        icon: new AMap.Icon({            
			            size: new AMap.Size(6, 6),  //图标大小
			            image: "source/img/circle.png",
			        }),
			        offset: new AMap.Pixel( - 3, -3),
			    });
			    marker.setMap(map);
			    marker.content = data[i]['dateTime'];
			    marker.on('mouseover', markerClick);
			}
			var polyline = new AMap.Polyline({
		        path: lineArr,          //设置线覆盖物路径
		        strokeColor: "#3366FF", //线颜色
		        strokeOpacity: 1,       //线透明度
		        strokeWeight: 5,        //线宽
		        strokeStyle: "solid",   //线样式
		        strokeDasharray: [10, 5] //补充线样式
		    });
		    polyline.setMap(map);
		}
	});
}
//显示所有轨迹
function showAllTra(){
	if($('#txt_lng').val()==""){
		alert("先点击地图以确定区域");
		return ;
	}
	var userid = $('#dropUid').val();
	//获取区域内轨迹
	$.ajax({
		type : "GET",
		async : false,
		charset : "UTF-8",
		url : '/Paper/getTraInZone',
		data : "userid="+userid+"&type=showAllTra",
		dataType : "json",
		error : function() {
			alert("失败");
		},
		success : function(data) {
			/*for(var i=0;i<data.length;i++){
				var lineArr = [];
				for(var j=0;j<data[i].length;j++){
					lineArr[lineArr.length] = [data[i][j]['lang'],data[i][j]['lat']];
				}
				var polyline = new AMap.Polyline({
			        path: lineArr,          //设置线覆盖物路径
			        strokeColor: "#3366FF", //线颜色
			        strokeOpacity: 1,       //线透明度
			        strokeWeight: 5,        //线宽
			        strokeStyle: "solid",   //线样式
			        strokeDasharray: [10, 5] //补充线样式
			    });
			    polyline.setMap(map);
			}*/
		}
	});
}