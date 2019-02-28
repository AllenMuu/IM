package com.hzt.file.server.domain.vo;

import com.hzt.common.base.domain.vo.UserSessionVO;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FileServerHttpRequest {

    private String requestUrl; // 本次请求的URL
    private HttpRequest request; // 本次请求对象，注意不包含请求体
    private HttpPostRequestDecoder decoder; // 请求解码器
    private UserSessionVO userSessionVO; // 用户Session 信息
    private Map<String, String> parameterMap = new HashMap<String, String>(); // 本次请求的参数列表
    private FileServerHttpResponse fileServerHttpResponse = new FileServerHttpResponse(); // 请求的响应对象
    private Date requestTime = new Date(); // 请求开始时间

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public HttpPostRequestDecoder getDecoder() {
        return decoder;
    }

    public void setDecoder(HttpPostRequestDecoder decoder) {
        this.decoder = decoder;
    }

    public Map<String, String> getParameterMap() {
        return parameterMap;
    }

    public void setParameterMap(Map<String, String> parameterMap) {
        this.parameterMap = parameterMap;
    }

    public UserSessionVO getUserSessionVO() {
        return userSessionVO;
    }

    public FileServerHttpRequest setUserSessionVO(UserSessionVO userSessionVO) {
        this.userSessionVO = userSessionVO;
        return this;
    }

    public FileServerHttpResponse getFileServerHttpResponse() {
        return fileServerHttpResponse;
    }

    public void setFileServerHttpResponse(FileServerHttpResponse fileServerHttpResponse) {
        this.fileServerHttpResponse = fileServerHttpResponse;
    }

    public Date getRequestTime() {
        return requestTime;
    }
}
