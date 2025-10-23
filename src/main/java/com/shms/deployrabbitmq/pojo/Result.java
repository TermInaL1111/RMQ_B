package com.shms.deployrabbitmq.pojo;

import lombok.Data;

//请求返回结果封装
@Data
public class Result {

    private Integer code;//编码 1,0 成功失败
    private String msg;//错误信息
    private Object data;//数据

    public static Result success(){
        Result result = new Result();
        result.code = 1;
        result.msg = "success";
        return result;
    }
    public static Result success(Object object){
        Result result = new Result();
        result.code = 1;
        result.msg = "success";
        result.data = object;
        return result;
    }
    public static Result error(String msg){
        Result result = new Result();
        result.code = 0;
        result.msg = msg;
        return result;
    }

}