package com.taskshowchart.controller;

import com.taskshowchart.dto.RespDto;
import com.taskshowchart.service.DingTalkService;
import com.taskshowchart.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Description: 上传文件到指定目录
 *
 * @author bai.wenlong
 * @date 2020/3/12 18:37
 */
@RestController
@RequestMapping("/")
public class UploadController {
    @Autowired
    private FileService fileService;

    @Value("${file.dir}")
    private String fileDir;

    @Autowired
    private DingTalkService dingTalkService;

    /**
     * Description: 文件上传
     *
     * @param file MultipartFile
     * @return RespDto
     * @author bai.wenlong
     * @date 2020/3/12 18:39
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public RespDto uoloadFile(@RequestParam("file") MultipartFile file) {
        RespDto respDto = new RespDto();
        uploadFile(file, respDto);
        return respDto;
    }

    /**
     * Description: 文件上传
     *
     * @param file MultipartFile
     * @return RespDto
     * @author bai.wenlong
     * @date 2020/3/12 18:39
     */
    @RequestMapping(value = "/uploadAndPush", method = RequestMethod.POST)
    public RespDto uploadAndPush(@RequestParam("file") MultipartFile file) {
        RespDto respDto = new RespDto();
        //上传文件
        uploadFile(file, respDto);
        if (respDto.isFlag()) {
            // 推送到钉钉
            dingTalkService.pushDdMsg4Warn();
            dingTalkService.pushDdMsg4Image();
        }
        return respDto;
    }

    /**
     * Description: 解析文件
     *
     * @author bai.wenlong
     * @date 2020/3/12 19:55
     */
    public void uploadFile(MultipartFile file, RespDto respDto) {
        // 获取文件名，带后缀
        String originalFilename = file.getOriginalFilename();
        // 获取文件的后缀格式
        String fileSuffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if ("xls".equals(fileSuffix)) {
            try {
                //先把文件校验下格式是否正确
                fileService.parseExcel(file.getInputStream(), respDto, "1");
                LocalDate today = LocalDate.now();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String dateStr = today.format(fmt);
                String newFileName = dateStr + "." + fileSuffix;
                // 该方法返回的为当前项目的工作目录，即在哪个地方启动的java线程
                String filePath = fileDir + newFileName;
                File destFile = new File(filePath);
                // 如果文件已经存在先删除
                if (destFile.exists()) {
                    destFile.delete();
                }
                if (!destFile.getParentFile().exists()) {
                    destFile.getParentFile().mkdirs();
                }
                file.transferTo(destFile);
                respDto.setFlag(true);
            } catch (Exception e) {
                if (StringUtils.isEmpty(respDto.getMsg())) {
                    respDto.setMsg((StringUtils.isEmpty(e.getMessage()) ? "文件上传出错啦！！" : e.getMessage()) + "ε(┬┬﹏┬┬)3");
                }
                respDto.setFlag(false);
            }
        } else {
            respDto.setMsg("文件格式不正确！！(*￣︿￣)");
        }
    }
}
