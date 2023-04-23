package com.zhangyin.region.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyResponse {
    public StatusCode statusCode;
    public String msg;
    public String type;

    public static MyResponse success(String type) {
        return new MyResponse(StatusCode.SUCCESS, "", type);
    }

    public static MyResponse success(String msg, String type) {
        return new MyResponse(StatusCode.SUCCESS, msg, type);
    }

    public static MyResponse fail(String type) {
        return new MyResponse(StatusCode.FAIL, "", type);
    }

    public static MyResponse fail(String msg, String type) {
        return new MyResponse(StatusCode.FAIL, msg, type);
    }
}
