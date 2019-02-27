package com.vichain.chat.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @description: 处理心跳
 * @author: Mr.Joe
 * @create:
 */
public class HeartBeatHandler extends ChannelHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            PingWebSocketFrame ping = new PingWebSocketFrame();
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                ctx.writeAndFlush(ping);
                System.out.println("进入(客户端)读空闲。。。");
            } else if (state == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(ping);
                System.out.println("进入(服务端)写空闲。。。");
            } else if (state == IdleState.ALL_IDLE) {
                //关闭无用的channel 以防资源浪费
                Channel channel = ctx.channel();
                channel.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}