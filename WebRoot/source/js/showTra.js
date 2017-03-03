var map = new AMap.Map("container", {
	resizeEnable : true,
	zoom : 17,
});

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
// useId下拉框变化的响应，填充traNum下拉框
function useridOnchange() {
	$('#dropTid').empty();
	var userid = $('#dropUid').val();
	$.ajax({
		type : "GET",
		async : false,
		charset : "UTF-8",
		url : '/Paper/getUserTras',
		data : "userId=" + userid + "",
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
//set fit view
function setFitViewOnClick(){
	map.setFitView();
}
//输出轨迹号
function outputTraNo(){
	console.log($('#dropTid').val());
}
// 点击确定的响应
function submitOnClick() {
	// 获取纠偏后的位置点，或者纠偏前后的位置点
	map.clearMap();
	var userid = $('#dropUid').val();
	var traid = $('#dropTid').val();
	var corrected = 0;
	var uncorrected = 0;
	var correctedAll = 0;
	if($('#chkAllCorrected').is(':checked')){
		correctedAll = 1;
	}
	if($('#chkUncorrected').is(':checked')){
		uncorrected = 1;
	}
	if($('#chkCorrected').is(':checked')){
		corrected = 1;
	}
	$.ajax({
		type : "GET",
		async : false,
		charset : "UTF-8",
		url : '/Paper/getTraPoints',
		data : "correctedAll="+correctedAll+"&corrected="+corrected+"&uncorrected="+uncorrected+"&userid="+userid+"&traid="+traid,// corrected:纠偏后的数据，uncorrected：纠偏前的数据
		dataType : "json",
		error : function() {
			alert("失败");
		},
		success : function(data) {
			if(correctedAll == 1){//纠偏后的所有轨迹
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
			}else {
				if(corrected == 1){//指定的纠偏后的某一条轨迹
					var lineArr = [];
					var out = data;
					for(var i=0;i<out[1].length;i++){
						lineArr[lineArr.length] = [out[1][i]['lang'],out[1][i]['lat']];
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
				if(uncorrected == 1){//指定的纠偏前的某一条轨迹
					var lineArr = [];
					var out = data;
					for(var i=0;i<out[0].length;i++){
						lineArr[lineArr.length] = [out[0][i]['lang'],out[0][i]['lat']];
					}
				    var polyline = new AMap.Polyline({
				        path: lineArr,          //设置线覆盖物路径
				        strokeColor: "#F70909", //线颜色
				        strokeOpacity: 1,       //线透明度
				        strokeWeight: 5,        //线宽
				        strokeStyle: "solid",   //线样式
				        strokeDasharray: [10, 5] //补充线样式
				    });
				    polyline.setMap(map);
				}
			}
			
		    map.setFitView();
		}
	});
}