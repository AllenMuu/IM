package com.hzt.file.server.util;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 请求参数解析器，仅支持请求的URL参数解析
 */
public class RequestParser {

    /**
     * 解析请求参数
     * @return 包含所有请求参数的键值对, 如果没有参数, 则返回空 Map
     */
    public static Map<String, String> parse(Object message) {
    	
    	// 初始化参数集容器
    	Map<String, String> paramMap = new LinkedHashMap<String, String>();
    	
    	// 参数不合法，则返回空Map
    	if(message == null){
    		return paramMap;
    	}

        if(message instanceof HttpRequest) {

    	    HttpRequest request = (HttpRequest) message;

            // 解析参数
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());

            // 设置参数集
            for (Map.Entry<String, List<String>> entry : queryStringDecoder.parameters().entrySet()) {
                paramMap.put(entry.getKey(), entry.getValue().get(0));
            }
        }

        // 解析完成，返回结果列表
        return paramMap;
    }
}