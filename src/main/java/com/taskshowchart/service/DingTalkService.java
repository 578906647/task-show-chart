package com.taskshowchart.service;

/**
 * Description: 任务接口
 *
 * @author bai.wenlong
 * @date 2020/1/20 13:13
 */
public interface DingTalkService {
    /**
     * Description: 推送钉钉消息：9点告警信息
     *
     * @author bai.wenlong
     * @date 2020/1/20 13:13
     */
    void pushDdMsg4Warn();

    /**
     * Description: 推送钉钉消息：10点图形信息
     *
     * @author bai.wenlong
     * @date 2020/1/20 13:13
     */
    void pushDdMsg4Image();

    /**
     * Description: 从zmp拉去数据并生成文件
     *
     * @author bai.wenlong
     * @date 2020/1/21 10:51
     */
    void pullData();
}
