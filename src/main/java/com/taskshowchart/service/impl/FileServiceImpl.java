package com.taskshowchart.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.taskshowchart.dto.RespDto;
import com.taskshowchart.dto.TaskDto;
import com.taskshowchart.service.FileService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Description: TODO
 *
 * @author bai.wenlong
 * @date 2020/1/20 16:49
 */
@Service
public class FileServiceImpl implements FileService {
    /**
     * Description:  解析文件生成图形
     *
     * @param inputStream InputStream
     * @param respDto     RespDto
     * @author bai.wenlong
     * @date 2020/1/9 14:26
     */
    @Override
    public void parseExcelForImage(InputStream inputStream, RespDto respDto, String dateType) throws Exception {
        List<TaskDto> taskDtoList = parseExcel(inputStream, respDto, dateType);
        wrapDataForImage(taskDtoList, respDto, dateType);
        System.out.println("respDto=" + JSONObject.toJSONString(respDto));
    }

    /**
     * Description: 解析文件
     *
     * @author bai.wenlong
     * @date 2020/1/21 11:03
     */
    @Override
    public List<TaskDto> parseExcel(InputStream inputStream, RespDto respDto, String dateType) throws Exception {
        List<String> titleList = getTitleList();
        // 单元格对应的名称集合
        Workbook workbook;
        workbook = WorkbookFactory.create(inputStream);
        inputStream.close();
        //工作表对象
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            respDto.setMsg("文件第一页呢？？(*￣︿￣)");
            throw new Exception();
        }
        //总行数
        int rowLength = sheet.getLastRowNum() + 1;
        if (rowLength < 1) {
            respDto.setMsg("没有数据怎么解析？？(*￣︿￣)");
            throw new Exception();
        }
        System.out.println("总行数有多少行" + rowLength);
        //工作表的列
        Row row = sheet.getRow(0);
        if (row == null) {
            respDto.setMsg("呕吼，表头呢？？(*￣︿￣)");
            throw new Exception();
        }
        // 获取需要的数据对应的单元格
        Map<String, Integer> cellMap = getCellMap(row, titleList);

        //总列数
        int colLength = row.getLastCellNum();
        System.out.println("总列数有多少列" + colLength);

