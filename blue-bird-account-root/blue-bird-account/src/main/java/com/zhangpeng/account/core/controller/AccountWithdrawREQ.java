package com.zhangpeng.account.core.controller;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AccountWithdrawREQ implements Serializable {

    @NotBlank(message = "提现金额不能为空")
    private String amount;

}
