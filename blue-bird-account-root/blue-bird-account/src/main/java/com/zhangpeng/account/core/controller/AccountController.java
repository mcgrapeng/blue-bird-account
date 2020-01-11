package com.zhangpeng.account.core.controller;

import com.google.common.collect.Maps;
import com.zhangpeng.account.api.AccountRES;
import com.zhangpeng.account.api.PageBean;
import com.zhangpeng.account.api.PageParam;
import com.zhangpeng.account.api.domain.Account;
import com.zhangpeng.account.api.domain.AccountHistory;
import com.zhangpeng.account.api.enums.AccountTypeEnum;
import com.zhangpeng.account.api.enums.PublicStatusEnum;
import com.zhangpeng.account.api.enums.TrxTypeEnum;
import com.zhangpeng.account.api.service.AccountQueryService;
import com.zhangpeng.account.api.service.AccountService;
import com.zhangpeng.account.api.service.AccountTransactionService;
import com.zhangpeng.account.core.enums.ResultEnum;
import com.zhangpeng.sso.api.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    private AccountService accountService;

    @Autowired
    private AccountQueryService accountQueryService;

    @Autowired
    private AccountTransactionService accountTransactionService;

    @RequestMapping(value = "/balance", method = RequestMethod.POST)
    @ResponseBody
    public AccountRES<Account> getBalance() {
        User user = getLoginUser();
        String userNo = user.getUserName();
        Account account;
        try {
            account = accountQueryService.getAccountByUserNo(userNo);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AccountRES.of(ResultEnum.您尚未绑定账户.code, ResultEnum.您尚未绑定账户.name());
        }
        return AccountRES.of(ResultEnum.处理成功.code, account, ResultEnum.处理成功.name());
    }


    @RequestMapping(value = "/withdraw", method = RequestMethod.POST)
    @ResponseBody
    public AccountRES<Account> withdraw(@RequestParam(value = "amount", required = false) String amount) {
        User user = getLoginUser();
        String userNo = StringUtils.isBlank(user.getUserName()) ? user.getWxOpenId() : user.getUserName();
        Account account;
        try {
            account = accountTransactionService.debitToAccount(userNo, new BigDecimal(amount), "", TrxTypeEnum.WITHDRAW.name(), "卡卡得提现");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AccountRES.of(ResultEnum.您尚未绑定账户.code, ResultEnum.您尚未绑定账户.name());
        }
        return AccountRES.of(ResultEnum.处理成功.code, account, ResultEnum.处理成功.name());
    }


    @RequestMapping(value = "/withdraw-record", method = RequestMethod.POST)
    @ResponseBody
    public AccountRES<PageBean<AccountHistory>> withdrawRecord(PageParam pageParam) {
        User user = getLoginUser();
        String userNo = user.getUserName();
        Account account;
        try {
            account = accountQueryService.getAccountByUserNo(userNo);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AccountRES.of(ResultEnum.您尚未绑定账户.code, ResultEnum.您尚未绑定账户.name());
        }
        String accountNo = account.getAccountNo();
        Map<String, Object> params = Maps.newHashMap();
        params.put("accountNo", accountNo);
        params.put("trxType", TrxTypeEnum.WITHDRAW.name());
        return AccountRES.of(ResultEnum.处理成功.code
                , accountQueryService.pageAccountHistory(pageParam, params), ResultEnum.处理成功.name());
    }


    @RequestMapping(value = "/bind-account", method = RequestMethod.POST)
    @ResponseBody
    public AccountRES<String> bindAccount(@RequestParam(value = "accountNo", required = false) String accountNo
            , @RequestParam(value = "accountName", required = false) String accountName) {
        User user = getLoginUser();
        String userNo = user.getUserName();
        Account account = accountService.getAccount(userNo);
        if (null == account) {
            account = new Account();
            account.setAccountType(AccountTypeEnum.USER.name());
            account.setBalance(BigDecimal.ZERO);
            account.setUserNo(userNo);
            account.setTodayExpend(BigDecimal.ZERO);
            account.setSecurityMoney(BigDecimal.ZERO);
            account.setSettAmount(BigDecimal.ZERO);
            account.setTodayIncome(BigDecimal.ZERO);
            account.setTotalExpend(BigDecimal.ZERO);
            account.setTotalIncome(BigDecimal.ZERO);
            account.setUnbalance(BigDecimal.ZERO);
            account.setStatus(PublicStatusEnum.ACTIVE.name());
            account.setRemark("卡卡得账户");
            account.setCreater(userNo);
            account.setAccountNo(accountNo);
            account.setAccountName(accountName);
            accountService.saveData(account);
        }
        return AccountRES.of(ResultEnum.处理成功.code, ResultEnum.处理成功.name());
    }

}
