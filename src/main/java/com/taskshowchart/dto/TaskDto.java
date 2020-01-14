package com.taskshowchart.dto;

import lombok.Data;

/**
 * 任务单数据载体
 */
@Data
public class TaskDto {
    /**
     * 计划发布日期
     */
    private String publishPlanDate;
    /**
     * 工单环节
     */
    private String link;

    /**
     * 发布补丁
     */
    private String publishPatch;

    /**
     * 项目名称
     */
    private String projectName;

    @Override
    public String toString() {
        return "TaskDto{" +
                "publishPlanDate='" + publishPlanDate + '\'' +
                ", link='" + link + '\'' +
                ", publishPatch='" + publishPatch + '\'' +
                ", projectName='" + projectName + '\'' +
                '}';
    }
}
