package com.shms.deployrabbitmq.config;


import com.shms.deployrabbitmq.Service.DispatcherConsumerService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

//动态队列监听器
@Slf4j
@Configuration
public class DynamicRabbitListenerConfig {

    private final ConnectionFactory connectionFactory;
    private final DispatcherConsumerService dispatcherConsumerService;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Value("${chat.mq.queue-count:10}") // 可配置队列数量
    private int queueCount;

    public DynamicRabbitListenerConfig(ConnectionFactory connectionFactory,
                                       DispatcherConsumerService dispatcherConsumerService) {
        this.connectionFactory = connectionFactory;
        this.dispatcherConsumerService = dispatcherConsumerService;
    }

    //binding
    @PostConstruct
    public void initQueues() {
        TopicExchange exchange = new TopicExchange(RabbitMQConfig.EXCHANGE_NAME);
        rabbitAdmin.declareExchange(exchange);

        for (int i = 0; i < queueCount; i++) {
            String queueName = RabbitMQConfig.QUEUE_PREFIX + i;
            Queue queue = new Queue(queueName, true);
            rabbitAdmin.declareQueue(queue);

            // 绑定到交换机，routingKey 与发送时一致
            Binding binding = BindingBuilder.bind(queue)
                    .to(exchange)
                    .with("chat.user." + i);
            rabbitAdmin.declareBinding(binding);

            log.info(" 已声明并绑定私聊队列: {} -> routingKey: chat.user.{}", queueName, i);
            // 广播队列绑定
            Queue broadcastQueue = new Queue(RabbitMQConfig.QUEUE_BROADCAST, true);
            rabbitAdmin.declareQueue(broadcastQueue);
            Binding broadcastBinding = BindingBuilder.bind(broadcastQueue)
                    .to(exchange)
                    .with(RabbitMQConfig.ROUTING_KEY_BROADCAST);
            rabbitAdmin.declareBinding(broadcastBinding);
        }
    }

    //多队列 多消费者消费
    @PostConstruct
    public void registerDynamicListeners() {
        // 注册私聊队列
        IntStream.range(0, queueCount).forEach(i -> registerQueue(RabbitMQConfig.QUEUE_PREFIX + i));

        // 注册广播队列
        registerQueue(RabbitMQConfig.QUEUE_BROADCAST);
    }

    private void registerQueue(String queueName) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        //多队列 被dispatcherConsumerService 多线程消费
        container.setMessageListener((Message message) -> {
            try {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                dispatcherConsumerService.processMessage(body); // 异步处理
            } catch (Exception e) {
                log.error("动态队列 {} 消费异常", queueName, e);
            }
        });
        container.start();
        log.info("已注册动态队列监听器: {}", queueName);
    }
}