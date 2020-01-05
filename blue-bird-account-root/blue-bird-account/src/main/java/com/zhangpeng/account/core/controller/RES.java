package com.zhangpeng.account.core.controller;

import java.io.Serializable;

public class RES<T> implements Serializable {

    private int code;
    private T data;
    private String message;

    public RES(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message= message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RES(int code, String message) {
        this(code,null,message);
    }

    public static <T> RES<T> of(int code, T data, String message){
        return new RES<T>(code,data,message);
    }

    public static <T> RES<T> of(int code, String message){
        return new RES<T>(code,message);
    }

}
