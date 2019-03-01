package com.vichain.websocket.server;

import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Mr.Joe
 * @ServerEndPoint 注解是一个类层次的注解，它的功能主要是将目前的类定义成一个websocket服务器端，
 * 注解的值将被用于监听用户连接的终端访问URL地址，客户端可以通过这个URL连接到websocket服务器端
 * @since 2019-03-01
 */
@ServerEndpoint("/websocket")
@Component
public class WebSocketServer {

    // 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;
    // concurrent包的线程安全Set，用来存放每个客户端对应的WebSocketServer对象。
    // private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<>();
    // 定义一个
    private static ConcurrentHashMap userMap = new ConcurrentHashMap();
    // 与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("id") int userId) {
        this.session = session;
        String cid = session.getId();
        System.out.println("客户端" + cid + "进入房间");
        //加入set中
        if (!userMap.containsKey(userId)) {
            //塞入map中
            userMap.put(userId, session);
        }
        //在线数加1
        addOnlineCount();
        System.out.println("客户端:" + cid + "进入房间,当前在线人数为" + getOnlineCount());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) {
    }

    /**
     * 收到客户端消息后调用的方法
     * @param message 客户端发送过来的消息 ,消息格式为发件人id,收件人id,消息
     *                例如: 1,[1.2.3],abcdefg
     */
    @OnMessage
    public void onMessage(String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        String[] list = message.split(",");
        if (list.length <= 3) {
            return;
        }

        // 发消息
        Session sender = (Session) userMap.get(list[0]);
        Session receiver = (Session) userMap.get(list[1]);
        String msg = list[2];
        if (sender != null && receiver != null) {
            //发送文字
            sender.getAsyncRemote().sendText(msg);
            receiver.getAsyncRemote().sendText(msg);
        } else {
            sender.getAsyncRemote().sendText("对方已离线");
            // 持久化数据,等待上线推送

        }

    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    /**
     * 群发自定义消息
     */
    /*public static void sendInfo(String message) {
        for (WebSocketServer item : webSocketSet) {
            item.sendMessage(message);
        }
    }*/

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) {
        this.session.getAsyncRemote().sendText(message);
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }

}
