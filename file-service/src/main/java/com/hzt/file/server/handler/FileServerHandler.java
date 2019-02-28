package com.hzt.file.server.handler;

import com.hzt.common.base.domain.Result;
import com.hzt.common.constant.Constants;
import com.hzt.common.exception.BusinessException;
import com.hzt.file.server.controller.FileServerController;
import com.hzt.file.server.domain.vo.FileServerHttpRequest;
import com.hzt.file.server.domain.vo.FileServerHttpResponse;
import com.hzt.file.server.util.FileServerUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class FileServerHandler extends ChannelInboundHandlerAdapter {

    // 日志记录器
    private Logger logger = LoggerFactory.getLogger(FileServerHandler.class);

    // 自定义请求对象
    private FileServerHttpRequest fileServerHttpRequest = new FileServerHttpRequest();

    /**
     * 接受到数据（请求）的处理方法
     */
    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object message) throws Exception {

        try {

            // 1. 解析uri路径
            FileServerUtil.sanitizeHttpRequest(message, fileServerHttpRequest);

            // 2. 获取对应的请求处理类
            FileServerController fileServerController = FileServerUtil.getHandler(fileServerHttpRequest.getRequestUrl());

            // 3. 开始业务处理，得到响应；若响应不存在，则表示还未处理完成
            FileServerHttpResponse fileServerHttpResponse = fileServerController.doExecute(message, fileServerHttpRequest);
            if (fileServerHttpResponse == null) {
                return;
            }

            // 4. 写出响应
            if (fileServerHttpResponse.isFinished()) {
                fileServerHttpRequest.setFileServerHttpResponse(fileServerHttpResponse);
                FileServerUtil.writeResponse(channelHandlerContext, fileServerHttpResponse.getResponse());
            }

            // 5. 若是文件下载请求，则还需写出文件
            if (!StringUtils.isBlank(fileServerHttpResponse.getFileDownload())) {
                FileServerUtil.writeFile(channelHandlerContext, fileServerHttpResponse);
            }

            // 6. 写出结束分片
            ChannelFuture channelFuture = channelHandlerContext.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

            // 7. 若不是长连接则关闭
            if (!HttpUtil.isKeepAlive(fileServerHttpRequest.getRequest())) {
                channelFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } finally {

            // 释放资源本次读取的资源
            ReferenceCountUtil.release(message, ReferenceCountUtil.refCnt(message));

            // 请求响应完成，释放本次请求相关资源，重置连接对象及其参数
            if (fileServerHttpRequest != null && fileServerHttpRequest.getFileServerHttpResponse() != null && fileServerHttpRequest.getFileServerHttpResponse().isFinished()) {
                FileServerUtil.resourceRelease(fileServerHttpRequest);
                fileServerHttpRequest = new FileServerHttpRequest();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext channelHandlerContext) {
        fileServerHttpRequest = null;
        logger.info("HTTP连接已关闭！");
    }

    /**
     * 服务异常处理方法
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) {

        // 1. 记录日志
        logger.error("文件服务业务处理异常！原因如下：", throwable);

        // 2. 若连接已断开，则直接关闭连接
        if (throwable instanceof IOException) {
            FileServerUtil.resourceRelease(fileServerHttpRequest);
            fileServerHttpRequest = null;
            channelHandlerContext.close();
            return;
        }

        // 获取本次请求方法
        HttpMethod method = fileServerHttpRequest.getRequest().method();

        // 初始化默认错误提示信息
        String errorCode = method.equals(HttpMethod.POST) ? Constants.FILE_UPLOAD_FAIL : method.equals(HttpMethod.GET) ? Constants.FILE_DOWNLOAD_FAIL : Constants.OPERATION_FAIL;
        String errorMessage = method.equals(HttpMethod.POST) ? "文件上传失败！" : method.equals(HttpMethod.GET) ? "文件下载失败！" : Constants.OPERATION_ERROR_MESSAGE;

        // 若异常自带错误码，则返回异常的错误码
        if (throwable instanceof BusinessException) {
            errorCode = StringUtils.isBlank(errorCode = ((BusinessException) throwable).getCode()) ? Constants.OPERATION_FAIL : errorCode;
            errorMessage = StringUtils.isBlank(errorMessage = throwable.getMessage()) ? Constants.OPERATION_ERROR_MESSAGE : errorMessage;
        }

        // 3. 其他异常，先写出提示，再关闭连接
        if (channelHandlerContext.channel().isActive()) {
            FileServerUtil.writeErrorResponse(channelHandlerContext, fileServerHttpRequest, Result.createWithErrorMessage(errorMessage, errorCode));
        }

        // 4. 释放相关资源
        FileServerUtil.resourceRelease(fileServerHttpRequest);
    }
}