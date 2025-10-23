package com.shms.deployrabbitmq.pojo;


import lombok.*;

import java.io.Serializable;

@Data
@Getter
@Setter
public class UserStatusMessage implements Serializable {
    private String username;      // 用户名
    private String status;      // online / offline
    private long timestamp;     // 事件时间戳
}
