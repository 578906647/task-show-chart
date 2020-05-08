package com.taskshowchart.dto;

import lombok.Data;

/**
 * 任务单数据载体
 */
@Data
public class TaskDto {
    /**
     * 标题
     */
    private String title;
    /**
     * 事务单号
     */
    private String taskNum;
    /**
     * 计划发布日期
     */
    private String publishPlanDate;

    /**
     * 研发封版日期
     */
    private String devDate;

    /**
     * 工单环节
     */
    private String link;

    /**
     * 发布补丁
     */
    private String publishPatch;

    /**
     * 处理人名称
     */
    private String processName;

    @Override
    public String toString() {
        return "TaskDto{" +
                "title='" + title + '\'' +
                ", taskNum='" + taskNum + '\'' +
                ", publishPlanDate='" + publishPlanDate + '\'' +
                ", devDate='" + devDate + '\'' +
                ", link='" + link + '\'' +
                ", publishPatch='" + publishPatch + '\'' +
                ", processName='" + processName + '\'' +
                '}';
    }
}
