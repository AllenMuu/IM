package com.vichain.util;

import com.vichain.client.WebSocketClient;
import io.netty.channel.Channel;

/**
 * 客户端工具类
 * @author : Mr.Joe
 * @date : 2019-03-01
 */
public class IMClientUtil {

    public void sendNettyMessage( String message) {
        Channel channel = WebSocketClient.channel;
        channel.writeAndFlush(message);
    }
}
