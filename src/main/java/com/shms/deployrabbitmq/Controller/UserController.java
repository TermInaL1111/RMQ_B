package com.shms.deployrabbitmq.Controller;

import com.shms.deployrabbitmq.Database;
import com.shms.deployrabbitmq.DynamicQueueUtil;
import com.shms.deployrabbitmq.Service.ChatService;
import com.shms.deployrabbitmq.pojo.ChatMessage;
import com.shms.deployrabbitmq.pojo.Result;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/user")
public class UserController {

    @Value("${chat.user-db:./data/users.xml}")
    public String userDbPath;

    private Database db;

    @PostConstruct
    public void initDb() {
        this.db = new Database(userDbPath);
    }

    @Autowired
    public ChatService chatService;
    @Autowired
    public DynamicQueueUtil dynamicQueueUtil;

    //注册接口
    @PostMapping("/register")
    public Result register(@RequestParam String username, @RequestParam String password) {
        if (db.userExists(username)) return Result.error("用户已存在");
        db.addUser(username, password);
        return Result.success("注册成功");
    }
    // 用户登录接口
    @PostMapping("/login")
    public Result login(@RequestParam String username, @RequestParam String password) {
        // 1. 此处省略：密码验证、生成登录态等逻辑
        // 2. 登录成功后，为用户动态创建私信队列
        if (!db.checkLogin(username, password))
            return Result.error("账号或密码错误");
        dynamicQueueUtil.createUserQueue(username);
        return Result.success("登录成功");
    }

    // （可选）用户注销时删除队列
    @PostMapping("/logout")
    public Result logout(@RequestParam String username) {
        dynamicQueueUtil.deleteQueue("queue_chat_user_" + username);
        return Result.success("登出成功");
    }

    @PostMapping("/sendAll")
    public Result sendAll(@RequestParam String sender, @RequestParam String content) {
        ChatMessage msg = new ChatMessage();
        msg.setType("message");
        msg.setSender(sender);
        msg.setReceiver("all");
        msg.setContent(content);
        msg.setTimestamp(System.currentTimeMillis());
        chatService.sendToAll(msg);
        return Result.success("群发成功");
    }

    @PostMapping("/sendTo")
    public Result sendTo(@RequestParam String sender, @RequestParam String receiver, @RequestParam String content) {
        ChatMessage msg = new ChatMessage();
        msg.setType("message");
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content);
        msg.setTimestamp(System.currentTimeMillis());
        chatService.sendToUser(msg);
        return Result.success("私发成功");
    }

    @PostMapping("/sendFile")
    public Result sendFile(@RequestParam String sender,
                           @RequestParam String receiver,
                           @RequestPart MultipartFile file) throws IOException {
        chatService.sendFile(sender, receiver, file);
        return Result.success("文件发送成功");
    }
}