package com.taskshowchart.service;

import com.taskshowchart.dto.RespDto;
import com.taskshowchart.dto.TaskDto;

import java.io.InputStream;
import java.util.List;

/**
 * Description: 文件处理接口
 *
 * @author bai.wenlong
 * @date 2020/1/20 16:46
 */
public interface FileService {
    /**
     * Description: 解析文件生成图形
     *
     * @author bai.wenlong
     * @date 2020/1/20 16:53
     */
    void parseExcelForImage(InputStream inputStream, RespDto respDto, String dateType) throws Exception;

    /**
     * Description: 解析文件推送消息
     *
     * @author bai.wenlong
     * @date 2020/1/20 16:53
     */
    List<TaskDto> parseExcel(InputStream inputStream, RespDto respDto, String dateType) throws Exception;

}
