package com.shms.deployrabbitmq;

import com.shms.deployrabbitmq.Controller.UserController;
import com.shms.deployrabbitmq.Service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RmqBApplicationTests {
    @Autowired
    UserController controller ;
//    @Autowired
//    ChatService service ;
//    @Autowired
//    DynamicQueueUtil dynamicQueueUtil ;
    @Test
    void contextLoads() {
    }

    @Test
    public void test() {
        // 1. 初始化 Controller


        // 模拟 @Value 注入
        controller.userDbPath = "./data/users_test.xml";

        // 模拟 @PostConstruct
        controller.initDb();

        // 2. 注册用户
        System.out.println(controller.register("alice", "123456"));

        // 3. 登录
        System.out.println(controller.login("alice", "123456"));

        // 4. 群发消息
        System.out.println(controller.sendAll("alice", "Hello everyone"));

        // 5. 私发消息
        System.out.println(controller.sendTo("alice", "bob", "Hi Bob"));

        // 6. 注销
        System.out.println(controller.logout("alice"));
    }


}
