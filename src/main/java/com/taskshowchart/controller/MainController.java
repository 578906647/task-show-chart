package com.taskshowchart.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * Description: 跳转页面
 */
@RestController
@RequestMapping("/")
public class MainController {

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
