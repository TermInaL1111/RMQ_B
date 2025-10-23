package com.shms.deployrabbitmq.pojo;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatMessage implements Serializable {
    private String type;       // message / file / system
    private String sender;     // 发送方用户名
    private String receiver;   // 接收方（"all" 表示群发）
    private String content;    // 文本内容或文件名
    private byte[] fileData;   // 若为文件则存字节
    private long timestamp;    // 时间戳
}
