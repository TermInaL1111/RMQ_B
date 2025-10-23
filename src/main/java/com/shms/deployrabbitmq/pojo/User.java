package com.shms.deployrabbitmq.pojo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

//消息传输对象User  实现序列化接口Serializable
//get set
@Data
@Getter
@Setter
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 7186944970241886064L;
    private String username;
    private String password;
}
