package com.vichain.chat.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;


/**
 *
 * 考虑反射:
 * 由于在调用 SingletonHolder.instance 的时候，才会对单例进行初始化，而且通过反射，是不能从外部类获取内部类的属性的。
 * 所以这种形式，很好的避免了反射入侵。
 * 考虑多线程：
 * 由于静态内部类的特性，只有在其被第一次引用的时候才会被加载，所以可以保证其线程安全性。
 * 不需要传参的情况下 优先考虑静态内部类
 * @author QIAOMU
 * @date 2019-02-27
 *
 */
@Component
public class WsServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap server;
    private ChannelFuture future;

    private static class SingletonWsServer{
        static final WsServer instance = new WsServer();
    }

    public static WsServer getInstance(){
        return SingletonWsServer.instance;
    }

    public WsServer() {
        bossGroup = new NioEventLoopGroup();
        workerGroup =new NioEventLoopGroup();
        server = new ServerBootstrap();

        server.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new WsServerInitializer());// 自定义初始化handler容器
        //.option(ChannelOption.SO_BACKLOG, 128)// 设置tcp协议的请求等待队列
        //.childOption(ChannelOption.SO_KEEPALIVE, true);
    }

        public void start(){
        //自定义端口8288
        this.future = server.bind(8288);
    }

}
