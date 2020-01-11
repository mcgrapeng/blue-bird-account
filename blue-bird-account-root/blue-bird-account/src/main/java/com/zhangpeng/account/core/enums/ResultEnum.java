package com.zhangpeng.account.core.enums;

/**
 * ajax结果枚举
 */
public enum ResultEnum {

    /**
     * 处理成功
     */
    处理成功(200),

    处理失败(500),
    /**
     * 请求参数不完整
     */
    请求参数不完整(00001),

    /**
     * 信息已存在
     */
    信息已存在(00002),

    /**
     * 请求参数错误
     */
    请求参数错误(00003),

    /**
     * 信息不存在
     */
    信息不存在(00004),

    您尚未绑定账户(00010),

    /**
     * 信息不允许修改
     */
    信息不允许修改(00005),

    /**
     * 用户已存在
     */
    用户已存在(10001),

    /**
     * 用户登录失败
     */
    用户登录失败(50000),

    /**
     * 用户登陆过期
     */
    用户登陆过期(50001),

    /**
     * 权限不足
     */
    权限不足(50002),

    /**
     * 旧密码校验失败
     */
    旧密码校验失败(50003),


    验证码已过期(60001),
    ;

    public int code;

    ResultEnum(int code) {
        this.code = code;
    }
}
