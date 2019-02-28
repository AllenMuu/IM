package com.hzt.file.server.controller;

import com.hzt.common.constant.Constants;
import com.hzt.common.util.ExceptionUtil;
import com.hzt.file.server.domain.vo.FileServerHttpRequest;
import com.hzt.file.server.domain.vo.FileServerHttpResponse;
import org.springframework.stereotype.Component;

@Component("FileServerControllerFavicon")
public class FileServerControllerFavicon implements FileServerController {

    @Override
    public FileServerHttpResponse doExecute(Object message, FileServerHttpRequest fileServerHttpRequest) throws Exception {
        ExceptionUtil.rollback("请求的文件未找到！", Constants.OPERATION_FAIL);
        return null;
    }
}
