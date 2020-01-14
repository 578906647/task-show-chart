package com.taskshowchart.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

@Data
public class RespDto implements Serializable {
    private boolean flag = false;
    private String msg;
    private TreeMap<String, List<Long>> dataMap;
    private List<String> linkList;
    private List<String> publishPatchList;

    @Override
    public String toString() {
        return "RespDto{" +
                "flag=" + flag +
                ", msg='" + msg + '\'' +
                ", dataMap=" + dataMap +
                ", linkList=" + linkList +
                ", publishDateList=" + publishPatchList +
                '}';
    }
}
