package com.vichain.nettyserver.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;


/**
 * 考虑反射:
 * 由于在调用 SingletonHolder.instance 的时候，才会对单例进行初始化，而且通过反射，是不能从外部类获取内部类的属性的。
 * 所以这种形式，很好的避免了反射入侵。
 * 考虑多线程：
 * 由于静态内部类的特性，只有在其被第一次引用的时候才会被加载，所以可以保证其线程安全性。
 * 不需要传参的情况下 优先考虑静态内部类
 * @author QIAOMU
 * @date 2019-02-27
 */
@Component
public class NettyServer {
    private ServerBootstrap server;

    private static class SingletonNettyServer {
        static final NettyServer instance = new NettyServer();
    }

    static NettyServer getInstance() {
        return SingletonNettyServer.instance;
    }

    public NettyServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        server = new ServerBootstrap();

        server.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ServerInitializer())
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
    }

    void start() {
        int port = 8388;
        ChannelFuture future = server.bind(port);
    }
}
