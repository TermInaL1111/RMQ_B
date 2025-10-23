package com.shms.deployrabbitmq.Controller;

import com.shms.deployrabbitmq.DynamicQueueUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private DynamicQueueUtil dynamicQueueUtil;

    // 用户登录接口
    @PostMapping("/login")
    public String login(@RequestParam String userId, @RequestParam String password) {
        // 1. 此处省略：密码验证、生成登录态等逻辑
        // 2. 登录成功后，为用户动态创建私信队列
        dynamicQueueUtil.createUserQueue(userId);
        return "用户【" + userId + "】登录成功，已创建私信队列";
    }

    // （可选）用户注销时删除队列
    @PostMapping("/logout")
    public String logout(@RequestParam String userId) {
        String queueName = "queue_chat_user_" + userId;
        dynamicQueueUtil.deleteQueue(queueName);
        return "用户【" + userId + "】注销成功，已删除私信队列";
    }
}