package com.hzt.file.server.server;

import com.hzt.common.util.RequestIpUtil;
import com.hzt.file.server.handler.FileServerAuthenticationHandler;
import com.hzt.file.server.handler.FileServerHandler;
import com.hzt.file.server.util.FileServerUtil;
import com.hzt.file.server.util.SecureChatSslContextFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;

public final class FileServer extends Thread {

    private Logger logger = LoggerFactory.getLogger(FileServer.class);

    private static final int port = 8400;

    private static final boolean SSL = false;

    private EventLoopGroup connectionGroup = new NioEventLoopGroup(8); // 处理连接的线程
    private EventLoopGroup workerGroup = new NioEventLoopGroup(80); // 处理业务数据的线程

    private boolean isShutDown = false;

    // 单例示例对象
    private static FileServer socketServer = new FileServer();

    /**
     * 私有化构造函数，保证单例
     */
    private FileServer() {
    }

    /**
     * 获取单例实例对象
     *
     * @return
     */
    public static FileServer getInstance() {
        return socketServer;
    }

    /**
     * 关闭文件服务器
     */
    public void shutdown() {

        // 修改标记
        isShutDown = true;

        // 连接线程组关闭
        if (connectionGroup != null) {
            connectionGroup.shutdownGracefully();
        }

        // 工作线程组关闭
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void run() {

        while (true) {

            try {

                // 开始初始化服务器，对ServerChannel的封装
                ServerBootstrap serverBootstrap = new ServerBootstrap();
                serverBootstrap.group(connectionGroup, workerGroup);
                serverBootstrap.channel(NioServerSocketChannel.class);
                serverBootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(1024 * 8, 1024 * 64, 1024 * 512)); // 设置默认缓冲区块大小：最小8K，默认初始化64K，最大512K
                serverBootstrap.option(ChannelOption.SO_BACKLOG, 12000); // 最多有几个请求排队(用于临时存放已完成三次握手的请求的队列的最大长度，最好限制为合理数值，防止连接过多导致系统资源耗尽)
                serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() { // ChannelHandler: 处理自己感兴趣的事件，ChannelPipeline为处理事件的容器，其实这里就是Netty事件驱动的实现。

                    @Override
                    protected void initChannel(SocketChannel socketChannel) {

                        // 获取管道对象
                        ChannelPipeline pipeline = socketChannel.pipeline();

                        // 给管道设置各类Handler用于数据处理：注意Handler是有序的
                        if (SSL) {
                            SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
                            engine.setUseClientMode(false);  // 运行模式：true：客户端模式，false：服务端模式
                            engine.setNeedClientAuth(false); // 是否需要双向认证
                            pipeline.addFirst("SSL", new SslHandler(engine));
                        }
                        pipeline.addLast("authentication", new FileServerAuthenticationHandler()); // IP 白名单认证
                        pipeline.addLast("http-decoder", new HttpRequestDecoder()); // http请求解码
                        pipeline.addLast("http-encoder", new HttpResponseEncoder()); // http响应编码
                        pipeline.addLast("http-chunked", new ChunkedWriteHandler()); // 大数据分片写出
                        pipeline.addLast("fileServerHandler", new FileServerHandler()); // 自定义业务处理
                    }
                });

                // 启动服务器
                ChannelFuture ChannelFuture = serverBootstrap.bind(port).sync();

                // 获取Channel
                Channel channel = ChannelFuture.channel();

                // 服务器启动成功
                logger.info("HTTP 文件服务器启动, 地址是：" + "http://" + RequestIpUtil.getLocalIp() + ":" + port + "/file");

                // 线程阻塞直到连接关闭
                channel.closeFuture().sync();

                // 服务端停止服务，重启
                throw new Exception("文件服务器已关闭，等待重启...");

            } catch (Exception e) {
                logger.error("文件服务器异常关闭，原因如下：", e);
            }

            // 若系统关闭，则关闭
            if (isShutDown) {
                break;
            }

            // 5s 后重启服务
            try {
                logger.error("文件服务器将于5s后重启...");
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.error("文件服务器重启...");
            }
        }
    }
}

