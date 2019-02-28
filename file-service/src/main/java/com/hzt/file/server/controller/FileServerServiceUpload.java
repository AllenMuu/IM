package com.hzt.file.server.controller;

import com.hzt.common.base.domain.Result;
import com.hzt.common.base.domain.vo.CommonFileVO;
import com.hzt.common.base.domain.vo.UserSessionVO;
import com.hzt.common.constant.Constants;
import com.hzt.common.context.BeanTool;
import com.hzt.common.util.BeanCopyUtil;
import com.hzt.common.util.DateUtil;
import com.hzt.common.util.ExceptionUtil;
import com.hzt.common.util.JsonUtil;
import com.hzt.common.util.KeyGeneratorUtil;
import com.hzt.common.util.RedisUtil;
import com.hzt.file.server.constant.FileServerConstants;
import com.hzt.file.server.domain.po.CommonFile;
import com.hzt.file.server.domain.vo.FileServerHttpRequest;
import com.hzt.file.server.domain.vo.FileServerHttpResponse;
import com.hzt.file.server.service.FileService;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static io.netty.buffer.Unpooled.copiedBuffer;

@Component("FileServerControllerUpload")
public class FileServerServiceUpload implements FileServerController {

    // 默认Http解码器工厂对象
    private static final HttpDataFactory FACTORY = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

