package com.vichain.chat.handle;

import com.vichain.chat.util.GlobalUserUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * webSocketHandler
 * @author QIAOMU
 * @date 2019-02-28
 */
public class ServerHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker handshaker;

    /**
     * channel 通道 action 活跃的
     * 当客户端主动链接服务端的链接后，这个通道就是活跃的了。也就是客户端与服务端建立了通信通道并且可以传输数据
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 添加

        GlobalUserUtil.clients.add(ctx.channel());
        GlobalUserUtil.clients.writeAndFlush(new TextWebSocketFrame(new Date().toString() + "当前用户数:" + GlobalUserUtil.clients.size()));
    }

    /**
     * channel 通道 Inactive 不活跃的
     * 当客户端主动断开服务端的链接后，这个通道就是不活跃的。也就是说客户端与服务端关闭了通信通道并且不可以传输数据
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 移除

        GlobalUserUtil.clients.remove(ctx.channel());
        GlobalUserUtil.clients.writeAndFlush(new TextWebSocketFrame(new Date().toString() + "==用户" + ctx.channel().id() + "退出" + "===当前用户数为" + GlobalUserUtil.clients.size()));
    }

    /**
     * 接收客户端发送的消息 channel 通道 Read 读
     * 简而言之就是从通道中读取数据，也就是服务端接收客户端发来的数据。但是这个数据在不进行解码时它是ByteBuf类型的
     */
    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) {

        // 传统的HTTP接入
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, ((FullHttpRequest) msg));
            // WebSocket接入
        } else if (msg instanceof WebSocketFrame) {
            handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    /**
     * channel 通道 Read 读取 Complete 完成 在通道读取完成后会在这个方法里通知，对应可以做刷新操作 ctx.flush()
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 判断是否关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        // 判断是否ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 判断是否为文本消息
        if (frame instanceof TextWebSocketFrame) {
            // 返回应答消息
            String request = ((TextWebSocketFrame) frame).text();
            if (StringUtils.isEmpty(request.trim())) {
                return;
            }
            String[] array = request.split(",");
            String userId = array[0];
            String receiverId = array[1];
            String message = array[2];
            if (StringUtils.isEmpty(userId)) {
                return;
            }
            if (!GlobalUserUtil.hasChannel(userId)) {
                GlobalUserUtil.userChannelInfo.put(userId, ctx.channel());
            }
            if (StringUtils.isEmpty(message.trim())) {
                // 返回【谁发的发给谁】
                ctx.channel().writeAndFlush("发送内容不能为空!");
                return;
            }
            if (StringUtils.isEmpty(receiverId)){
                // 发送给所有人
                GlobalUserUtil.clients.writeAndFlush(new TextWebSocketFrame(message));
                return;
            }
            ctx.channel().writeAndFlush(message);
            GlobalUserUtil.sendTextMessage(userId, receiverId, message, ctx.channel());
            return;
        }
        // 判断是否为二进制消息
        if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame img = (BinaryWebSocketFrame) frame;
            ByteBuf byteBuf = img.content();
            try (FileOutputStream outputStream = new FileOutputStream("D:\\a.jpg")) {
                byteBuf.readBytes(outputStream, byteBuf.capacity());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            byteBuf.clear();
        }

    }


    private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req) {
        // 如果HTTP解码失败，返回HTTP异常
        if (!req.decoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, (FullHttpRequest) req, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        // 获取url后置参数
        HttpMethod method = req.method();
        String uri = req.uri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        Map<String, List<String>> parameters = queryStringDecoder.parameters();
        if (parameters.containsKey("id")) {
            System.out.println("请求id:" + parameters.get("id"));
        }
        if (method == HttpMethod.GET && "/webssss".equals(uri)) {
            // ....处理
            ctx.attr(AttributeKey.valueOf("type")).set("andriod");
        } else if (method == HttpMethod.GET && "/websocket".equals(uri)) {
            // ...处理
            ctx.attr(AttributeKey.valueOf("type")).set("live");
        }
        // 构造握手响应返回，本机测试
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://" + req.headers().get("Host") + "/" + "websocket" + "", null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }

        //进行连接
        handshaker.handshake(ctx.channel(), (FullHttpRequest) req);
        System.out.println(ctx.attr(AttributeKey.valueOf("type")).get());
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx,
                                         FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(),
                    CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpHeaderUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * exception 异常 Caught 抓住 抓住异常，当发生异常的时候，可以做一些相应的处理，比如打印日志、关闭链接
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    /*@Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        clients.remove(ctx.channel());
        clients.writeAndFlush(new TextWebSocketFrame("==用户" + ctx.channel().id() + "退出" + "===当前用户数为" + clients.size()));
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {

        clients.remove(ctx.channel());
        clients.writeAndFlush(new TextWebSocketFrame("==用户" + ctx.channel().id() + "退出" + "===当前用户数为" + clients.size()));
    }*/
}
