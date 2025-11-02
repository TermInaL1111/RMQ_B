package com.shms.deployrabbitmq.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shms.deployrabbitmq.Controller.ChatWebSocketHandler;
import com.shms.deployrabbitmq.Enity.MessageEntity;
import com.shms.deployrabbitmq.Repository.MessageRepository;
import com.shms.deployrabbitmq.config.RabbitMQConfig;
import com.shms.deployrabbitmq.pojo.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DispatcherService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatWebSocketHandler webSocketHandler;
    private final MessageRepository messageRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${chat.mq.queue-count:10}")
    private int queueCount;

    public DispatcherService(ChatWebSocketHandler webSocketHandler,
                             MessageRepository messageRepository,
                             RabbitTemplate rabbitTemplate) {
        this.webSocketHandler = webSocketHandler;
        this.messageRepository = messageRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /** 将 ChatMessage 放入 MQ */
//    public void sendMessageToMQ(ChatMessage msg) {
//        try {
//            //String.hashCode() 在 Java 中会对任意字符串生成一个 int 值。
//            //把不同的用户均匀映射到固定数量的 MQ 队列池里，避免单个队列压力过大。
//            int index = Math.abs(msg.getReceiver().hashCode()) % queueCount;
//            String routingKey = String.valueOf(index);
//            String json = objectMapper.writeValueAsString(msg);
//            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, json);
//            log.info("📤 消息放入 MQ [{}]: {}", routingKey, json);
//        } catch (Exception e) {
//            log.error("发送到 MQ 失败", e);
//        }
//    }

    public void sendMessageToMQ(ChatMessage msg) {
        try {
            String routingKey;
            if ("status".equals(msg.getType()) || "all".equals(msg.getReceiver())) {
                // 广播或状态消息
                routingKey = RabbitMQConfig.ROUTING_KEY_BROADCAST;
            } else {
                // 私聊消息
                routingKey = RabbitMQConfig.ROUTING_KEY_USER + msg.getReceiver();
            }

            String json = objectMapper.writeValueAsString(msg);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, json);
            log.info("📤 消息发到 MQ [{}]: {}", routingKey, json);
        } catch (Exception e) {
            log.error("发送到 MQ 失败", e);
        }
    }

    /** MQ 动态监听回调处理 */
    public void processMessage(String message) {
        try {
            ChatMessage msg = objectMapper.readValue(message, ChatMessage.class);
            log.info("📥 动态监听收到 MQ 消息: {}", msg);
            boolean online = webSocketHandler.isOnline(msg.getReceiver());
            if (online) {
                webSocketHandler.pushToUser(msg.getReceiver(), msg);
                saveMessage(msg, MessageEntity.Status.DELIVERED);
            } else {
                saveMessage(msg, MessageEntity.Status.SENT);
                log.info("💾 [{}] 离线，消息入库", msg.getReceiver());
            }
        } catch (Exception e) {
            log.error("MQ 消息处理异常", e);
        }
    }

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
