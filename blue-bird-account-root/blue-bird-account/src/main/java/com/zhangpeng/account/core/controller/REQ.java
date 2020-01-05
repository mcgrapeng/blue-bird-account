package com.zhangpeng.account.core.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@Slf4j
@Data
public class REQ<T> implements Serializable {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String verifycode;


    public static <T> Map<String,String> bean2Map(T bean){
        Map<String,String> describe = null;
        try {
            describe = BeanUtils.describe(bean);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error(e.getMessage(),e);
        }
        return describe;
    }

}
