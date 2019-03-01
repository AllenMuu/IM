package com.vichain.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import javax.net.ssl.SSLEngine;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: Mr.Joe
 * @create:
 */
public class WebSocketClient extends Thread {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8388;

    private EventLoopGroup group = new NioEventLoopGroup(8);

    public static Channel channel = null;

    private boolean isShutDown = false;

    // 单例示例对象
    private static WebSocketClient webSocketClient = new WebSocketClient();

    /**
     * 私有化构造函数，保证单例
     */
    private WebSocketClient() {
    }

    /**
     * 获取单例实例对象
     * @return
     */
    public static WebSocketClient getInstance() {
        return webSocketClient;
    }

    @Override
    public void run() {

        while (true) {

            try {

                Bootstrap client = new Bootstrap();
                client.group(group).channel(NioSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .channel(NioServerSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {

                            @Override
                            protected void initChannel(SocketChannel socketChannel) throws Exception {

                                ChannelPipeline pipeline = socketChannel.pipeline();


                                pipeline.addLast(new HttpServerCodec())
                                        .addLast(new ChunkedWriteHandler())
                                        .addLast(new HttpObjectAggregator(1024 * 64))
                                        .addLast(new IMClientHandler());
                            }
                        });

                // 启动服务器
                ChannelFuture ChannelFuture = client.connect(HOST, PORT).sync();

                // 获取Channel
                channel = ChannelFuture.channel();

                // 线程阻塞直到连接关闭
                channel.closeFuture().sync();

                // 服务端停止服务，重启
                throw new Exception("文件服务器客户端已关闭，等待重启...");

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (isShutDown) {
                break;
            }

            // 5s 后重启服务
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void shutdownClient() {

        isShutDown = true;

        // 等待数据的传输通道关闭
        group.shutdownGracefully();
    }

}
