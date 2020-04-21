package com.taskshowchart.service.impl;

import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.taskshowchart.dto.RespDto;
import com.taskshowchart.dto.TaskDto;
import com.taskshowchart.service.DingTalkService;
import com.taskshowchart.service.FileService;
import com.taskshowchart.util.ExcelUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description: 任务实现类
 *
 * @author bai.wenlong
 * @date 2020/1/20 13:14
 */
@Service
public class DingTalkServiceImpl implements DingTalkService {

    @Autowired
    private ExcelUtils excelUtils;

    /**
     * 机器人地址
     */
    @Value("${dd.talk.push.url}")
    private String serverUrl;

    /**
     * 加签
     */
    @Value("${dd.talk.push.secret}")
    private String secret;

    /**
     * 特殊@的人
     */
    @Value("${dd.talk.special}")
    private String specialMan;

    /**
     * 端口号
     */
    @Value("${server.port}")
    private String port;

    /**
     * zmpUrl
     */
    @Value("$(zmp.url)")
    private String zmpUrl;

    @Value("${file.dir}")
    private String fileDir;

    @Autowired
    private FileService fileService;

    /**
     * 测试环节
     */
    private static List<String> testLinkList = new ArrayList<String>() {
        {
            add("Testing");
            add("To be testing");
        }
    };

