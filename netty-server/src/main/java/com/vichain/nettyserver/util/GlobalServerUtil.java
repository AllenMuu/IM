package com.vichain.nettyserver.util;

import io.netty.channel.Channel;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局服务工具类
 * @author : QIAOMU
 * @since :2019-03-01
 */
public class GlobalServerUtil {
    /**
     * map中的key:代表服务的ip和端口号
     * map中的value:代表服务的channel(通道)
     */
    public static ConcurrentHashMap<String, Channel> serverChannelInfo = new ConcurrentHashMap<>();

    /**
     * 判断此IP是否连接了服务器
     * @param ip
     * @return
     */
    public static boolean hasChannel(String ip) {
        Channel channel = serverChannelInfo.get(ip);
        return channel != null;
    }

    /**
     * 服务掉线,移除channel
     * @param ip
     */
    public static void removeChannel(String ip) {
        serverChannelInfo.remove(ip);
    }

    /**
     * 发送非二进制的消息
     * @param sendId
     * @param receiverId
     * @param message
     * @param channel
     */
    public static void sendTextMessage(String sendId, String receiverId, String message, Channel channel) {
        // 发送消息

    }
}
