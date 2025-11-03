package com.shms.deployrabbitmq.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shms.deployrabbitmq.Controller.ChatWebSocketHandler;
import com.shms.deployrabbitmq.Enity.MessageEntity;
import com.shms.deployrabbitmq.Repository.MessageRepository;
import com.shms.deployrabbitmq.pojo.ChatMessage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


//消费者
@Service
@Slf4j
public class DispatcherConsumerService {

    @Value("${thread.maxnum:2}")
    private Integer maxthread;
    private  ExecutorService executor ; // 线程池
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatWebSocketHandler webSocketHandler;
    private final MessageRepository messageRepository;
    @PostConstruct
    public void init() {
        int threads = maxthread != null ? maxthread : 2; // 给默认值
        System.out.println("maxthread = " + threads);
        executor = Executors.newFixedThreadPool(threads); // ✅ 在这里初始化线程池
        // 初始化逻辑放这里
    }
    public DispatcherConsumerService(ChatWebSocketHandler webSocketHandler,
                                     MessageRepository messageRepository) {
        this.webSocketHandler = webSocketHandler;
        this.messageRepository = messageRepository;
    }

    public void processMessage(String message) {
        executor.submit(() -> {
            try {
                log.info("Received message: {}", message);
                // 去掉外层双引号并处理转义
                String raw = message;
                if (raw.startsWith("\"") && raw.endsWith("\"")) {
                    raw = raw.substring(1, raw.length() - 1).replace("\\\"", "\"");
                }
                log.info("Received message: {}", raw);
                ChatMessage msg = objectMapper.readValue(raw, ChatMessage.class);
                if ("status".equals(msg.getType()) || "all".equals(msg.getReceiver())) {
                    log.info("111");
                    // 广播上线/下线状态 or 群发消息
                   // boolean online = webSocketHandler.isOnline(msg.getReceiver());
                 //   if (online) {
                        webSocketHandler.pushToAll(msg);
                  //      saveMessage(msg, MessageEntity.Status.DELIVERED);
//                    }
//                    else {
//                    saveMessage(msg, MessageEntity.Status.SENT);
//                        }
                } else {
                    // 私聊消息
                    boolean online = webSocketHandler.isOnline(msg.getReceiver());
                    if (online) {
                        log.info("发消息?");
                        webSocketHandler.pushToUser(msg.getReceiver(), msg);
                        saveMessage(msg, MessageEntity.Status.DELIVERED);
                    } else {
                        saveMessage(msg, MessageEntity.Status.SENT);
                    }
                }
            } catch (Exception e) {
                log.error("处理消息异常", e);
            }
        });
    }


    //保持消息到数据库
    private void saveMessage(ChatMessage msg, MessageEntity.Status status) {
        MessageEntity entity = new MessageEntity();
        entity.setMessageId(msg.getMessageId());
        entity.setSender(msg.getSender());
        entity.setReceiver(msg.getReceiver());
        entity.setType(MessageEntity.MessageType.valueOf(msg.getType()));
        entity.setContent(msg.getContent());
        entity.setFileUrl(msg.getFileUrl());
        entity.setTimestamp(msg.getTimestamp());
        entity.setStatus(status);
        messageRepository.save(entity);
    }
}
