package com.taskshowchart.scheduled;

import com.taskshowchart.service.DingTalkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Description: 定时任务，从zmp拉去数据处理
 *
 * @author bai.wenlong
 * @date 2020/1/20 9:40
 */
@Configuration
@EnableScheduling
public class ScheduledTask {

    /**
     * taskChartService
     */
    @Autowired
    private DingTalkService dingTalkService;

    /**
     * Description: 定时任务：处理告警信息
     *
     * @author bai.wenlong
     * @date 2020/1/20 9:40
     */
    @Scheduled(cron = "0 0 10 * * *")
    private void pushDdMsg4Warn() {
        dingTalkService.pushDdMsg4Warn();
    }

    /**
     * Description: 定时任务：处理图形信息
     *
     * @author bai.wenlong
     * @date 2020/1/20 9:40
     */
    @Scheduled(cron = "0 0 10 * * *")
    private void pushDdMsg4Image() {
        dingTalkService.pushDdMsg4Image();
    }

    /**
     * Description: 定时任务：从ZMP拉去数据
     *
     * @author bai.wenlong
     * @date 2020/1/20 9:40
     */
    @Scheduled(cron = "0 30 9 * * *")
    private void pullData() {
        dingTalkService.pullData();
    }
}
