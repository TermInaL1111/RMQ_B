package com.shms.deployrabbitmq.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shms.deployrabbitmq.Controller.ChatWebSocketHandler;
import com.shms.deployrabbitmq.Enity.MessageEntity;
import com.shms.deployrabbitmq.Repository.MessageRepository;
import com.shms.deployrabbitmq.pojo.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


//消费者
@Service
@Slf4j
public class DispatcherConsumerService {

    @Value("${thread.maxnum}")
    private Integer maxthread;
    private final ExecutorService executor = Executors.newFixedThreadPool(maxthread); // 线程池
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatWebSocketHandler webSocketHandler;
    private final MessageRepository messageRepository;

    public DispatcherConsumerService(ChatWebSocketHandler webSocketHandler,
                                     MessageRepository messageRepository) {
        this.webSocketHandler = webSocketHandler;
        this.messageRepository = messageRepository;
    }

    public void processMessage(String message) {
        executor.submit(() -> {
            try {
                ChatMessage msg = objectMapper.readValue(message, ChatMessage.class);
                log.info("📥 消费 MQ 消息: {}", msg);

                boolean online = webSocketHandler.isOnline(msg.getReceiver());
                if (online) {
                    webSocketHandler.pushToUser(msg.getReceiver(), msg);
                    saveMessage(msg, MessageEntity.Status.DELIVERED);
                } else {
                    saveMessage(msg, MessageEntity.Status.SENT);
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
