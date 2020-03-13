$(function () {
    //初始化日期控件
    $('#datetimeDiv').datetimepicker({
        format: 'YYYY-MM-DD',
        locale: moment.locale('zh-cn'),
        defaultDate: new Date()
    });
    // 绑定查询事件
    $("#queryBtn").bind("click", function () {
        const date = $("#dateInput").val();
        if (date === '') {
            alert("日期没有选择！！(*￣︿￣)");
        } else {
            $.ajax({
                //请求方式
                type: "GET",
                //请求地址
                url: "/task-chart/parse",
                //数据，json字符串
                data: {"date": date, "dateType": $('input:radio:checked').val()},
                //请求成功
                success: function (result) {
                    if (!result.flag) {
                        alert(result.msg);
                    } else {
                        renderChart(result);
                    }
                },
                //请求失败，包含具体的错误信息
                error: function (e) {
                    alert("出错啦！！ε(┬┬﹏┬┬)3");
                }
            });
        }
    });
    // 绑定上传事件
    $("#uploadBtn").bind("click", function () {
        if ($("#fileInput").val() === "") {
            alert("请选择文件！！(*￣︿￣)");
        } else {
            var formData = new FormData($("#uploadForm")[0]);
            $.ajax({
                //请求方式
                type: "POST",
                //请求地址
                url: "/task-chart/upload",
                data: formData,
                processData: false,//必填 必须false 才会避开jq对formdata的默认处理 XMLHttpRequest才会对formdata进行正确处理  否则会报Illegal invocation错误
                contentType: false,//必填 必须false 才会加上正确的Content-Typ
                success: function (result) {
                    if (!result.flag) {
                        alert(result.msg);
                    } else {
                        showChartView();
                    }
                },
                //请求失败，包含具体的错误信息
                error: function (e) {
                    alert("出错啦！！ε(┬┬﹏┬┬)3");
                }
            });
        }
    });
    // 绑定上传事件
    $("#uploadAndPushBtn").bind("click", function () {
        if ($("#fileInput").val() === "") {
            alert("请选择文件！！(*￣︿￣)");
        } else {
            var formData = new FormData($("#uploadForm")[0]);
            $.ajax({
                //请求方式
                type: "POST",
                //请求地址
                url: "/task-chart/uploadAndPush",
                data: formData,
                processData: false,//必填 必须false 才会避开jq对formdata的默认处理 XMLHttpRequest才会对formdata进行正确处理  否则会报Illegal invocation错误
                contentType: false,//必填 必须false 才会加上正确的Content-Typ
                success: function (result) {
                    if (!result.flag) {
                        alert(result.msg);
                    } else {
                        showChartView();
                    }
                },
                //请求失败，包含具体的错误信息
                error: function (e) {
                    alert("出错啦！！ε(┬┬﹏┬┬)3");
                }
            });
        }
    });

    function showChartView() {
        $("#uploadContent").removeClass("in active");
        $("#chartContent").addClass("in active");
        $("#queryBtn").click();
    }

    var paraString = location.search;
    if (paraString !== "") {
        showChartView();
    }
});