package com.shms.deployrabbitmq.config;


import com.shms.deployrabbitmq.Service.DispatcherConsumerService;
import com.shms.deployrabbitmq.Service.DispatcherService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.context.ApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

//动态队列监听器
@Slf4j
@Configuration
public class DynamicRabbitListenerConfig {

    private final ConnectionFactory connectionFactory;
    private final DispatcherConsumerService dispatcherConsumerService;

    @Value("${chat.mq.queue-count}") // 可配置队列数量
    private int queueCount;

    public DynamicRabbitListenerConfig(ConnectionFactory connectionFactory,
                                       DispatcherConsumerService dispatcherConsumerService) {
        this.connectionFactory = connectionFactory;
        this.dispatcherConsumerService = dispatcherConsumerService;
    }

    //多队列 多消费者消费
    @PostConstruct
    public void registerDynamicListeners() {
        IntStream.range(0, queueCount).forEach(i -> {
            String queueName = "chat.queue." + i;

            // 创建监听容器
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            container.setQueueNames(queueName);

            // 消息监听器
            container.setMessageListener((Message message) -> {
                try {
                    String body = new String(message.getBody(), StandardCharsets.UTF_8);
                    dispatcherConsumerService.processMessage(body); // 调用 DispatcherConsumerService 消费
                } catch (Exception e) {
                    log.error("动态队列 {} 消费异常", queueName, e);
                }
            });

            container.start();
            log.info("✅ 已注册动态队列监听器: {}", queueName);
        });
    }
}
