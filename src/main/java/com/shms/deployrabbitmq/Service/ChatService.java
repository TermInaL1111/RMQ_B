package com.shms.deployrabbitmq.Service;

import com.shms.deployrabbitmq.pojo.ChatMessage;
import com.shms.deployrabbitmq.pojo.UserStatusMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ChatService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 群发消息
    //md 这里参数搞错了  想当然没有 routingkey  结果把 交换机当成 key了
    public void sendToAll(ChatMessage msg) {
        rabbitTemplate.convertAndSend("chat_fanout_exchange","", msg);
    }

//    // 私发消息
//    public void sendToUser(ChatMessage msg) {
//        // 路由键规则：chat.user.用户ID
//        String routeKey = "chat.user." + msg.getReceiver();
//        rabbitTemplate.convertAndSend("chat_topic_exchange", routeKey, msg);
//    }

    // 群发 上线 / 下线   目前
    public void sendUserStatus(ChatMessage msg) {

        rabbitTemplate.convertAndSend("status_fanout_exchange","", msg);
    }


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
}