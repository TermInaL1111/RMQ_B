package com.shms.deployrabbitmq.config;

import lombok.Getter;

@Getter
public enum MqConfigEnum {
    DIRECT("direct-exchange","cug-direct"),
    ;
    private String exchange;
    private String key;
    MqConfigEnum(String exchange, String key) {
        this.exchange = exchange;
        this.key = key;
    }
    //get set
//     String getExchange() {
//        return exchange;
//    }
    //    String getKey()
//    {
//        return key;
//    }
    void setExchange(String exchange) {
        this.exchange = exchange;
    }

    void setKey(String key) {
        this.key = key;
    }

}
