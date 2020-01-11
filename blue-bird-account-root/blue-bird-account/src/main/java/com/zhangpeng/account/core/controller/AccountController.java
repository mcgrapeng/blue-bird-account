package com.zhangpeng.account.core.controller;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.zhangpeng.account.api.AccountRES;
import com.zhangpeng.account.api.PageBean;
import com.zhangpeng.account.api.PageParam;
import com.zhangpeng.account.api.domain.Account;
import com.zhangpeng.account.api.domain.AccountHistory;
import com.zhangpeng.account.api.enums.AccountFundDirectionEnum;
import com.zhangpeng.account.api.enums.AccountTypeEnum;
import com.zhangpeng.account.api.enums.PublicStatusEnum;
import com.zhangpeng.account.api.enums.TrxTypeEnum;
import com.zhangpeng.account.api.service.AccountHistoryService;
import com.zhangpeng.account.api.service.AccountQueryService;
import com.zhangpeng.account.api.service.AccountService;
import com.zhangpeng.account.api.service.AccountTransactionService;
import com.zhangpeng.account.core.enums.ResultEnum;
import com.zhangpeng.account.core.utils.CommonUtils;
import com.zhangpeng.sso.api.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.Valid;
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

    @Autowired
    private AccountHistoryService accountHistoryService;

    @RequestMapping(value = "/balance", method = RequestMethod.POST)
    @ResponseBody
    public AccountRES<Account> getBalance() {
        User user = getLoginUser();
        if(null == user){
            return AccountRES.of(ResultEnum.您尚未登录.code, ResultEnum.您尚未登录.name());
        }
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
    public AccountRES<Boolean> withdraw(@RequestBody @Valid AccountWithdrawREQ req, BindingResult bindingResult) {

        User user = getLoginUser();
        if(null == user){
            return AccountRES.of(ResultEnum.您尚未登录.code, Boolean.FALSE, ResultEnum.您尚未登录.name());
        }

        if (bindingResult.hasErrors()) {
            String errMsg = bindingResult.getFieldError().getDefaultMessage();
            log.error("用户提现，请求参数校验失败，{}，请求数据 {}", errMsg, JSON.toJSONString(req));
            return AccountRES.of(ResultEnum.请求参数不完整.code, Boolean.FALSE, ResultEnum.请求参数不完整.name());
        }

        String amount = req.getAmount();
        if (!CommonUtils.isNumber(amount)) {
            return AccountRES.of(ResultEnum.请求参数错误.code, Boolean.FALSE, ResultEnum.请求参数错误.name());
        }

        BigDecimal withdrawAmount = new BigDecimal(amount);
        if (withdrawAmount.compareTo(BigDecimal.ZERO) < 1) {
            return AccountRES.of(ResultEnum.请求参数错误.code, Boolean.FALSE, ResultEnum.请求参数错误.name());
        }


        String userNo = user.getUserName();
        try {
            accountHistoryService.createAccountHistory("KKD" + System.currentTimeMillis()
                    , userNo, TrxTypeEnum.WITHDRAW.name()
                    , AccountFundDirectionEnum.SUB.name(), withdrawAmount, "用户= {" + user.getUserName() + "," + user.getNickName() + "},提现账单");

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return AccountRES.of(ResultEnum.您尚未绑定账户.code, Boolean.FALSE, ResultEnum.您尚未绑定账户.name());
        }
        return AccountRES.of(ResultEnum.处理成功.code, Boolean.TRUE, ResultEnum.处理成功.name());
    }


    @RequestMapping(value = "/withdraw-record", method = RequestMethod.POST)
    @ResponseBody
    public AccountRES<PageBean<AccountHistory>> withdrawRecord(@RequestBody @Valid PageParam pageParam) {
        User user = getLoginUser();
        if(null == user){
            return AccountRES.of(ResultEnum.您尚未登录.code, ResultEnum.您尚未登录.name());
        }
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
    public AccountRES<Boolean> bindAccount(@RequestBody @Valid BindAccountREQ req, BindingResult bindingResult) {
        User user = getLoginUser();
        if(null == user){
            return AccountRES.of(ResultEnum.您尚未登录.code, Boolean.FALSE, ResultEnum.您尚未登录.name());
        }
        if (bindingResult.hasErrors()) {
            String errMsg = bindingResult.getFieldError().getDefaultMessage();
            log.error("绑定账户，请求参数校验失败，{}，请求数据 {}", errMsg, JSON.toJSONString(req));
            return AccountRES.of(ResultEnum.请求参数不完整.code, Boolean.FALSE, ResultEnum.请求参数不完整.name());
        }

        //TODO  待优化  账户被恶意绑定

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
            account.setAccountNo(req.getAccountNo());
            account.setAccountName(req.getAccountName());
            accountService.saveData(account);
        }
        return AccountRES.of(ResultEnum.处理成功.code, ResultEnum.处理成功.name());
    }

}
