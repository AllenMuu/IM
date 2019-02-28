package com.hzt.file.client.util;

import com.hzt.file.client.client.FileClient;
import com.hzt.file.client.vo.FileClientHttpResponse;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class FileClientUtil {

	private Logger logger = LoggerFactory.getLogger(FileClientUtil.class);

	/**
	 * 将指定文件上传至文件服务器
	 * @param path
	 */
	public static void uploadFile(String path) {
		
		if (path == null) {
			throw new RuntimeException("上传文件的路径不能为null...");
		}
		
		File file = new File(path);
		if (!file.canRead()) {
			throw new RuntimeException(file.getName() + "不可读...");
		}
		
		if (file.isHidden() || !file.isFile()) {
			throw new RuntimeException(file.getName() + "不存在...");
		}
		
		HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

		try {

			HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/file/upload");
			request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
			HttpPostRequestEncoder bodyRequestEncoder = new HttpPostRequestEncoder(factory, request, false);
			bodyRequestEncoder.addBodyAttribute("form", "POST");
			bodyRequestEncoder.addBodyFileUpload("file", file, "application/x-zip-compressed", false);

			List<InterfaceHttpData> bodyList = bodyRequestEncoder.getBodyListAttributes();
			if (bodyList == null) {
				throw new RuntimeException("请求体不存在...");
			}

			HttpRequest request2 = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/file/upload");
			HttpPostRequestEncoder bodyRequestEncoder2 = new HttpPostRequestEncoder(factory, request2, true);

			bodyRequestEncoder2.setBodyHttpDatas(bodyList);
			bodyRequestEncoder2.finalizeRequest();

			Channel channel = FileClient.channel;
			if (channel.isActive() && channel.isWritable()) {
				channel.writeAndFlush(request2);

				if (bodyRequestEncoder2.isChunked()) {
					channel.writeAndFlush(bodyRequestEncoder2).awaitUninterruptibly();
					channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
				}

				bodyRequestEncoder2.cleanFiles();
			}
			
		} catch (Exception e) {
			throw new RuntimeException("文件上传出错！", e);
		} finally {
			factory.cleanAllHttpData();
		}
	}

	/**
	 * 从文件服务器下载指定文件
	 * @param fileId
	 */
	public static void downloadFile(Long fileId) {

		if (fileId == null) {
			throw new RuntimeException("下载文件的主键不能为null...");
		}
		
		try {
			
			HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/file/download?fileId=" + fileId);
			request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

			Channel channel = FileClient.channel;
			if (channel.isActive() && channel.isWritable()) {
				channel.writeAndFlush(request);
				channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
			}
			
		} catch (Exception e) {
			throw new RuntimeException("文件下载出错！", e);
		}
	}

	/**
	 * 释放响应资源
	 * @param fileClientHttpResponse
	 */
	public static void resourceRelease(FileClientHttpResponse fileClientHttpResponse){

		// 释放字节数组资源
		if (fileClientHttpResponse.getResultByteList() != null) {
			fileClientHttpResponse.getResultByteList().clear();
			fileClientHttpResponse.setResultByteList(null);
		}

		// 释放文件流相关资源
		if (fileClientHttpResponse.getFileOutputStream() != null) {
			try {
				fileClientHttpResponse.getFileOutputStream().close();
			} catch (Exception e) {
			}
			fileClientHttpResponse.setFileOutputStream(null);
		}

		// 释放响应对象
		if(ReferenceCountUtil.refCnt(fileClientHttpResponse.getResponse()) > 0){
			ReferenceCountUtil.release(fileClientHttpResponse.getResponse(), ReferenceCountUtil.refCnt(fileClientHttpResponse.getResponse()));
		}
	}
}
