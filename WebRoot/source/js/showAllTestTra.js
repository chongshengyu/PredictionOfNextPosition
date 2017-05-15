var map = new AMap.Map("container", {
	resizeEnable : true,
	zoom : 17,
	lang: 'en',
});

$.ajax({
	type : "GET",
	async : false,
	charset : "UTF-8",
	url : '/Paper/getAllTestTra',
	data : "",
	dataType : "json",
	error : function() {
		alert("失败");
	},
	success : function(data) {
		var out = data;
		for(var i=0;i<out.length;i++){
			var lineArr = [];
			for(var j=0;j<out[i].length;j++){
				lineArr[lineArr.length] = [out[i][j]['lang'],out[i][j]['lat']];
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
	}
});