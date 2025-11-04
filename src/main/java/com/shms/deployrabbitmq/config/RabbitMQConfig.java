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
     *  chat.user.{Id}   —— 用户私聊
     *  chat.broadcast.*     —— 系统广播
     */
    public static final String ROUTING_KEY_USER = "chat.user.";
    public static final String ROUTING_KEY_BROADCAST = "chat.broadcast.all";
    //广播队列名
    public static final String QUEUE_BROADCAST = "chat.queue.broadcast";
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

