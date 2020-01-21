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
    $("#queryBtn").click();
});