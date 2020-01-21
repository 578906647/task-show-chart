package com.taskshowchart.util;

import com.taskshowchart.dto.TaskDto;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Description: ExcelUtils
 *
 * @author bai.wenlong
 * @date 2020/1/20 14:16
 */
@Component
public class ExcelUtils {
    @Value("${file.dir}")
    private String fileDir;

    /**
     * sheetName
     */
    private String sheetName = "任务单";
    /**
     * Excel文件sheet页的第一行
     */
    private String[] titleArr = {"标题", "事务单号", "研发封板日期", "计划发布日期", "工单环节", "工单处理人", "发布补丁"};
    /**
     * 表头对应的数据字段
     */
    private String[] titleDataArr = {"Title", "TaskNum", "DevDate", "PublishPlanDate", "Link", "ProcessName", "PublishPatch"};


    /**
     * HSSFWorkbook
     */
    private HSSFWorkbook workbook = null;


    /**
     * 创建新excel.
     *
     * @param filePath excel的路径
     */
    private void createExcel(String filePath) throws Exception {
        //创建workbook
        workbook = new HSSFWorkbook();
        //添加Worksheet（不添加sheet时生成的xls文件打开时会报错)
        workbook.createSheet(sheetName);
        //新建文件
        FileOutputStream out = null;
        try {
            //添加表头
            //创建第一行
            Row row = workbook.getSheet(sheetName).createRow(0);
            for (int i = 0; i < titleArr.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(titleArr[i]);
            }
            out = new FileOutputStream(filePath);
            workbook.write(out);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 往excel中写入.
     *
     * @param filePath    文件路径
     * @param taskDtoList Object
     */
    private void writeToExcel(String filePath, List<TaskDto> taskDtoList) throws Exception {
        //创建workbook
        File file = new File(filePath);
        try {
            workbook = new HSSFWorkbook(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream out = null;
        HSSFSheet sheet = workbook.getSheet(sheetName);
        // 获取表格的总行数
        // 需要加一
        int rowCount = sheet.getLastRowNum() + 1;
        try {
            //最新要添加的一行
            Row row;
            Class<TaskDto> taskDtoClass = TaskDto.class;
            for (TaskDto taskDto : taskDtoList) {
                row = sheet.createRow(rowCount);
                rowCount++;
                for (int j = 0; j < titleDataArr.length; j++) {
                    String titleData = titleDataArr[j];
                    String methodName = "get" + titleData;
                    // 设置要执行的方法
                    Method method = taskDtoClass.getDeclaredMethod(methodName);
                    // 执行该get方法,即要插入的数据
                    Optional<Object> optional = Optional.ofNullable(method.invoke(taskDto));
                    Cell cell = row.createCell(j);
                    optional.ifPresent(o -> cell.setCellValue(o.toString()));
                }
            }


            out = new FileOutputStream(filePath);
            workbook.write(out);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Description: 根据数据生成excel表格
     *
     * @author bai.wenlong
     * @date 2020/1/20 14:35
     */
    public boolean createExcelByData(List<TaskDto> taskDtoList) throws Exception {
        LocalDate date = LocalDate.now();
        String filePath = fileDir + "/" + date + ".xls";
        //判断该名称的文件是否存在
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        //创建
        createExcel(filePath);
        //写入到excel
        writeToExcel(filePath, taskDtoList);
        return true;
    }
}