        List<TaskDto> taskDtoList = new ArrayList<>();
        //环节列表
        List<String> linkList = new ArrayList<>();
        for (int i = 1; i < rowLength; i++) {
            row = sheet.getRow(i);
            taskDtoList.add(getCellData(row, cellMap, linkList));
        }
        respDto.setLinkList(linkList);
        return taskDtoList;
    }

    /**
     * Description: 封装图像数据
     *
     * @author bai.wenlong
     * @date 2020/1/10 12:02
     */
    private void wrapDataForImage(List<TaskDto> taskDtoList, RespDto respDto, String dateType) {
        //根据发布分支进行分组
        Map<String, List<TaskDto>> tempMap = null;
        if ("1".equals(dateType)) {
            tempMap = taskDtoList.parallelStream().filter(item -> !StringUtils.isEmpty(item.getPublishPlanDate()))
                    .collect(Collectors.groupingBy(TaskDto::getPublishPatch));
        } else {
            tempMap = taskDtoList.parallelStream().filter(item -> !StringUtils.isEmpty(item.getDevDate()))
                    .collect(Collectors.groupingBy(TaskDto::getPublishPatch));
        }
        //在key加上发布日期时间
        TreeMap<String, List<TaskDto>> tempMap2 = new TreeMap<>();
        tempMap.forEach((key, value) -> {
            tempMap2.put(key + "(" + value.get(0).getPublishPlanDate() + ")", value);
        });

        //最终结果map //按照时间排个序
        TreeMap<String, Map<String, Long>> tempMap3 = new TreeMap<>((obj1, obj2) -> {
            int result = getDateStr(obj1).compareTo(getDateStr(obj2));
            if (result == 0) {
                return 1;
            }
            return result;
        });
        tempMap2.forEach((key, value) -> {
            //环节与数量map
            Map<String, Long> dataMap = value.stream().collect(Collectors.groupingBy(TaskDto::getLink, Collectors.counting()));
            tempMap3.put(key, dataMap);
        });
        // 统计任务单环节对数量结果集
        TreeMap<String, List<Long>> resultMap = new TreeMap<>();
        // 发布分支结果集
        List<String> publishPatchList = new ArrayList<>();
        tempMap3.forEach((key, value) -> {
            publishPatchList.add(key);
            respDto.getLinkList().forEach(link -> {
                if (resultMap.containsKey(link)) {
                    resultMap.get(link).add(Optional.ofNullable(value.get(link)).orElse(0L));
                } else {
                    resultMap.put(link, new ArrayList<Long>() {{
                        add(Optional.ofNullable(value.get(link)).orElse(0L));
                    }});
                }
            });
        });
        respDto.setDataMap(resultMap);
        respDto.setPublishPatchList(publishPatchList);
    }

    /**
     * Description: 获取版本日期，排序使用
     *
     * @author bai.wenlong
     * @date 2020/1/21 10:54
     */
    public String getDateStr(String verKey) {
        String a = spiltRtoL(verKey);
        String b = a.substring(1, 12);
        String c = spiltRtoL(b);
        return c;
    }

    /**
     * Description: 反转字符串
     *
     * @author bai.wenlong
     * @date 2020/1/21 10:54
     */
    public static String spiltRtoL(String s) {
        StringBuffer sb = new StringBuffer();
        int length = s.length();
        char[] c = new char[length];
        for (int i = 0; i < length; i++) {
            c[i] = s.charAt(i);
        }
        for (int i = length - 1; i >= 0; i--) {
            sb.append(c[i]);
        }

        return sb.toString();
    }

    /**
     * Description: 获取单元格数据构造dto
     *
     * @author bai.wenlong
     * @date 2020/1/9 17:04
     */
    private TaskDto getCellData(Row row, Map<String, Integer> cellMap, List<String> linkList) {
        TaskDto taskDto = new TaskDto();
        Cell cell = row.getCell(cellMap.get("计划发布日期"));
        if (cell != null) {
            cell.setCellType(Cell.CELL_TYPE_STRING);
            taskDto.setPublishPlanDate(cell.getStringCellValue().trim());
        }

        cell = row.getCell(cellMap.get("研发封版日期"));
        if (cell != null) {
            cell.setCellType(Cell.CELL_TYPE_STRING);
            taskDto.setDevDate(cell.getStringCellValue().trim());
        }

        cell = row.getCell(cellMap.get("工单环节"));
        if (cell != null) {
            cell.setCellType(Cell.CELL_TYPE_STRING);
            taskDto.setLink(cell.getStringCellValue());
            if (!StringUtils.isEmpty(taskDto.getPublishPlanDate())) {
                if (!linkList.contains(taskDto.getLink())) {
                    linkList.add(taskDto.getLink());
                }
            }
        }
        cell = row.getCell(cellMap.get("发布补丁"));
        if (cell != null) {
            cell.setCellType(Cell.CELL_TYPE_STRING);
            taskDto.setPublishPatch(cell.getStringCellValue());
        }
        cell = row.getCell(cellMap.get("工单处理人"));
        if (cell != null) {
            cell.setCellType(Cell.CELL_TYPE_STRING);
            taskDto.setProcessName(cell.getStringCellValue());
        }
        cell = row.getCell(cellMap.get("事务单号"));
        if (cell != null) {
            cell.setCellType(Cell.CELL_TYPE_STRING);
            taskDto.setTaskNum(cell.getStringCellValue());
        }
        cell = row.getCell(cellMap.get("标题"));
        if (cell != null) {
            cell.setCellType(Cell.CELL_TYPE_STRING);
            taskDto.setTitle(cell.getStringCellValue());
        }
        return taskDto;
    }

    /**
     * Description: 表头对应的所在单元格
     *
     * @param row       Row
     * @param titleList
     * @return Map<String, Integer>
     * @author bai.wenlong
     * @date 2020/1/9 16:45
     */
    private Map<String, Integer> getCellMap(Row row, List<String> titleList) throws Exception {
        Map<String, Integer> cellMap = new HashMap<>(16);
        int colLength = row.getLastCellNum();
        //得到指定的单元格
        Cell cell;
        for (int j = 0; j < colLength; j++) {
            cell = row.getCell(j);
            if (cell != null) {
                cell.setCellType(Cell.CELL_TYPE_STRING);
                String data = cell.getStringCellValue();
                if (titleList.contains(data)) {
                    cellMap.put(data, j);
                }
            }
        }
        if (cellMap.size() != 7) {
            String str = String.join(",", titleList);
            throw new Exception(str + " 列必须存在！！");
        }
        return cellMap;
    }

    /**
     * Description: 获取需要的表头集合
     *
     * @author bai.wenlong
     * @date 2020/1/13 19:25
     */
    private List<String> getTitleList() {
        List<String> titleList = new ArrayList<>();
        titleList.add("发布补丁");
        titleList.add("工单环节");
        titleList.add("工单处理人");
        titleList.add("事务单号");
        titleList.add("标题");
        titleList.add("计划发布日期");
        titleList.add("研发封版日期");
        return titleList;
    }
}
