package com.hzt.file.server.util;

import com.alibaba.fastjson.JSONObject;
import com.hzt.common.base.domain.Result;
import com.hzt.common.constant.Constants;
import com.hzt.common.util.ExceptionUtil;
import com.hzt.common.util.JsonUtil;
import com.hzt.file.server.config.FileServerConfig;
import com.hzt.file.server.constant.FileServerConstants;
import com.hzt.file.server.controller.FileServerController;
import com.hzt.file.server.domain.vo.FileServerHttpRequest;
import com.hzt.file.server.domain.vo.FileServerHttpResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLDecoder;

public class FileServerUtil {

    /**
     * 请求uri校验
     * @param message               消息
     * @param fileServerHttpRequest 自定义请求对象
     * @return HttpRequest返回true，否则false
     */
    public static void sanitizeHttpRequest(Object message, FileServerHttpRequest fileServerHttpRequest) {

        try {

            // 若不是Http请求对象，则一律返回null
            if (!(message instanceof HttpRequest)) {
                return;
            }

            // 获取请求对象
            HttpRequest request = (HttpRequest) message;

            // 获取原始uri
            String uri = request.uri();

            // 解码请求uri，失败则禁止请求
            String requestUri = URLDecoder.decode(uri, "UTF-8");

            // 若请求链接为空
            if (StringUtils.isEmpty(requestUri)) {
                ExceptionUtil.rollback("请求链接不可为空！", Constants.OPERATION_FAIL);
            }

            // 截取请求链接，不包含参数
            int index = requestUri.indexOf("?");
            requestUri = index > -1 ? requestUri.substring(0, index) : requestUri;

            // 设置请求对象
            fileServerHttpRequest.setRequest(request);

            // 解析请求参数
            fileServerHttpRequest.getParameterMap().putAll(RequestParser.parse(request));

            // 设置请求链接
            fileServerHttpRequest.setRequestUrl(requestUri);

        } catch (Exception e) {
            ExceptionUtil.rollback("请求地址解析失败！", Constants.OPERATION_FAIL, e);
        }
    }

    /**
     * 根据请求对象获取对应的处理类对象
     * @param uri 请求链接
     * @return Handler
     */
    public static FileServerController getHandler(String uri) {

        // 获取请求处理类
        FileServerController fileServerController = FileServerConfig.getHandler(uri);

        // 若该Handler不存在，抛出异常
        if (fileServerController == null) {
            ExceptionUtil.rollback("URI对应的Handler不存在！", Constants.OPERATION_FAIL);
        }

        // 返回结果
        return fileServerController;
    }

