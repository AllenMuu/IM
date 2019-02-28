package com.hzt.file.server.controller;

import com.hzt.common.annotation.FunctionDesc;
import com.hzt.common.base.domain.Result;
import com.hzt.file.server.domain.qo.FileQueryVO;
import com.hzt.file.server.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    /**
     * 根据ID获取文件信息
     * @param fileQueryVO
     * @return
     */
    @GetMapping("/file/query")
    @FunctionDesc(module = "文件服务", operationName = "查询文件")
    public Result getUserInfoById(HttpServletRequest request, FileQueryVO fileQueryVO){
        return fileService.getFileById(fileQueryVO);
    }
}