    @Override
    public FileServerHttpResponse doExecute(Object message, FileServerHttpRequest fileServerHttpRequest) throws Exception {

        // 1. 若是请求头
        if (message instanceof HttpRequest) {

            // 获取请求头对象
            HttpRequest request = (HttpRequest) message;

            // 文件上传强制为 POST 请求
            if (!request.method().equals(HttpMethod.POST) && !request.method().equals(HttpMethod.OPTIONS)) {
                ExceptionUtil.rollback("请求方法不允许！", Constants.FILE_UPLOAD_FAIL);
            }

            // 若请求方法是OPTION方法，则直接响应200
            if (fileServerHttpRequest.getRequest().method().equals(HttpMethod.OPTIONS)) {
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
                FileServerHttpResponse fileServerHttpResponse = new FileServerHttpResponse();
                fileServerHttpResponse.setResponse(response);
                fileServerHttpResponse.setFinished(true);
                return fileServerHttpResponse;
            }

            // 校验用户身份信息
            String token = request.headers().get(HttpHeaderNames.AUTHORIZATION);
            if (StringUtils.isBlank(token)) {
                ExceptionUtil.rollback("上传文件前请先登录！", Constants.NO_PRIVILEGE);
            }

            // 保存用户Session 信息
            fileServerHttpRequest.setUserSessionVO((UserSessionVO) RedisUtil.get(Constants.AUTH_SESSION_KEY + token));
            if (fileServerHttpRequest.getUserSessionVO() == null) {
                ExceptionUtil.rollback("登录已过期，请重新登录！", Constants.LOGIN_TIMEOUT);
            }

            // 解析请求
            fileServerHttpRequest.setDecoder(new HttpPostRequestDecoder(FACTORY, request));

            // 请求头处理完成，直接返回
            return null;
        }

        // 2. 若是请求分片，根据解析的请求及本次的请求分片读取分片信息
        if (message instanceof HttpContent && fileServerHttpRequest.getDecoder() != null) {
            try {

                // 将本次的分片数据与之前的数据合并
                fileServerHttpRequest.getDecoder().offer((HttpContent) message);

                // 继上次位置继续遍历分片数据
                while (fileServerHttpRequest.getDecoder().hasNext()) {

                    // 取出数据
                    HttpData httpData = (HttpData) fileServerHttpRequest.getDecoder().next();

                    try {

                        // 若文件读取完毕，则Hold 住该对象！注意：此处的资源释放需要待到文件写入硬盘指定位置成功后才可以释放，否则临时文件会直接被删除
                        if (httpData.isCompleted()) {

                            // 若传输的是文件
                            if (httpData instanceof FileUpload) {

                                // 获取上传的临时文件
                                FileUpload fileUpload = (FileUpload) httpData;

                                // 获取上传文件的原始文件名
                                String fileName = fileUpload.getFilename();
                                if (fileName.length() > FileServerConstants.FILE_NAME_LENGTH) {
                                    ExceptionUtil.rollback("文件名总长度不允许大于64位", Constants.FILE_NAME_TOO_LONG);
                                }

                                // 获取文件大小
                                Long size = fileUpload.length();
                                if (size > FileServerConstants.FILE_SIZE) {
                                    ExceptionUtil.rollback("文件不允许大于30MB", Constants.FILE_TOO_LARGE);
                                }

                                // 生成新文件存储名称
                                String storeName = new StringBuffer(String.valueOf(System.currentTimeMillis())).append("_").append(fileName).toString();

                                // 生成文件存储路径
                                StringBuffer filePath = new StringBuffer(FileServerConstants.FILE_STORAGE_PATH).append(Constants.PATH_SEPARATOR).append(DateUtil.formatDate(fileServerHttpRequest.getRequestTime(), DateUtil.DATE_FORMAT_MONTH));
                                File file = new File(filePath.toString());
                                if (!file.exists()) {
                                    file.mkdirs();
                                }

                                // 生成文件存储全路径
                                String storePath = filePath.append(Constants.PATH_SEPARATOR).append(storeName).toString();

                                // 操作无误，则将临时文件保存至硬盘目录
                                fileUpload.renameTo(new File(storePath));

                                // 文件保存成功后，准备插入数据库
                                CommonFile commonFile = new CommonFile();
                                commonFile.setCode(KeyGeneratorUtil.createUUID());
                                commonFile.setOriginalName(fileName);
                                commonFile.setStoreName(storeName);
                                commonFile.setStorePath(storePath);
                                commonFile.setExtend(storeName.indexOf(".") > -1 ? storeName.substring(storeName.lastIndexOf(".")) : null);
                                commonFile.setSize(size);
                                commonFile.setCreateBy(fileServerHttpRequest.getUserSessionVO().getUserId());
                                commonFile.setCreateTime(fileServerHttpRequest.getRequestTime());
                                commonFile.setUpdateBy(commonFile.getCreateBy());
                                commonFile.setUpdateTime(DateUtil.getCurrentTime());

                                // 保存文件索引
                                FileService fileService = BeanTool.getBean("FileService", FileService.class);
                                fileService.createFileIndex(commonFile);

                                // 响应结果中增加文件主键
                                List<CommonFileVO> fileList = new ArrayList<>();
                                fileList.add(BeanCopyUtil.copy(commonFile, CommonFileVO.class));
                                fileServerHttpRequest.getFileServerHttpResponse().setResult(Result.createWithModels("文件上传成功！", fileList));
                            }

                            // 若传输的是其他属性
                            if (httpData instanceof Attribute) {
                                Attribute attribute = (Attribute) httpData;
                                fileServerHttpRequest.getParameterMap().put(attribute.getName(), attribute.getValue());
                            }
                        }
                    } finally {
                        httpData.release(); // 释放资源
                    }
                }
            } catch (HttpPostRequestDecoder.EndOfDataDecoderException e) {
                // 无需处理，说明后续已经没有任何分片数据
            }

            // 2.1 若是最后请求分片
            if (message instanceof LastHttpContent) {

                // 生成响应提示
                String resultStr = JsonUtil.toJsonString(fileServerHttpRequest.getFileServerHttpResponse().getResult());

                // 将请求响应的内容转换成ChannelBuffer.
                ByteBuf resultBuf = copiedBuffer(resultStr, CharsetUtil.UTF_8);

                // 构建请求响应对象
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, resultBuf);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, FileServerConstants.CONTENT_TYPE_HTML);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, resultBuf.readableBytes());

                // 判断是否关闭请求响应连接
                if (HttpUtil.isKeepAlive(fileServerHttpRequest.getRequest())) {
                    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }

                // 生成并返回自定义响应对象
                FileServerHttpResponse fileServerHttpResponse = new FileServerHttpResponse();
                fileServerHttpResponse.setFinished(true);
                fileServerHttpResponse.setResponse(response);
                return fileServerHttpResponse;
            }
        }
        return null;
    }
}
