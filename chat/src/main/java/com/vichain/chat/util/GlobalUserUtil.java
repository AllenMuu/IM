package com.vichain.chat.util;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @description:
 * @author: Mr.Joe
 * @create:
 */
public class GlobalUserUtil {
    //保存全局的  连接上服务器的客户
    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
}
