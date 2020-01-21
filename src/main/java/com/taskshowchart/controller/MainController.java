package com.taskshowchart.controller;

import com.taskshowchart.dto.RespDto;
import com.taskshowchart.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;

/**
 * Description: 跳转页面
 *
 * @author bai.wenlong
 * @date 2020/1/20 15:47
 */
@RestController
@RequestMapping("/")
public class MainController {

    @Autowired
    private FileService fileService;

    @Value("${file.dir}")
    private String fileDir;

    /**
     * Description: TODO
     *
     * @return String
     * @author bai.wenlong
     * @date 2020/1/9 10:41
     */
    @GetMapping("/parse")
    @ResponseBody
    public RespDto parse(HttpServletRequest req) {
        String date = req.getParameter("date");
        RespDto respDto = new RespDto();
        if (StringUtils.isEmpty(date)) {
            respDto.setMsg("兄弟，想绕过日期验证？？(*￣︿￣)");
            return respDto;
        }
        File file = new File(fileDir + "/" + date + ".xls");
        if (!file.exists()) {
            respDto.setMsg("选择的日期没有数据哦！！(*￣︿￣)");
            return respDto;
        }
        try {
            fileService.parseExcelForImage(new FileInputStream(file), respDto, req.getParameter("dateType"));
        } catch (Exception e) {
            e.printStackTrace();
            if (StringUtils.isEmpty(respDto.getMsg())) {
                respDto.setMsg((StringUtils.isEmpty(e.getMessage()) ? "系统出错啦！！" : e.getMessage()) + "ε(┬┬﹏┬┬)3");
            }
        }
        if (StringUtils.isEmpty(respDto.getMsg())) {
            respDto.setFlag(true);
        }
        return respDto;
    }

    /**
     * Description: TODO
     *
     * @return String
     * @author bai.wenlong
     * @date 2020/1/9 10:41
     */
    @GetMapping("/index")
    public ModelAndView index() {
        return new ModelAndView("chart");
    }
}
