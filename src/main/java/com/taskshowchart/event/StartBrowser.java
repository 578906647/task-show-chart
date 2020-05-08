package com.taskshowchart.event;

import com.taskshowchart.service.DingTalkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.IOException;

/**
 * Description: 监听程序是否启动
 *
 * @author bai.wenlong
 * @date 2020/1/20 15:44
 */
@Configuration
public class StartBrowser {
    @Autowired
    private DingTalkService dingTalkService;
    @EventListener({ApplicationReadyEvent.class})
    void applicationReadyEvent() {
//        System.out.println("应用已经准备就绪 ... 启动浏览器");
//        String url = "http://localhost:28080/task-chart/index";
//        Runtime runtime = Runtime.getRuntime();
//        try {
//            runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        dingTalkService.pullData();
        dingTalkService.pushDdMsg4Warn();
        dingTalkService.pushDdMsg4Image();

    }
}