    /**
     * 写出文件
     * @param channelHandlerContext
     * @param fileServerHttpResponse
     */
    public static void writeFile(ChannelHandlerContext channelHandlerContext, FileServerHttpResponse fileServerHttpResponse) {

        // 声明可分片写出文件对象
        RandomAccessFile randomAccessFile;
        ChunkedFile chunkedFile;

        try {

            // 校验文件是否合法存在，不存在则提示404
            File file = new File(fileServerHttpResponse.getFileDownload());
            if (!file.exists() || !file.isFile() || file.isHidden()) {
                ExceptionUtil.rollback("请求下载的文件不存在！", Constants.FILE_DOWNLOAD_FAIL);
            }

            // 设置文件分片起起始与终止位置
            long rangeStart = 0;
            long rangeEnd = file.length() - 1;

            // 判断是否需要分片下载
            String rangeStr = fileServerHttpResponse.getResponse().headers().get(HttpHeaderNames.CONTENT_RANGE);
            if (StringUtils.isNotBlank(rangeStr)) {
                String[] rangeArr = rangeStr.substring(rangeStr.indexOf(" ") + 1, rangeStr.indexOf("/")).split("-");
                rangeStart = Long.valueOf(rangeArr[0]);
                rangeEnd = Long.valueOf(rangeArr[1]);
            }

            // 判断文件长度决定分片大小
            long chunkLength = rangeEnd - rangeStart + 1;
            int chunkSize = chunkLength > FileServerConstants.CONTENT_RANGE_BUFF_LENGTH ? FileServerConstants.CONTENT_RANGE_BUFF_LENGTH : (int) chunkLength;

            // 初始化可分片写出文件对象
            randomAccessFile = new RandomAccessFile(fileServerHttpResponse.getFileDownload(), "r"); // 只读
            chunkedFile = new ChunkedFile(randomAccessFile, rangeStart, rangeEnd - rangeStart + 1, chunkSize);

            // 写出文件
            ChannelFuture channelFuture = channelHandlerContext.writeAndFlush(chunkedFile, channelHandlerContext.newProgressivePromise());

            // 自定义监听器，主要用于资源释放
            channelFuture.addListener((ChannelFutureListener) channelFutureListener -> {

                // 关闭文件系统资源
                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (Exception e) {
                    }
                }

                // 关闭文件系统资源
                if (chunkedFile != null) {
                    try {
                        chunkedFile.close();
                    } catch (IOException e) {
                    }
                }
            });

        } catch (Exception e) {
            ExceptionUtil.rollback("请求下载的文件不存在！", Constants.FILE_DOWNLOAD_FAIL, e);
        }
    }

    /**
     * 写出响应
     * @param channelHandlerContext
     * @param response
     */
    public static ChannelFuture writeResponse(ChannelHandlerContext channelHandlerContext, HttpResponse response) {

        // 统一设置跨域响应头
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, FileServerConstants.ACCESS_CONTROL_ALLOW_ORIGIN);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, FileServerConstants.ACCESS_CONTROL_ALLOW_METHODS);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, FileServerConstants.ACCESS_CONTROL_ALLOW_HEADERS);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, FileServerConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, FileServerConstants.ACCESS_CONTROL_MAX_AGE);

        // 写出响应
        return channelHandlerContext.writeAndFlush(response);
    }

    /**
     * 业务处理异常响应方法
     * @param context
     * @param request
     * @param result
     */
    public static void writeErrorResponse(ChannelHandlerContext context, FileServerHttpRequest request, Result result) {

        // 声明响应内容，默认为空
        String resultStr = Constants.BLANK_STRING;

        // 获取请求的方法类型
        HttpMethod method = request.getRequest().method();

        // 根据方法生成对应的响应体内容
        if (method.equals(HttpMethod.POST)) {
            resultStr = JsonUtil.toJsonString(result);
        } else if (method.equals(HttpMethod.GET)) {
            JSONObject message = new JSONObject();
            message.put("target", request.getParameterMap().get("target"));
            message.put("message", result.getMessage());
            resultStr = new StringBuffer("<script>parent.postMessage(").append(message).append(", '*')</script>").toString();
        }

        // 生成响应体对象
        ByteBuf byteBuf = Unpooled.copiedBuffer(resultStr, CharsetUtil.UTF_8);

        // 生成响应对象
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, method.equals(HttpMethod.POST) ? FileServerConstants.CONTENT_TYPE_JSON : FileServerConstants.CONTENT_TYPE_HTML);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, byteBuf.readableBytes());

        // 写出响应并关闭连接
        writeResponse(context, response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 请求资源释放
     * @param fileServerHttpRequest
     */
    public static void resourceRelease(FileServerHttpRequest fileServerHttpRequest) {

        if (ReferenceCountUtil.refCnt(fileServerHttpRequest.getDecoder()) > 0) {
            fileServerHttpRequest.getDecoder().destroy();
            fileServerHttpRequest.setDecoder(null);
        }

        // 释放请求对象
        if (ReferenceCountUtil.refCnt(fileServerHttpRequest.getRequest()) > 0) {
            ReferenceCountUtil.release(fileServerHttpRequest.getRequest(), ReferenceCountUtil.refCnt(fileServerHttpRequest.getRequest()));
        }
    }
}
