package com.taskshowchart.controller;

import com.alibaba.fastjson.JSONObject;
import com.taskshowchart.dto.RespDto;
import com.taskshowchart.dto.TaskDto;
import org.apache.poi.ss.usermodel.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Description: 上传文件
 */
@RestController
public class UploadController {

    /**
     * 文件后缀集合
     */
    private static List<String> suffixList = new ArrayList<String>() {
        {
            add("xls");
            add("xlsx");
        }
    };

    /**
     * Description: 上传文件
     *
     * @author bai.wenlong
     * @date 2020/1/9 14:04
     */
    @PostMapping("/upload")
    @ResponseBody
    public RespDto upload(HttpServletRequest req, @RequestParam("file") MultipartFile file) {
        //  根据类型区分使用哪个日期作为数据的过滤条件
        String dateType = req.getParameter("dateType");
        List<String> titleList = getTitleList(dateType);
        RespDto respDto = new RespDto();
        if (file == null) {
            respDto.setMsg("兄弟，你的文件呢？？(*￣︿￣)");
            return respDto;
        }
        // 验证文件是否是excel文件
        String fileName = file.getOriginalFilename();
        String[] fileNameArr = fileName.split("\\.");
        if (fileNameArr.length != 2 || !suffixList.contains(fileNameArr[1])) {
            respDto.setMsg("写了要上传excel格式文件，看不见？？(*￣︿￣)");
            return respDto;
        }
        try {
            parseExcel(file.getInputStream(), respDto, titleList);
        } catch (Exception e) {
            e.printStackTrace();
            if (StringUtils.isEmpty(respDto.getMsg())) {
                respDto.setMsg((StringUtils.isEmpty(e.getMessage()) ? "系统出错啦！！" : e.getMessage()) + "ε(┬┬﹏┬┬)3");
            }
        }
        if (StringUtils.isEmpty(respDto.getMsg())) {
            respDto.setFlag(true);
            respDto.setMsg("上传成功，小哥哥正在马不停蹄的处理文件 !!(*^__^*)");
        }
        return respDto;
    }

    /**
     * Description: 获取需要的表头集合
     *
     * @author bai.wenlong
     * @date 2020/1/13 19:25
     */
    private List<String> getTitleList(String dateType) {
        List<String> titleList = new ArrayList<>();
        titleList.add("发布补丁");
        titleList.add("环节");
        if ("1".equals(dateType)) {
            titleList.add("研发计划发布日期");
        } else {
            titleList.add("研发封版日期");
        }
        return titleList;
    }

    /**
     * Description: TODO
     *
     * @param inputStream InputStream
     * @param respDto
     * @param titleList
     * @author bai.wenlong
     * @date 2020/1/9 14:26
     */
    private void parseExcel(InputStream inputStream, RespDto respDto, List<String> titleList) throws Exception {
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
        wrapData(taskDtoList, respDto);
        System.out.println("respDto=" + JSONObject.toJSONString(respDto));
    }

    /**
     * Description: 封装数据
     *
     * @author bai.wenlong
     * @date 2020/1/10 12:02
     */
    private void wrapData(List<TaskDto> taskDtoList, RespDto respDto) {
        //根据发布分支进行分组
        Map<String, List<TaskDto>> tempMap = taskDtoList.parallelStream().filter(item -> !StringUtils.isEmpty(item.getPublishPlanDate()))
                .collect(Collectors.groupingBy(TaskDto::getPublishPatch));

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

    public String getDateStr(String verKey) {

        String a = spiltRtoL(verKey);

        String b = a.substring(1, 12);

        String c = spiltRtoL(b);

        System.out.println(c);
        return c;
    }

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
        Cell cell = row.getCell(cellMap.get("研发计划发布日期") == null ? cellMap.get("研发封版日期") : cellMap.get("研发计划发布日期"));
        if (cell != null) {
            cell.setCellType(Cell.CELL_TYPE_STRING);
            taskDto.setPublishPlanDate(cell.getStringCellValue().trim());
        }

        cell = row.getCell(cellMap.get("环节"));
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
        if (cellMap.size() != 3) {
            String str = String.join(",", titleList);
            throw new Exception(str + " 列必须存在！！");
        }
        return cellMap;
    }

}
