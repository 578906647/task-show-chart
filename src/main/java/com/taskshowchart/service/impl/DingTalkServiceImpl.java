package com.taskshowchart.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.taskshowchart.dto.RespDto;
import com.taskshowchart.dto.TaskDto;
import com.taskshowchart.service.DingTalkService;
import com.taskshowchart.service.FileService;
import com.taskshowchart.util.ExcelUtils;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
    @Value("${zmp.url}")
    private String zmpUrl;

    /**
     * 文件上传路径
     */
    @Value("${file.dir}")
    private String fileDir;

    /**
     * 员工工号
     */
    @Value("${staff.code}")
    private String staffCode;

    @Autowired
    private FileService fileService;

    private Map<String, String> staffInfoMap;

    /**
     * 需要@的开发
     */
    private List<String> devRemindList;

    /**
     * 需要@的测试
     */
    private List<String> testRemindList;

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
            devRemindList = new ArrayList<>();
            testRemindList = new ArrayList<>();
            // 推送开发环节数据
            pushWarnText(parseWarnData4Dev(taskDtoList), devRemindList);
            // 推送测试环节数据
            pushWarnText(parseWarnData4Test(taskDtoList), testRemindList);
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
            if (!CollectionUtils.isEmpty(mobiles)) {
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
            sb.append(parseNameAndTask(devDataMap, true));
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
            sb.append(parseNameAndTask(testDataMap, false));
        }
        return sb.toString();
    }

    /**
     * Description: 拼接处处理人和任务单
     *
     * @author bai.wenlong
     * @date 2020/1/21 15:13
     */
    private String parseNameAndTask(Map<String, List<TaskDto>> map, boolean devFlag) {
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
                if (devFlag) {
                    devRemindList.add(staffInfoMap.get(k));
                } else {
                    testRemindList.add(staffInfoMap.get(k));
                }
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
            List<TaskDto> taskDtoList = requestZmp();
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
    private List<TaskDto> requestZmp() throws Exception {
        //从ZMP获取数据
        PrintWriter out = null;
        BufferedReader in = null;
        List<TaskDto> taskDtoList = new ArrayList<>();
        try {

            URL realUrl = new URL(zmpUrl);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            //1.获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            //2.中文有乱码的需要将PrintWriter改为如下
            //out=new OutputStreamWriter(conn.getOutputStream(),"UTF-8")
            // 发送请求参数
            String param = "{'ACTIVE':'1', 'ORG_STATE':'O', 'DIR_ID':'1,6,7','serviceName':'QueryTansAdv', 'ISREQ':'1'}";
            JSONObject json = JSONObject.parseObject(param);
            json.put("STAFF_CODE", staffCode);
            out.print(json.toJSONString());
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            List<Map<String, Object>> resultMapList = (List<Map<String, Object>>) JSONObject.parse(result.toString());
            TaskDto taskDto;
            for (Map<String, Object> map : resultMapList) {
                taskDto = new TaskDto();
                taskDto.setTitle((String) map.get("title"));
                taskDto.setTaskNum((String) map.get("transId"));
                taskDto.setDevDate((String) map.get("firstDeliveryDate"));
                taskDto.setPublishPlanDate((String) map.get("planPublishDate"));
                taskDto.setLink((String) map.get("dealState"));
                taskDto.setProcessName((String) map.get("dealStaff"));
                taskDto.setPublishPatch((String) map.get("patchCode"));
                taskDtoList.add(taskDto);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("请求ZMP发生错误");
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return taskDtoList;
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

    @Value("${staff.info}")
    public void parseStaffInfo(String str) {
        staffInfoMap = (Map<String, String>) JSONObject.parse(str);
    }
}
