$(function () {
    // 绑定上传事件
    $("#uploadBtn").bind("click", function () {
        const fileInput = $("#fileInput");
        if (fileInput.val() === '') {
            alert("哎哎哎,你文件呢？？(*￣︿￣)");
        } else {
            $.ajax({
                //请求方式
                type: "POST",
                processData: false,
                contentType: false,
                //请求地址
                url: "/task-chart/upload",
                //数据，json字符串
                data: new FormData($('#fileForm')[0]),
                //请求成功
                success: function (result) {
                    if (!result.flag) {
                        alert(result.msg);
                    } else {
                        renderChart(result);
                        $('#myTab a[href="#chart"]').tab('show');
                    }
                    $("#uploadBtn").disable();
                },
                //请求失败，包含具体的错误信息
                error: function (e) {
                    $("#uploadBtn").disable();
                    alert("出错啦！！ε(┬┬﹏┬┬)3");
                }
            });
            $("#uploadBtn").disabled();
        }
    });
});