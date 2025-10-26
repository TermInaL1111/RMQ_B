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

    // 发送私信（共享队列 + routingKey）
    public void sendPrivateMessage(ChatMessage msg) {
        msg.setTimestamp(System.currentTimeMillis());
        msg.setMessageId(UUID.randomUUID().toString()); // 唯一消息ID

        rabbitTemplate.convertAndSend(
                "chat_topic_exchange",
                "chat.user." + msg.getReceiver(),
                msg,
                new CorrelationData(msg.getMessageId())
        );

        // 记录消息状态（未读）
        MessageEntity entity = new MessageEntity();
        entity.setMessageId(msg.getMessageId());
        entity.setSender(msg.getSender());
        entity.setReceiver(msg.getReceiver());
        entity.setContent(msg.getContent());
        entity.setType(MessageEntity.MessageType.valueOf(msg.getType()));
        entity.setStatus(MessageEntity.Status.valueOf("UNREAD"));
        entity.setTimestamp(msg.getTimestamp());
        messageRepository.save(entity);

        log.info("发送私信: {}", msg);
    }

    // 发送群发消息（状态 / 广播）
    public void sendUserStatus(ChatMessage msg) {
        msg.setTimestamp(System.currentTimeMillis());
        rabbitTemplate.convertAndSend("status_fanout_exchange", "", msg);
        log.info("发送用户状态: {}", msg);
    }

    // 消息回执处理
    public void ackMessage(String messageId) {
        messageRepository.findByMessageId(messageId).ifPresent(msg -> {
            msg.setStatus(MessageEntity.Status.valueOf("READ"));
            messageRepository.save(msg);
            log.info("消息已确认: {}", messageId);
        });
    }
}








//@Service
//public class ChatService {
//
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//
//    // 群发消息
//    //md 这里参数搞错了  想当然没有 routingkey  结果把 交换机当成 key了
//    public void sendToAll(ChatMessage msg) {
//        rabbitTemplate.convertAndSend("chat_fanout_exchange", "", msg);
//    }
//
////    // 私发消息
////    public void sendToUser(ChatMessage msg) {
////        // 路由键规则：chat.user.用户ID
////        String routeKey = "chat.user." + msg.getReceiver();
////        rabbitTemplate.convertAndSend("chat_topic_exchange", routeKey, msg);
////    }
//
//    // 群发 上线 / 下线   目前
//    public void sendUserStatus(ChatMessage msg) {
//
//        rabbitTemplate.convertAndSend("status_fanout_exchange", "", msg);
//    }
//}

//    // 发送文件（文件转为字节数组）
//    public void sendFile(String sender, String receiver, MultipartFile file) throws IOException {
//        ChatMessage msg = new ChatMessage();
//        msg.setType("file");
//        msg.setSender(sender);
//        msg.setReceiver(receiver);
//        msg.setContent(file.getOriginalFilename());
//        msg.setFileData(file.getBytes());
//        msg.setTimestamp(System.currentTimeMillis());
//        sendToUser(msg);
//    }
