package com.hzt.file.server.controller;

import com.hzt.common.constant.Constants;
import com.hzt.common.context.BeanTool;
import com.hzt.common.util.ExceptionUtil;
import com.hzt.file.server.constant.FileServerConstants;
import com.hzt.file.server.domain.po.CommonFile;
import com.hzt.file.server.domain.vo.FileServerHttpRequest;
import com.hzt.file.server.domain.vo.FileServerHttpResponse;
import com.hzt.file.server.service.FileService;
import com.hzt.file.server.util.FileContentTypeUtil;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.net.URLEncoder;

@Component("FileServerControllerDownload")
public class FileServerControllerDownload implements FileServerController {

    @Override
    public FileServerHttpResponse doExecute(Object message, FileServerHttpRequest fileServerHttpRequest) throws Exception {

        // 校验请求是否是GET 请求
        if (!fileServerHttpRequest.getRequest().method().equals(HttpMethod.GET)) {
            ExceptionUtil.rollback("请求方法不允许！", Constants.FILE_DOWNLOAD_FAIL);
        }

        // 若读取到结束标志分片，则表示所有分片都已读取完毕，否则表示未读取完毕，先不处理
        if (!(message instanceof LastHttpContent)) {
            return null;
        }

        // 校验请求参数
        if (StringUtils.isBlank(fileServerHttpRequest.getParameterMap().get("fileId"))) {
            ExceptionUtil.rollback("请求参数有误！", Constants.FILE_DOWNLOAD_FAIL);
        }

        // 获取文件主键
        Long fileId = Long.valueOf(fileServerHttpRequest.getParameterMap().get("fileId"));

        // 根据文件主键获取文件信息
        FileService fileService = BeanTool.getBean("FileService", FileService.class);
        CommonFile commonFile = (CommonFile) fileService.getFileById(fileId).getModel();
        if (commonFile == null) {
            ExceptionUtil.rollback("请求的文件未找到！", Constants.FILE_DOWNLOAD_FAIL);
        }

        // 检测文件校验码
        String fileCode = fileServerHttpRequest.getParameterMap().get("fileCode");
        if (StringUtils.isNotBlank(commonFile.getCode()) && !commonFile.getCode().equals(fileCode)) {
            ExceptionUtil.rollback("无权限访问该文件！", Constants.FILE_DOWNLOAD_FAIL);
        }

        // 校验请求下载的文件
        File downloadFile = new File(commonFile.getStorePath());

        // 若文件不存在或指向是路径或者文件是隐藏的，则禁止读取
        if (!downloadFile.exists() || !downloadFile.isFile() || downloadFile.isHidden()) {
            ExceptionUtil.rollback("请求的文件未找到！", Constants.FILE_DOWNLOAD_FAIL);
        }

        // 原始文件名，若原始文件名为空，则使用现行文件名称
        String originalFileName = commonFile.getOriginalName();

        // 获取浏览器请求头代理人，用于判断浏览器厂商及版本
        String agent = fileServerHttpRequest.getRequest().headers().get("USER-AGENT");
        if (agent != null && agent.toLowerCase().contains("firefox")) {
            originalFileName = new StringBuffer("=?UTF-8?B?").append(new String(Base64Utils.encode(originalFileName.getBytes("UTF-8")))).append("?=").toString();
        } else {
            originalFileName = URLEncoder.encode(originalFileName, "UTF-8");
        }

        // 生成响应头
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, FileServerConstants.CACHE_CONTROL_MAX_AGE);
        response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, FileServerConstants.CONTENT_DISPOSITION + originalFileName);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, downloadFile.length());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, FileContentTypeUtil.getContentTypeByFile(downloadFile));
        response.headers().set(HttpHeaderNames.ACCEPT_RANGES, FileServerConstants.ACCEPT_RANGES);

        // 若需要保持连接则设置相应的响应头
        if (HttpUtil.isKeepAlive(fileServerHttpRequest.getRequest())) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // 判断是否需要断点续传
        String range = fileServerHttpRequest.getRequest().headers().get(HttpHeaderNames.RANGE);
        if (StringUtils.isNotBlank(range)) {

            // 计算断点续传的起始及结束位置
            long fileLength = downloadFile.length();
            long rangeStart = 0;
            long rangeEnd = fileLength - 1;
            String[] rangeArr = range.split("=")[1].split("-");
            if (rangeArr.length == 1) {
                rangeStart = Long.valueOf(rangeArr[0]);
            } else if (rangeArr.length == 2) {
                rangeStart = Long.valueOf(rangeArr[0]);
                rangeEnd = Long.valueOf(rangeArr[1]);
            }

            // 设置响应头
            response.setStatus(HttpResponseStatus.PARTIAL_CONTENT);
            response.headers().set(HttpHeaderNames.CONTENT_RANGE, new StringBuffer(FileServerConstants.ACCEPT_RANGES).append(" ").append(rangeStart).append("-").append(rangeEnd).append("/").append(fileLength).toString());
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, rangeEnd - rangeStart + 1);
        }

        // 生成自定义响应对象
        FileServerHttpResponse fileServerHttpResponse = new FileServerHttpResponse();
        fileServerHttpResponse.setFinished(true);
        fileServerHttpResponse.setResponse(response);
        fileServerHttpResponse.setFileDownload(commonFile.getStorePath());
        return fileServerHttpResponse;
    }
}
