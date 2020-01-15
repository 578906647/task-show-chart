function renderChart(data) {
    var barChartData = {};
    barChartData.labels = data.publishPatchList;
    var datasets = [];
    var colorIndex = 0;
    for (var pro in data.dataMap) {
        var dataset = {};
        dataset.label = pro;
        dataset.data = data.dataMap[pro];
        dataset.backgroundColor = colorArr[colorIndex];
        colorIndex++;
        datasets.push(dataset);
    }
    barChartData.datasets = datasets;
    $("#canvas").remove();
    $("#canvasDiv").append("<canvas id=\"canvas\"></canvas>");
    var c = document.getElementById('canvas');
    var ctx = c.getContext('2d');

    window.myBar = new Chart(ctx, {
        type: 'bar',
        data: barChartData,
        options: {
            title: {
                display: true,
                text: '任务单分析图'
            },
            tooltips: {
                mode: 'index',
                intersect: false
            },
            responsive: true,
            scales: {
                xAxes: [{
                    stacked: true,
                }],
                yAxes: [{
                    stacked: true
                }]
            },
            hover: {
                animationDuration: 0  // 防止鼠标移上去，数字闪烁
            },
            animation: {           // 这部分是数值显示的功能实现
                onComplete: function () {
                    var chartInstance = this.chart,

                        ctx = chartInstance.ctx;
                    // 以下属于canvas的属性（font、fillStyle、textAlign...）
                    ctx.font = Chart.helpers.fontString(Chart.defaults.global.defaultFontSize, Chart.defaults.global.defaultFontStyle, Chart.defaults.global.defaultFontFamily);
                    ctx.fillStyle = "black";
                    ctx.textAlign = 'top';
                    ctx.textBaseline = 'bottom';

                    var xLength = this.data.datasets.length;
                    var numArr = [];
                    this.data.datasets.forEach(function (dataset, i) {
                        var meta = chartInstance.controller.getDatasetMeta(i);
                        var num = 0;
                        meta.data.forEach(function (bar, index) {
                            numArr[index] = numArr[index] == null ? 0 + dataset.data[index] : numArr[index] + dataset.data[index];
                            if (i == xLength - 1) {
                                ctx.fillText(numArr[index], bar._model.x - 2, bar._model.y - 5);
                            }
                        });
                    });
                }
            }
        }
    });
};

var colorArr = ['rgb(255, 99, 132)', 'rgb(255, 159, 64)', 'rgb(255, 205, 86)',
    'rgb(54, 162, 235)', 'rgb(153, 102, 255)', 'rgb(201, 203, 207)', 'rgb(0, 0, 128)',
    'rgb(0, 0, 0)', 'rgb(69, 139, 116)', 'rgb(0, 245, 255)', 'rgb(255, 222, 173)',
    'rgb(75, 192, 192)', 'rgb(82, 139, 139)', 'rgb(47, 79, 79)', 'rgb(119, 136, 153)'
    , 'rgb(105, 139, 34)'];

(function () {

    if (document.location.hostname.match(/^(www\.)?chartjs\.org$/)) {
        (function (i, s, o, g, r, a, m) {
            i['GoogleAnalyticsObject'] = r;
            i[r] = i[r] || function () {
                (i[r].q = i[r].q || []).push(arguments)
            }, i[r].l = 1 * new Date();
            a = s.createElement(o),
                m = s.getElementsByTagName(o)[0];
            a.async = 1;
            a.src = g;
            m.parentNode.insertBefore(a, m)
        })(window, document, 'script', '//www.google-analytics.com/analytics.js', 'ga');
        ga('create', 'UA-28909194-3', 'auto');
        ga('send', 'pageview');
    }
    /* eslint-enable */

}(this));