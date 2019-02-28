package com.hzt.file.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

public class FileServerAuthenticationHandler extends ChannelInboundHandlerAdapter {

    private static List<String> IPList = Arrays.asList("127.0.0.1", "192.168.2.120");

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String IP = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        if(!IPList.contains(IP)){
            // ctx.close();
        }
    }
}
