package com.shms.deployrabbitmq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;



@Configuration
public class MyRabbitConfig {
    //转为json
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 1. 声明 Topic 交换机
    @Bean
    public TopicExchange chatTopicExchange() {
        return new TopicExchange("chat_topic_exchange", true, false);
    }
    // 2. 声明“全员消息”队列 + 绑定路由键
    @Bean
    public Queue allUserQueue() {
        return new Queue("queue_chat_all", true, false, false); // 队列名：全员队列
    }
    @Bean
    public Binding allUserBinding(Queue allUserQueue, TopicExchange chatTopicExchange) {
        // 路由键：chat.all，绑定队列到交换机
        return BindingBuilder.bind(allUserQueue).to(chatTopicExchange).with("chat.all");
    }

    // 3. 显式配置 RabbitAdmin（关键：解决自动装配失败问题）
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        // 传入 ConnectionFactory 实例，创建 RabbitAdmin
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        // 必须设置为 true，否则 RabbitAdmin 不会自动执行声明操作
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }
}
//RabbitAdmin 的创建依赖：RabbitAdmin 需要基于 ConnectionFactory（RabbitMQ 连接工厂）创建，
//而 Spring Boot 虽然会自动配置 ConnectionFactory（只要配置了 spring.rabbitmq 相关参数），
//但 RabbitAdmin 并非默认一定会被自动注册为 Bean。
