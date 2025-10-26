package com.shms.deployrabbitmq.Controller;

import com.shms.deployrabbitmq.Database;
import com.shms.deployrabbitmq.DynamicQueueUtil;
import com.shms.deployrabbitmq.Enity.UserEntity;
import com.shms.deployrabbitmq.Repository.UserRepository;
import com.shms.deployrabbitmq.Service.ChatService;
import com.shms.deployrabbitmq.pojo.ChatMessage;
import com.shms.deployrabbitmq.pojo.Result;
import com.shms.deployrabbitmq.pojo.User;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatService chatService;

    @Autowired
    private DynamicQueueUtil dynamicQueueUtil;

    @PostMapping("test")
    public Result test(@RequestBody User user){
        return Result.success();
    }

    // 用户注册
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return Result.error("用户已存在");
        }
        UserEntity entity = new UserEntity();
        entity.setUsername(user.getUsername());
        entity.setPassword(user.getPassword());
        userRepository.save(entity);
        log.info("用户注册成功：{}", user.getUsername());
        return Result.success("注册成功");
    }

    // 用户登录
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        Optional<UserEntity> optional = userRepository.findByUsername(user.getUsername());
        if (optional.isEmpty() || !optional.get().getPassword().equals(user.getPassword())) {
            log.error("登录失败: {}", user.getUsername());
            return Result.error("账号或密码错误");
        }

        // 创建用户队列
        dynamicQueueUtil.createUserQueue(user.getUsername());

        // 广播上线状态
        ChatMessage statusMsg = new ChatMessage();
        statusMsg.setType("status");
        statusMsg.setSender(user.getUsername());
        statusMsg.setReceiver("all");
        statusMsg.setContent("online");
        chatService.sendUserStatus(statusMsg);

        log.info("用户登录成功: {}", user.getUsername());
        return Result.success("登录成功");
    }

    // 用户登出
    @PostMapping("/logout")
    public Result logout(@RequestParam String username) {
        // 删除状态队列
        dynamicQueueUtil.deleteQueue(username);

        // 广播下线状态
        ChatMessage statusMsg = new ChatMessage();
        statusMsg.setType("status");
        statusMsg.setSender(username);
        statusMsg.setReceiver("all");
        statusMsg.setContent("offline");
        chatService.sendUserStatus(statusMsg);

        log.info("用户登出成功: {}", username);
        return Result.success("登出成功");
    }

    // 消息回执接口
    @PostMapping("/ack")
    public Result ackMessage(@RequestParam String messageId) {
        chatService.ackMessage(messageId);
        return Result.success("消息已确认");
    }
}




//@RestController
//@Slf4j
//@RequestMapping("/user")
//public class UserController {
//
//    @Value("${chat.user-db:./data/users.xml}")
//    public String userDbPath;
//
//    private Database db;
//
//    @PostConstruct
//    public void initDb() {
//        this.db = new Database(userDbPath);
//    }
//
//    @Autowired
//    public ChatService chatService;
//    @Autowired
//    public DynamicQueueUtil dynamicQueueUtil;
//
//    //注册接口
//    @PostMapping("/register")
//    public Result register(@RequestBody User user) {
//        if (db.userExists(user.getUsername())) return Result.error("用户已存在");
//        db.addUser(user.getUsername(), user.getPassword());
//        log.info("用户注册成功：" + user.getUsername());
//        return Result.success("注册成功");
//    }
//
//    // 用户登录接口
//    @PostMapping("/login")
//    public Result login(@RequestBody User user) {
//        // 1. 此处省略：密码验证、生成登录态等逻辑
//        // 2. 登录成功后，为用户动态创建私信队列
//        if (!db.checkLogin(user.getUsername(), user.getPassword())) {
//            log.error("用户登录失败：" + user.getUsername());
//            return Result.error("账号或密码错误");
//        }
//        dynamicQueueUtil.createUserQueue(user.getUsername());
//
////        UserStatusMessage msg = new UserStatusMessage();
////        msg.setUsername(user.getUsername());
////        msg.setStatus("online");
//        ChatMessage msg = new ChatMessage();
//        msg.setType("status");
//        msg.setSender(user.getUsername());
//        msg.setContent("ONLINE");
//        msg.setReceiver("all");
//        msg.setTimestamp(System.currentTimeMillis());
//        chatService.sendUserStatus(msg);
//        log.info("用户登录成功：" + user.getUsername());
//
//        return Result.success("登录成功");
//    }
//
//    // 用户注销时删除队列
//    @PostMapping("/logout")
//    public Result logout(@RequestParam String username) {
//        dynamicQueueUtil.deleteQueue("queue_chat_user_" + username);
//
////        UserStatusMessage msg = new UserStatusMessage();
////        msg.setUsername(username);
////        msg.setStatus("offline");
//        ChatMessage msg = new ChatMessage();
//        msg.setType("status");
//        msg.setSender(username);
//        msg.setContent("OFFLINE");
//        msg.setReceiver("all");
//        msg.setTimestamp(System.currentTimeMillis());
//        chatService.sendUserStatus(msg);
//        log.info("用户注销成功：" + username);
//
//        return Result.success("登出成功");
//    }
//}
