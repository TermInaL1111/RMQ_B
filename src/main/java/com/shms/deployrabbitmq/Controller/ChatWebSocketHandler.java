package com.shms.deployrabbitmq.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shms.deployrabbitmq.Enity.UserEntity;
import com.shms.deployrabbitmq.Repository.UserRepository;
import com.shms.deployrabbitmq.Service.DispatcherProducerService;
import com.shms.deployrabbitmq.Service.DispatcherService;
import com.shms.deployrabbitmq.pojo.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final DispatcherProducerService dispatcherProducerService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository userRepository;

    public ChatWebSocketHandler(DispatcherProducerService dispatcherProducerService) {
        this.dispatcherProducerService = dispatcherProducerService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserId(session);
        sessions.put(userId, session);
        log.info("✅ {} 已连接", userId);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatMessage msg = new ObjectMapper().readValue(message.getPayload(), ChatMessage.class);
        dispatcherProducerService.sendMessageToMQ(msg);
    }
    public boolean isOnline(String userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = getUserId(session);
        sessions.remove(userId);
        log.info("🔴 {} 断开连接", userId);

        // 更新数据库状态为离线
        try {
            Optional<UserEntity> opt = userRepository.findByUsername(userId);
            if (opt.isPresent()) {
                UserEntity user = opt.get();
                user.setStatus(UserEntity.Status.offline);
                userRepository.save(user);
                log.info("用户 [{}] 断开连接，数据库状态更新为 offline", userId);
            }
        } catch (Exception e) {
            log.error("更新用户离线状态失败: {}", userId, e);
        }

        // 广播下线状态
        ChatMessage statusMsg = new ChatMessage();
        statusMsg.setType("status");
        statusMsg.setSender(userId);
        statusMsg.setReceiver("all");
        statusMsg.setContent("offline");
        pushStatusToAll(statusMsg);
    }

    // MQ 消费者会调用这个方法推送消息
    public void pushToUser(String userId, ChatMessage msg) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(msg)));
            } catch (Exception e) {
                log.error("发送消息失败给 {}", userId, e);
            }
        } else {
            log.warn("用户 {} 不在线", userId);
        }
    }
    private void pushStatusToAll(ChatMessage msg) {
        sessions.keySet().forEach(userId -> {
            pushToUser(userId, msg);
        });
    }

    private String getUserId(WebSocketSession session) {
        return session.getUri().getQuery().split("=")[1];
    }
}
