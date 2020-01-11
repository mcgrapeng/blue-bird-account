package com.zhangpeng.account.core.controller;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class BindAccountREQ implements Serializable {

    @NotBlank(message = "账号不能为空")
    private String accountNo;
    @NotBlank(message = "账户名称不能为空")
    private String accountName;

}
