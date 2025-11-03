package com.shms.deployrabbitmq.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shms.deployrabbitmq.config.RabbitMQConfig;
import com.shms.deployrabbitmq.pojo.ChatMessage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//生产者
@Service
@Slf4j
public class DispatcherProducerService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RabbitTemplate rabbitTemplate;
    private  ExecutorService executor;
    // 线程池
    @Value("${thread.maxnum:2}")
    private Integer maxthread;
    @PostConstruct
    public void init() {
        int threads = maxthread != null ? maxthread : 2; // 给默认值
        System.out.println("maxthread = " + threads);
        executor = Executors.newFixedThreadPool(threads);
        // 初始化逻辑放这里
    }


    @Value("${chat.mq.queue-count:10}")
    private int queueCount;

    public DispatcherProducerService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** 异步发送消息到 MQ */
    public void sendMessageToMQ(ChatMessage msg) {
        executor.submit(() -> {
            try {
                String routingKey;
                if ("status".equals(msg.getType()) || "all".equals(msg.getReceiver())) {
                    routingKey = RabbitMQConfig.ROUTING_KEY_BROADCAST;
                } else {
                    //把不同的用户均匀映射到固定数量的 MQ 队列池里，避免单个队列压力过大。
                    //int index = Math.abs(msg.getReceiver().hashCode()) % queueCount;
                    int index = Math.abs(msg.getReceiver().hashCode()) % queueCount;
                    routingKey = RabbitMQConfig.ROUTING_KEY_USER + index;
                    log.info(routingKey +"什么");
                }
                String json = objectMapper.writeValueAsString(msg);
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, json);
                log.info("📤 异步发送消息到 MQ [{}]: {}", routingKey, json);
            } catch (Exception e) {
                log.error("发送到 MQ 失败", e);
            }
        });
    }
}
