package com.shms.deployrabbitmq.Controller;

import com.shms.deployrabbitmq.DynamicQueueUtil;
import com.shms.deployrabbitmq.Enity.MessageEntity;
import com.shms.deployrabbitmq.Enity.UserEntity;
import com.shms.deployrabbitmq.Repository.MessageRepository;
import com.shms.deployrabbitmq.Repository.UserRepository;
import com.shms.deployrabbitmq.Service.ChatService;
import com.shms.deployrabbitmq.pojo.ChatMessage;
import com.shms.deployrabbitmq.pojo.Result;
import com.shms.deployrabbitmq.pojo.User;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Value("${chat.file.upload-dir}")
    private String fileDir; // 从配置文件注入
    @Value("${chat.file.access-url}")
    private String fileAccessUrl;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MessageRepository messageRepository;

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
        UserEntity entity = optional.get();
        // 检查是否已在线
        if (entity.getStatus() == UserEntity.Status.online) {
            log.warn("用户 {} 已经在线", user.getUsername());
            return Result.error("用户已在线，请先下线再登录");
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

        optional.get().setStatus(UserEntity.Status.online);
        userRepository.save(optional.get());

        // ✅ 拉取未读消息
        List<MessageEntity> unreadMessages = messageRepository.findByReceiverAndStatus(
                user.getUsername(), MessageEntity.Status.SENT
        );

        // 将未读消息发送给客户端
        for (MessageEntity msg : unreadMessages) {
            chatService.sendMessageToUser(user.getUsername(), msg);
            msg.setStatus(MessageEntity.Status.DELIVERED);
            messageRepository.save(msg);
        }

        log.info("用户登录成功: {}, 未读消息数量: {}", user.getUsername(), unreadMessages.size());


        log.info("用户登录成功: {}", user.getUsername());
        return Result.success("登录成功");
    }

    // 用户登出
//    @PostMapping("/logout")
//    public Result logout(@RequestParam String username) {
//        // 删除状态队列
//        dynamicQueueUtil.deleteQueue(username);
//
//        // 广播下线状态
//        ChatMessage statusMsg = new ChatMessage();
//        statusMsg.setType("status");
//        statusMsg.setSender(username);
//        statusMsg.setReceiver("all");
//        statusMsg.setContent("offline");
//        chatService.sendUserStatus(statusMsg);
////登出时数据库状态更新
//        Optional<UserEntity> opt = userRepository.findByUsername(username);
//        opt.ifPresent(user -> {
//            user.setStatus(UserEntity.Status.offline);
//            userRepository.save(user);
//        });
//
//
//        log.info("用户登出成功: {}", username);
//        return Result.success("登出成功");
//    }
@Transactional
    @PostMapping("/logout")
    public Result logout(@RequestBody User user1) {
        String username = user1.getUsername();
        log.info("收到登出请求，用户名: [{}]", username); // 注意打印原始字符串（含空格等）

        // 1. 验证用户是否存在
        Optional<UserEntity> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) {
            log.error("登出失败：未查询到用户 [{}]", username);
            return Result.error("用户不存在");
        }

        // 2. 删除队列（原有逻辑）
        dynamicQueueUtil.deleteQueue(username);

        // 3. 广播下线状态（原有逻辑）
        ChatMessage statusMsg = new ChatMessage();
        statusMsg.setType("status");
        statusMsg.setSender(username);
        statusMsg.setReceiver("all");
        statusMsg.setContent("offline");
        chatService.sendUserStatus(statusMsg);

        // 4. 更新数据库状态
        UserEntity user = opt.get();
        user.setStatus(UserEntity.Status.offline);
        UserEntity saved = userRepository.save(user); // 接收保存后的返回值
        log.info("用户 [{}] 状态更新为: {}（数据库实际值）", username, saved.getStatus()); // 确认保存结果

        log.info("用户登出成功: {}", username);
        return Result.success("登出成功");
    }
    // 消息回执接口
    @PostMapping("/ack")
    public Result ackMessage(@RequestParam String messageId) {
        messageRepository.findByMessageId(messageId).ifPresent(msg -> {
            msg.setStatus(MessageEntity.Status.READ);
            messageRepository.save(msg);
        });
        chatService.ackMessage(messageId);
        return Result.success("消息已确认");
    }

    @PostMapping("/upload")
    public Result uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("文件不能为空");
        try {
            File dir = new File(fileDir);
            if (!dir.exists()) dir.mkdirs();

            // 生成文件名（确保有下划线）
            String storedFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            // 新增日志：打印生成的文件名和存储路径
            log.info("生成的存储文件名：{}", storedFileName);
            File dest = new File(dir, storedFileName);
            log.info("文件实际存储路径：{}", dest.getAbsolutePath());

            file.transferTo(dest);

           // String fileUrl = fileAccessUrl + storedFileName;
            String fileUrl = fileAccessUrl + URLEncoder.encode(storedFileName, StandardCharsets.UTF_8);

            log.info("返回给前端的fileUrl：{}", fileUrl); // 打印返回的URL
            return Result.success(fileUrl);
        } catch (Exception e) {
            log.error("上传失败", e);
            return Result.error("上传失败");
        }
    }
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path path = Paths.get(fileDir, fileName);
            System.out.println("尝试下载文件路径：" + path.toAbsolutePath());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                log.warn("文件不存在: {}", path);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            log.error("下载路径解析失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
    @GetMapping("/online")
    public Result getonline(){
        List<UserEntity>list =  userRepository.findByStatus(UserEntity.Status.online);
        // 2. 提取用户名，转换为字符串列表
        List<String> usernameList = list.stream()
                .map(UserEntity::getUsername) // 提取username字段
                .collect(Collectors.toList());

        log.info("获取在线列表：{}", usernameList);
        return Result.success(usernameList); // 返回字符串列表
    }


//    @GetMapping("/download/{fileName}")
//    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
//        try {
//            Path path = Paths.get(fileDir, fileName);
//            Resource resource = new UrlResource(path.toUri());
//            if (!resource.exists()) return ResponseEntity.notFound().build();
//
//            return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
//                    .body(resource);
//        } catch (MalformedURLException e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }

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