    /**
     * Description: 推送钉钉消息：9点告警信息
     *
     * @author bai.wenlong
     * @date 2020/1/20 13:13
     */
    @Override
    public void pushDdMsg4Warn() {
        RespDto respDto = new RespDto();
        LocalDate date = LocalDate.now();
        File file = new File(fileDir + "/" + date + ".xls");
        List<TaskDto> taskDtoList = new ArrayList<>();
        if (!file.exists()) {
            List<String> mobiles = new ArrayList<>();
            mobiles.add(specialMan);
            try {
                pushWarnText("赶快来上传今天的任务单列表喽！！(^￣_￣^) \n" + getTaskChartUrl(), mobiles);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            return;
        } else {
            try {
                taskDtoList = fileService.parseExcel(new FileInputStream(file), respDto, "1");
            } catch (Exception e) {
                e.printStackTrace();
                if (StringUtils.isEmpty(respDto.getMsg())) {
                    respDto.setMsg((StringUtils.isEmpty(e.getMessage()) ? "任务单告警推送出错啦！！" : e.getMessage()) + "ε(┬┬﹏┬┬)3");
                }
            }
            if (StringUtils.isEmpty(respDto.getMsg())) {
                respDto.setFlag(true);
            }
        }
        warpAndPushWarnData(taskDtoList, respDto);
    }

    /**
     * Description: 组装告警信息
     *
     * @author bai.wenlong
     * @date 2020/1/21 13:43
     */
    private void warpAndPushWarnData(List<TaskDto> taskDtoList, RespDto respDto) {
        if (!respDto.isFlag()) {
            pushWarnText(StringUtils.isEmpty(respDto.getMsg()) ? "系统出错了，快去看看吧！！(*￣︿￣)" : respDto.getMsg(), null);
        } else {
            // 推送开发环节数据
            pushWarnText(parseWarnData4Dev(taskDtoList), null);
            // 推送测试环节数据
            pushWarnText(parseWarnData4Test(taskDtoList), null);
        }
    }

    /**
     * Description: 推送消息
     *
     * @author bai.wenlong
     * @date 2020/1/21 16:59
     */
    private void pushWarnText(String content, List<String> mobiles) {
        try {
            Long timestamp = System.currentTimeMillis();
            String sign = getServerUrl(timestamp);
            String url = serverUrl + "&timestamp=" + timestamp + "&sign=" + sign;
            DefaultDingTalkClient client = new DefaultDingTalkClient(url);
            OapiRobotSendRequest request = new OapiRobotSendRequest();
            request.setMsgtype("text");
            OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
            OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
            if (mobiles != null) {
                at.setAtMobiles(mobiles);
            } else {
                at.setIsAtAll("true");
            }
            request.setAt(at);
            text.setContent(content);
            request.setText(text);
            client.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Description: 处理告警信息
     *
     * @author bai.wenlong
     * @date 2020/1/21 13:49
     */
    private String parseWarnData4Dev(List<TaskDto> taskDtoList) {
        StringBuilder sb = new StringBuilder();
        sb.append("开发环节任务单告警(距离转到下一环节时间不足2天)\n");
        //开发告警数据
        Map<String, List<TaskDto>> devDataMap = taskDtoList.stream().filter(item -> {
            String devDate = item.getDevDate();
            if (StringUtils.isEmpty(devDate) || testLinkList.contains(item.getLink()) ||
                    StringUtils.isEmpty(item.getPublishPatch())) {
                return false;
            }
            return compareDate(devDate);
        }).collect(Collectors.groupingBy(TaskDto::getPublishPatch));
        if (devDataMap.size() < 1) {
            sb.append("研发小伙伴棒棒哒，没有告警的任务单！！(^￣_￣^)");
        } else {
            sb.append(parseNameAndTask(devDataMap));
        }
        return sb.toString();
    }

    /**
     * Description: 处理告警信息
     *
     * @author bai.wenlong
     * @date 2020/1/21 13:49
     */
    private String parseWarnData4Test(List<TaskDto> taskDtoList) {
        StringBuilder sb = new StringBuilder();
        sb.append("测试环节任务单告警(距离转到下一环节时间不足2天)\n");
        //测试告警数据
        Map<String, List<TaskDto>> testDataMap = taskDtoList.stream().filter(item -> {
            String publishDate = item.getPublishPlanDate();
            if (StringUtils.isEmpty(publishDate) || !testLinkList.contains(item.getLink()) ||
                    StringUtils.isEmpty(item.getPublishPatch())) {
                return false;
            }
            return compareDate(publishDate);
        }).collect(Collectors.groupingBy(TaskDto::getPublishPatch));
        if (testDataMap.size() < 1) {
            sb.append("测试小伙伴棒棒哒，没有告警的任务单！！(^￣_￣^)");
        } else {
            sb.append(parseNameAndTask(testDataMap));
        }
        return sb.toString();
    }

    /**
     * Description: 拼接处处理人和任务单
     *
     * @author bai.wenlong
     * @date 2020/1/21 15:13
     */
    private String parseNameAndTask(Map<String, List<TaskDto>> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<TaskDto>> entry : map.entrySet()) {
            String key = entry.getKey();
            List<TaskDto> value = entry.getValue();
            sb.append(key);
            sb.append(":\n");
            Map<String, List<TaskDto>> tempMap = value.stream().collect(Collectors.groupingBy(TaskDto::getProcessName));
            for (Map.Entry<String, List<TaskDto>> e : tempMap.entrySet()) {
                String k = e.getKey();
                List<TaskDto> v = e.getValue();
                sb.append(k.substring(k.indexOf("]") + 1));
                sb.append(":");
                sb.append(v.stream().map(TaskDto::getTaskNum).collect(Collectors.joining(",")));
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Description: 比较日期，是否符合告警条件
     *
     * @author bai.wenlong
     * @date 2020/1/21 14:10
     */
    private boolean compareDate(String date) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate regDate = LocalDate.parse(date, dateTimeFormatter);
        LocalDate futureDate = LocalDate.now().plusDays(2L);
        return !regDate.isAfter(futureDate);
    }


    /**
     * Description: 推送钉钉消息：10点图形信息
     *
     * @author bai.wenlong
     * @date 2020/1/20 13:13
     */
    @Override
    public void pushDdMsg4Image() {
        try {
            Long timestamp = System.currentTimeMillis();
            String sign = getServerUrl(timestamp);
            String url = serverUrl + "&timestamp=" + timestamp + "&sign=" + sign;
            DingTalkClient client = new DefaultDingTalkClient(url);
            OapiRobotSendRequest request = new OapiRobotSendRequest();
            request.setMsgtype("markdown");
            OapiRobotSendRequest.Markdown markdown = new OapiRobotSendRequest.Markdown();
            markdown.setTitle("任务单图形推送");
            markdown.setText("#### 充值研发团队-Turbo任务单\n" +
                    "> ![screenshot](https://note.youdao.com/yws/public/resource/58b17a975e2747e6409f2d2508b7323f/xmlnote/8573969111B34DF5BB07FCF8814955EF/5037)\n" +
                    "> ###### 10点发布 [任务图形](" + getTaskChartUrlView() + ") \n");
            request.setMarkdown(markdown);
            OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
            at.setIsAtAll("true");
            request.setAt(at);
            client.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Description: 从zmp拉去数据并生成文件
     *
     * @author bai.wenlong
     * @date 2020/1/21 10:51
     */
    @Override
    public void pullData() {
        //从ZMP获取数据
        try {
            List<TaskDto> taskDtoList = new ArrayList<>();
            excelUtils.createExcelByData(taskDtoList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Description: 从ZMP请求数据
     *
     * @author bai.wenlong
     * @date 2020/1/20 13:53
     */
    private void requestZmp() throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(zmpUrl);
        HttpResponse execute = httpClient.execute(httpGet);
        HttpEntity entity = execute.getEntity();
        System.out.println(entity);
        String result = new BufferedReader(new InputStreamReader(entity.getContent()))
                .lines().collect(Collectors.joining("\n"));
        System.out.println(result);
    }

    /**
     * Description: 获取展示的图片地址
     *
     * @author bai.wenlong
     * @date 2020/1/20 10:08
     */
    private String getServerUrl() throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getLocalHost();
        return "http://" + inetAddress.getHostAddress() + ":" + port + "/task-chart/";
    }

    /**
     * Description: 获取任务单图形化地址
     *
     * @author bai.wenlong
     * @date 2020/1/20 10:08
     */
    private String getTaskChartUrlView() throws UnknownHostException {
        return getServerUrl() + "index?view=chart";
    }

    /**
     * Description: 获取任务单图形化地址
     *
     * @author bai.wenlong
     * @date 2020/1/20 10:08
     */
    private String getTaskChartUrl() throws UnknownHostException {
        return getServerUrl() + "index";
    }


    /**
     * Description: 获取签名
     *
     * @param timestamp 时间
     * @return String
     * @author bai.wenlong
     * @date 2020/1/20 9:55
     */
    private String getServerUrl(Long timestamp) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return URLEncoder.encode(new String(Base64.encodeBase64(signData)), "UTF-8");
    }
}
