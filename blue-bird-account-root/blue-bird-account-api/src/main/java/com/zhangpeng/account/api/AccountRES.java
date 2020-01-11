package com.zhangpeng.account.api;

public class AccountRES<T> {

    private int code;
    private T data;
    private String message;

    public AccountRES(int code, T data, String message) {
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

    public AccountRES(int code, String message) {
        this(code,null,message);
    }

    public static <T> AccountRES<T> of(int code, T data, String message){
        return new AccountRES<>(code,data,message);
    }

    public static <T> AccountRES<T> of(int code, String message){
        return new AccountRES<>(code,message);
    }

}
