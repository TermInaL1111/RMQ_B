package com.shms.deployrabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DynamicQueueUtil {
    @Autowired
    private RabbitAdmin rabbitAdmin; // 核心工具类：用于动态操作MQ队列、绑定

    @Autowired
    private TopicExchange chatTopicExchange; // 注入之前声明的Topic交换机

    // 1. 动态为用户创建“私信队列”并绑定路由键（用户登录时调用）
    public void createUserQueue(String userId) {
        // 队列名规则：queue_chat_user_用户ID（确保唯一）
        String queueName = "queue_chat_user_" + userId;
        // 路由键规则：chat.user.用户ID（与发送逻辑对应）
        String routingKey = "chat.user." + userId;

        // 1.1 创建队列（参数：队列名、持久化、排他、自动删除）
        Queue userQueue = new Queue(queueName, true, false, false);
        rabbitAdmin.declareQueue(userQueue); // 执行创建队列

        // 1.2 绑定队列到交换机
        Binding userBinding = BindingBuilder.bind(userQueue)
                .to(chatTopicExchange)
                .with(routingKey);
        rabbitAdmin.declareBinding(userBinding); // 执行绑定
        System.out.println("已为用户【" + userId + "】创建私信队列：" + queueName);
    }



    // 3. （可选）动态删除队列（用户注销/退出群时调用，避免资源浪费）
    public void deleteQueue(String queueName) {
        rabbitAdmin.deleteQueue(queueName);
        System.out.println("已删除队列：" + queueName);
    }
}