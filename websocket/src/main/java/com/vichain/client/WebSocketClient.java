package com.vichain.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.channel.socket.SocketChannel;
import org.springframework.stereotype.Component;

/**
 * @description:
 * @author: Mr.Joe
 * @create:
 */
@Component
public class WebSocketClient extends Thread {

    private static final String host = "127.0.0.1";
    private static final int port = 8333;


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

                Bootstrap bootstrap = new Bootstrap();
                bootstrap.option(ChannelOption.TCP_NODELAY, true);
                bootstrap.option(ChannelOption.SO_SNDBUF, 1024 * 1024 * 64); // 发送缓冲区大小
                bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
                bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
                bootstrap.group(group).channel(NioSocketChannel.class);
                bootstrap.handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {

                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("encoder", new HttpRequestEncoder());
                        pipeline.addLast("decoder", new HttpResponseDecoder());
                        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
                        pipeline.addLast("dispatcher", new WebSocketClientHandler());
                    }
                });

                // 启动服务器
                ChannelFuture ChannelFuture = bootstrap.connect(host, port).sync();

                // 获取Channel
                channel = ChannelFuture.channel();

                // 线程阻塞直到连接关闭
                channel.closeFuture().sync();

                // 服务端停止服务，重启
                throw new Exception("文件服务器客户端已关闭，等待重启...");

            } catch (Exception e) {
            }

            if (isShutDown) {
                break;
            }

            // 5s 后重启服务
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }


    public void shutdownClient() {

        isShutDown = true;

        // 等待数据的传输通道关闭
        group.shutdownGracefully();
    }
}
