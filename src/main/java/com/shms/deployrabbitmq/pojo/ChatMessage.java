package com.shms.deployrabbitmq.pojo;

import lombok.*;

import java.io.Serializable;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L; // 必须添加序列化版本号
    private String messageId;  // 新增 消息id
    private String type;       // message / file
    private String sender;     // 发送方用户名
    private String receiver;   // 接收方（"all" 表示群发）
    private String content;    // 文本内容或文件名
    private byte[] fileData;   // 若为文件则存字节
    private long timestamp;    // 时间戳
}
