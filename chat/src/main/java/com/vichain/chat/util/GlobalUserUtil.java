package com.vichain.chat.util;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局工具类
 * @author: Mr.Joe
 * @create:
 */
public class GlobalUserUtil {
    //保存全局的  连接上服务器的客户
    public static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public static ConcurrentHashMap<String, Channel> userChannelInfo = new ConcurrentHashMap<>();

    public static boolean hasChannel(String id) {
        Channel channel = userChannelInfo.get(id);
        return channel != null;
    }

    public static void sendTextMessage(String sendId, String receiverId, String message, Channel channel) {
        Channel receiverChannel = userChannelInfo.get(receiverId);
        if (receiverChannel == null) {
            userChannelInfo.get(sendId).writeAndFlush(new TextWebSocketFrame("对方不在线"));
            return;
        }
        receiverChannel.writeAndFlush(new TextWebSocketFrame(new Date().toString() + message));
    }
}
