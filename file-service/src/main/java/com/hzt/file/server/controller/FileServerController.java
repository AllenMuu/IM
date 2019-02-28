package com.hzt.file.server.controller;

import com.hzt.file.server.domain.vo.FileServerHttpRequest;
import com.hzt.file.server.domain.vo.FileServerHttpResponse;

public interface FileServerController {

    FileServerHttpResponse doExecute(Object message, FileServerHttpRequest fileServerHttpRequest) throws Exception;
}
