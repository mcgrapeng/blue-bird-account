package com.zhangpeng.account.core.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.zhangpeng.account.api.domain.Account;
import com.zhangpeng.account.api.domain.AccountHistory;
import com.zhangpeng.account.api.enums.PublicEnum;
import com.zhangpeng.account.api.service.AccountHistoryService;
import com.zhangpeng.account.api.service.AccountQueryService;
import com.zhangpeng.account.core.mapper.AccountHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 账户历史service实现类
 */
@Slf4j
@Service(timeout = 60000,retries = 0,interfaceClass = AccountHistoryService.class)
@Component("accountHistoryService")
public class AccountHistoryServiceImpl implements AccountHistoryService {

	@Autowired
	private AccountHistoryMapper accountHistoryMapper;

	@Autowired
	private AccountQueryService accountQueryService;

	@Override
	public void createAccountHistory(String requestNo , String userNo  ,String trxType ,String fundDirection
			,BigDecimal amount,String remark) {

		// 记录账户历史
		AccountHistory accountHistoryEntity = new AccountHistory();
		accountHistoryEntity.setCreateTime(new Date());
		accountHistoryEntity.setEditTime(new Date());
		accountHistoryEntity.setIsAllowSett(PublicEnum.NO.name());
		accountHistoryEntity.setAmount(amount);
		Account account = null;
		try {
			account = accountQueryService.getAccountByUserNo(userNo);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw e;
		}
		accountHistoryEntity.setBalance(account.getBalance());
		accountHistoryEntity.setRequestNo(requestNo);
		accountHistoryEntity.setBankTrxNo(null);
		accountHistoryEntity.setIsCompleteSett(PublicEnum.NO.name());
		accountHistoryEntity.setRemark(remark);
		accountHistoryEntity.setFundDirection(fundDirection);
		accountHistoryEntity.setAccountNo(account.getAccountNo());
		accountHistoryEntity.setTrxType(trxType);
		accountHistoryEntity.setUserNo(userNo);
		saveData(accountHistoryEntity);
	}

	@Override
	public void saveData(AccountHistory rpAccountHistory) {
		try {
			accountHistoryMapper.insert(rpAccountHistory);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}

	@Override
	public void updateData(AccountHistory rpAccountHistory) {
		try {
			accountHistoryMapper.update(rpAccountHistory);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}

	@Override
	public AccountHistory getDataById(Integer id) {
		AccountHistory accountHistory = null;
		try {
			accountHistory = accountHistoryMapper.selectById(id);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return accountHistory;
	}

}