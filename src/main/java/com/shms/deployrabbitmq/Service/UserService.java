package com.shms.deployrabbitmq.Service;

import com.shms.deployrabbitmq.RmqBApplication;
import com.shms.deployrabbitmq.pojo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;



@Service
@RabbitListener(queues={"cug"})
public class UserService {




//
//    private  static Map<String, User> userGradeMap = new HashMap<>();
//
//    private static Logger log = LoggerFactory.getLogger(RmqBApplication.class);
//
//    static {
//        userGradeMap.put("张三",new User("张三","100"));
//        userGradeMap.put("李四",new User("李四","90"));
//        userGradeMap.put("王五",new User("王五","80"));
//    }
//
//    @Autowired
//    RabbitTemplate rabbitTemplate;
//    // 监听处理消息的方法
//    @RabbitHandler
//    public User receiveMessage(@Payload User user) {
//        System.out.println("接收到消息 user: " + user);
//        log.info("user: {}", user);
//
//        // 根据姓名查成绩
//        User user1 = userGradeMap.get(user.getUserName());
//        return user1;
//    }



}
