package com.shms.deployrabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.ArrayList;
import java.util.List;

//Spring Boot 启动
//扫描所有带 @Configuration 的配置类（比如你的 MyRabbitConfig）。
//执行这些类里所有的 @Bean 方法。
//把每个返回的对象（Queue、Exchange、Binding、RabbitAdmin 等）注册到 Spring 容器中。


@Configuration
public class RabbitMQConfig  {


    public static final String EXCHANGE_NAME = "chat.topic";
    public static final String QUEUE_PREFIX = "chat.queue.";

    /** Routingkey
     *  chat.user.{userId}   —— 单用户私聊
     *  chat.broadcast.*     —— 系统广播
     */
    public static final String ROUTING_KEY_USER = "chat.user.";
    public static final String ROUTING_KEY_BROADCAST = "chat.broadcast.all";

    //广播队列名
    public static final String QUEUE_BROADCAST = "chat.queue.broadcast";

//    //广播的单独队列
//    @Bean
//    public Queue broadcastQueue() {
//        return new Queue(QUEUE_BROADCAST, true);
//    }
//
//    @Bean
//    public Binding broadcastBinding(TopicExchange exchange, Queue broadcastQueue) {
//        return BindingBuilder.bind(broadcastQueue)
//                .to(exchange)
//                .with(RabbitMQConfig.ROUTING_KEY_BROADCAST);
//    }

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }
    //转为json
        @Bean
        public Jackson2JsonMessageConverter messageConverter() {
            return new Jackson2JsonMessageConverter();
        }
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("消息发送失败: " + cause);
            }
        });
        return template;
    }
}


//    //共享私信队列  receiver 或 routingKey 过滤自己的消息
//    @Bean
//    public Queue privateQueue() {
//        return new Queue("queue_chat_private", true);
//    }
//
//    @Bean
//    public Queue ackQueue() {
//        return new Queue("queue_chat_ack", true);
//    }
//
//    @Bean
//    public Binding privateBinding(Queue privateQueue, TopicExchange chatTopicExchange) {
//        return BindingBuilder.bind(privateQueue).to(chatTopicExchange).with("chat.user.*");
//    }



//@Configuration
//public class MyRabbitConfig {
//    //转为json
//    @Bean
//    public MessageConverter messageConverter() {
//        return new Jackson2JsonMessageConverter();
//    }
//
//    // 1. 声明 Topic 交换机
//    @Bean
//    public TopicExchange chatTopicExchange() {
//        return new TopicExchange("chat_topic_exchange", true, false);
//    }
//    //2 声明fanout 交换机
//    @Bean
//    public FanoutExchange chatFanoutExchange(){
//        return new FanoutExchange("chat_fanout_exchange",true,false);
//    }
//    @Bean
//    public FanoutExchange statusFanoutExchange(){
//        return new FanoutExchange("status_fanout_exchange",true,false);
//    }
//
//    // 4. 显式配置 RabbitAdmin（关键：解决自动装配失败问题）
//    @Bean
//    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
//        // 传入 ConnectionFactory 实例，创建 RabbitAdmin
//        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
//        // 必须设置为 true，否则 RabbitAdmin 不会自动执行声明操作 @Bean 方法
//        rabbitAdmin.setAutoStartup(true);
//        return rabbitAdmin;
//    }
//}
//RabbitAdmin 的创建依赖：RabbitAdmin 需要基于 ConnectionFactory（RabbitMQ 连接工厂）创建，
//而 Spring Boot 虽然会自动配置 ConnectionFactory（只要配置了 spring.rabbitmq 相关参数），
//但 RabbitAdmin 并非默认一定会被自动注册为 Bean。

//一个 chat_topic_exchange，通过 routing key 来区分：
//功能	Routing Key
//群聊消息	chat.all
//私聊消息	chat.user.<username>
//用户状态广播	user.status