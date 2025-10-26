package com.shms.deployrabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


//@Component
//public class DynamicQueueUtil {
//
//    @Autowired
//    private RabbitAdmin rabbitAdmin;
//
//    @Autowired
//    private FanoutExchange statusFanoutExchange;
//
//    // 创建用户状态队列（上线/下线通知）
//    public void createUserStatusQueue(String userId) {
//        String queueName = "queue_user_status_" + userId;
//        Queue statusQueue = new Queue(queueName, true, false, false);
//        rabbitAdmin.declareQueue(statusQueue);
//
//        Binding binding = BindingBuilder.bind(statusQueue).to(statusFanoutExchange);
//        rabbitAdmin.declareBinding(binding);
//
//        System.out.println("已为用户【" + userId + "】创建状态队列：" + queueName);
//    }
//
//    // 删除用户状态队列
//    public void deleteUserStatusQueue(String userId) {
//        String queueName = "queue_user_status_" + userId;
//        rabbitAdmin.deleteQueue(queueName);
//        System.out.println("已删除状态队列：" + queueName);
//    }
//}



@Component
public class DynamicQueueUtil {
    @Autowired
    private RabbitAdmin rabbitAdmin; // 核心工具类：用于动态操作MQ队列、绑定

    @Autowired
    private TopicExchange chatTopicExchange; // 注入之前声明的Topic交换机
    @Autowired
    private FanoutExchange chatFanoutExchange;
    @Autowired
    private FanoutExchange statusFanoutExchange;


    // 1. 动态为用户创建 队列 并绑定路由键（用户登录时调用）
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
        //绑定群发队列
        String queueName2 = "queue_chat_all_" + userId;
        Queue userQueue2 = new Queue(queueName2, true, false, false);
        rabbitAdmin.declareQueue(userQueue2);
        Binding userBinding2 = BindingBuilder.bind(userQueue2)
                .to(chatFanoutExchange);
        rabbitAdmin.declareBinding(userBinding2);
        //绑定用户状态队列
        String queueName3 = "queue_user_status_" + userId;
        Queue userQueue3 = new Queue(queueName3, true, false, false);
        rabbitAdmin.declareQueue(userQueue3);
        Binding userBinding3 = BindingBuilder.bind(userQueue3)
                .to(statusFanoutExchange);
        rabbitAdmin.declareBinding(userBinding3);
    }



    // 2. （可选）动态删除队列（用户注销/退出群时调用，避免资源浪费）
    public void deleteQueue(String userId) {
        String queueName1 = "queue_user_status_" + userId;
        rabbitAdmin.deleteQueue(queueName1);
        System.out.println("已删除队列：" + queueName1);
    }
}

//私信队列
//队列名：queue_chat_user_{userId}
//绑定路由键：chat.user.{userId}
//群发队列（可选，使用 Fanout）
//每个用户一个队列，例如：queue_chat_all_{userId}
//绑定到 Fanout 交换机或 Topic 的 chat.all 路由键
//用户状态队列（可选）
//每个用户独立队列，例如：queue_user_status_{userId}
//绑定到 user.status 路由键