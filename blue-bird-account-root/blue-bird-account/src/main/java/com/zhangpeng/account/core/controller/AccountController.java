package com.zhangpeng.account.core.controller;

import com.google.common.collect.Maps;
import com.zhangpeng.account.api.AccountRES;
import com.zhangpeng.account.api.PageBean;
import com.zhangpeng.account.api.PageParam;
import com.zhangpeng.account.api.domain.Account;
import com.zhangpeng.account.api.domain.AccountHistory;
import com.zhangpeng.account.api.enums.TrxTypeEnum;
import com.zhangpeng.account.api.service.AccountQueryService;
import com.zhangpeng.account.api.service.AccountTransactionService;
import com.zhangpeng.account.core.enums.ResultEnum;
import com.zhangpeng.sso.api.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/account")
public class AccountController extends BaseController {

    @Autowired
    private AccountQueryService accountQueryService;

    @Autowired
    private AccountTransactionService accountTransactionService;

    @RequestMapping(value = "/balance",method = RequestMethod.POST)
    @ResponseBody
    public AccountRES<Account>  getBalance(){
        User user = getLoginUser();
        String userNo = user.getUserName();
        Account account = null;
        try {
            account = accountQueryService.getAccountByUserNo(userNo);
        } catch (Exception e) {
           log.error(e.getMessage(),e);
            return AccountRES.of(String.valueOf(ResultEnum.处理失败.code),account,ResultEnum.处理失败.name());
        }
        return AccountRES.of(String.valueOf(ResultEnum.处理成功.code),account,ResultEnum.处理成功.name());
    }


    @RequestMapping(value = "/withdraw",method = RequestMethod.POST)
    @ResponseBody
    public AccountRES<Account> withdraw(@RequestParam(value = "amount",required = false) String amount){
        User user = getLoginUser();
        String userNo = user.getUserName();
        Account account = null;
        try {
            account = accountTransactionService.debitToAccount(userNo, new BigDecimal(amount) ,"", TrxTypeEnum.WITHDRAW.name(), "卡卡得提现");
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return AccountRES.of(String.valueOf(ResultEnum.处理失败.code),account,ResultEnum.处理失败.name());
        }
        return AccountRES.of(String.valueOf(ResultEnum.处理成功.code),account,ResultEnum.处理成功.name());
    }



    @RequestMapping(value = "/withdraw-record",method = RequestMethod.POST)
    @ResponseBody
    public AccountRES<PageBean<AccountHistory>> withdrawRecord(PageParam pageParam ){
        User user = getLoginUser();
        String userNo = user.getUserName();
        Account account = accountQueryService.getAccountByUserNo(userNo);
        String accountNo = account.getAccountNo();
        Map<String,Object> params = Maps.newHashMap();
        params.put("accountNo",accountNo);
        params.put("trxType", TrxTypeEnum.WITHDRAW.name());
        PageBean<AccountHistory>  page;
        try {
            page = accountQueryService.pageAccountHistory(pageParam,  params);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return AccountRES.of(String.valueOf(ResultEnum.处理失败.code),ResultEnum.处理失败.name());
        }
        return AccountRES.of(String.valueOf(ResultEnum.处理成功.code),page,ResultEnum.处理成功.name());
    }

}
