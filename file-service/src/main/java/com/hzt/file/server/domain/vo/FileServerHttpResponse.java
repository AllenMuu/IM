package com.hzt.file.server.domain.vo;

import com.hzt.common.base.domain.Result;
import io.netty.handler.codec.http.HttpResponse;

public class FileServerHttpResponse {

    private HttpResponse response; // 本次响应对象，注意不包含响应体
    private String fileDownload; // 文件下载对象
    private boolean finished; // 所有请求是否已经处理结束
    private Result result; // 响应结果集

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    public String getFileDownload() {
        return fileDownload;
    }

    public void setFileDownload(String fileDownload) {
        this.fileDownload = fileDownload;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public Result getResult() {
        return result;
    }

    public FileServerHttpResponse setResult(Result result) {
        this.result = result;
        return this;
    }
}
