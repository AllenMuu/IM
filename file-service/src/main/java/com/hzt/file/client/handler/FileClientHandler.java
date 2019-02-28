package com.hzt.file.client.handler;

import com.hzt.file.client.constant.FileClientConstants;
import com.hzt.file.client.util.FileClientUtil;
import com.hzt.file.client.vo.FileClientHttpResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class FileClientHandler extends ChannelInboundHandlerAdapter {
	
	private Logger logger = LoggerFactory.getLogger(FileClientHandler.class);

    private FileClientHttpResponse fileClientHttpResponse = new FileClientHttpResponse();

    @Override
	public void channelRead(ChannelHandlerContext channelHandlerContext, Object message) {

        // 1. 若是响应头分片信息
    	if (message instanceof HttpResponse) {

            // 获取响应头对象
            HttpResponse response = (HttpResponse) message;

            // 判断响应码
            if(!response.status().equals(HttpResponseStatus.OK)){
                throw new RuntimeException("服务器返回错误响应码：" + response.status());
            }

            // 判断请求是否转换成功
            if (!response.decoderResult().isSuccess()) {
                throw new RuntimeException("响应格式有误，无法解析！");
            }

            // 设置响应对象
            fileClientHttpResponse.setResponse(response);

            // 获取响应头类型
            String contentType = response.headers().get(HttpHeaderNames.CONTENT_TYPE);
            System.out.println(contentType);

            // 1.1 若响应格式为HTML或者JSON
            if(contentType.equals(HttpHeaderValues.APPLICATION_JSON.toString()) || contentType.startsWith(FileClientConstants.CONTENT_TYPE_TEXT)){

                // 初始化响应字节数组
                if(fileClientHttpResponse.getResultByteList() == null){
                    fileClientHttpResponse.setResultByteList(new ArrayList<byte[]>());
                }

                // 设置文件标志位
                fileClientHttpResponse.setFile(false);
            }

            // 1.2 若以文件流的形式返回
            if(contentType.equals(HttpHeaderValues.APPLICATION_OCTET_STREAM.toString())){

                // 获取文件响应头
                String contentDisposition = response.headers().get(HttpHeaderNames.CONTENT_DISPOSITION);

                // 校验文件名对象
                if(StringUtils.isBlank(contentDisposition)){
                    throw new RuntimeException("响应格式有误，无法解析！");
                }

                // 获取文件名称
                String originalFileName = contentDisposition.substring(contentDisposition.indexOf("=") + 1);
                if(StringUtils.isBlank(originalFileName)){
                    throw new RuntimeException("响应格式有误，无法解析！");
                }

                // 设置文件名、文件路径
                fileClientHttpResponse.setDownFileName(originalFileName);

                // 开始初始化文件输出流
                try {

                    // 创建文件对象
                    String filePath = FileClientConstants.CLIENT_STORAGE_PATH + originalFileName;
                    File file = new File(filePath);
                    if(file.exists()){
                        file.delete();
                    }
                    file.createNewFile();

                    // 创建文件输出流
                    fileClientHttpResponse.setFileOutputStream(new FileOutputStream(file));

                } catch (Exception e) {
                    throw new RuntimeException("响应文件路径有误，无法解析！", e);
                }

                // 设置文件标志位
                fileClientHttpResponse.setFile(true);
            }

            // 1.3 操作结束
            return;
        }

        // 2. 若是响应体分片消息
        if (message instanceof HttpContent) {

            // 2.1 若读取到的是响应体分片
            ByteBuf content = ((HttpContent) message).content();
            fileClientHttpResponse.setContentLength(fileClientHttpResponse.getContentLength() + content.readableBytes());

            // 2.2 文件形式返回
            if (fileClientHttpResponse.isFile()) {
                try {

                    // 汇总数据
                    byte[] contentChunk = new byte[content.readableBytes()];
                    while (content.isReadable()) {
                        content.readBytes(contentChunk);
                        fileClientHttpResponse.getFileOutputStream().write(contentChunk);
                    }

                    // 写出文件
                    fileClientHttpResponse.getFileOutputStream().flush();

                } catch (Exception e) {
                    throw new RuntimeException("响应文件写出异常！", e);
                }
            }

            // 2.3 若返回的是文本，则以字符流的形式输出，且限制大小为128KB
            if (!fileClientHttpResponse.isFile()) {

                // 响应体最大支持128KB
                if (fileClientHttpResponse.getContentLength() > 1024 * 128) {
                    throw new RuntimeException("响应体超大，无法解析！");
                }

                // 保存本次的数据
                byte[] contentChunk = new byte[content.readableBytes()];
                while (content.isReadable()) {
                    content.readBytes(contentChunk);
                    fileClientHttpResponse.getResultByteList().add(contentChunk);
                }
            }

            // 2.4 若读取到结束标志分片（即所有分片均已读取完毕），则释放相关资源
            if (message instanceof LastHttpContent) {

                // 2.1 输出非文件的提示信息
                try {
                    if (!fileClientHttpResponse.isFile()) {

                        // 汇总响应分片
                        byte[] destArray = new byte[(int) fileClientHttpResponse.getContentLength()];
                        int destLen = 0;
                        for (int i = 0; i < fileClientHttpResponse.getResultByteList().size(); i++) {
                            System.arraycopy(fileClientHttpResponse.getResultByteList().get(i), 0, destArray, destLen, fileClientHttpResponse.getResultByteList().get(i).length);
                            destLen = destLen + fileClientHttpResponse.getResultByteList().get(i).length;
                        }

                        // 输出提示信息
                        logger.info(new String(destArray, "UTF-8"));
                    }

                } catch (Exception e) {
                    throw new RuntimeException("文件服务器客户端响应解析异常！", e);
                } finally {

                    // 释放相关资源
                    FileClientUtil.resourceRelease(fileClientHttpResponse);
                    ReferenceCountUtil.release(message, ReferenceCountUtil.refCnt(message));
                    fileClientHttpResponse = null;
                }
            }

            // 3.4 释放相关资源
            if( ReferenceCountUtil.refCnt(message) > 0){
                ReferenceCountUtil.release(message, ReferenceCountUtil.refCnt(message));
            }
        }
    }  

    @Override  
    public void channelInactive(ChannelHandlerContext ctx) {
    	logger.info("HTTP文件上传、下载通道已关闭！");
    }  
  
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e)  {
    	logger.error("文件上传、下载异常！原因如下：", e);
        ctx.close();
    }
}  
