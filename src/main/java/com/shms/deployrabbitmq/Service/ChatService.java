package com.shms.deployrabbitmq.Service;

import com.shms.deployrabbitmq.Enity.MessageEntity;
import com.shms.deployrabbitmq.Repository.MessageRepository;
import com.shms.deployrabbitmq.Repository.UserRepository;
import com.shms.deployrabbitmq.pojo.ChatMessage;

import com.shms.deployrabbitmq.pojo.MessageAck;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;


@Service
@Slf4j
public class ChatService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MessageRepository messageRepository;

    public void sendMessageToUser(String username, MessageEntity msg) {
        String routingKey = "chat.user." + username; // 用户专属路由键
        rabbitTemplate.convertAndSend(
                "chat_topic_exchange", // topic 交换机
                routingKey,
                msg,
                new CorrelationData(msg.getMessageId()) // 确认消息
        );
        log.info("发送未读消息到 {} 路由: {}", username, msg.getMessageId());
    }

    // 发送群发消息（状态 / 广播）
    public void sendUserStatus(ChatMessage msg) {
        msg.setTimestamp(System.currentTimeMillis());
        rabbitTemplate.convertAndSend("status_fanout_exchange", "", msg);
        log.info("发送用户状态: {}", msg);
    }

//    // 消息回执处理
//    public void ackMessage(String messageId) {
//        messageRepository.findByMessageId(messageId).ifPresent(msg -> {
//            msg.setStatus(MessageEntity.Status.valueOf("READ"));
//            messageRepository.save(msg);
//            log.info("消息已确认: {}", messageId);
//        });
//    }

}