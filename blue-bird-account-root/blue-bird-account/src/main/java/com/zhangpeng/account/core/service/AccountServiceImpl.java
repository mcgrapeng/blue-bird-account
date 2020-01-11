
package com.zhangpeng.account.core.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.google.common.collect.Maps;
import com.zhangpeng.account.api.domain.Account;
import com.zhangpeng.account.api.service.AccountService;
import com.zhangpeng.account.core.mapper.AccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 账户service实现类
 */
@Slf4j
@Service(timeout = 60000,retries = 0,interfaceClass = AccountService.class)
@Component("accountService")
public class AccountServiceImpl implements AccountService {

	@Autowired
	private AccountMapper accountMapper;
	
	@Override
	public void saveData(Account account) {
		try {
			accountMapper.insert(account);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}

	@Override
	public void updateData(Account account) {
		try {
			accountMapper.update(account);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}

	@Override
	public Account getDataById(Integer id) {
		Account account = null;
		try {
			account = accountMapper.selectById(id);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return account;
	}

	@Override
	public Account getAccount(String userNo) {
		Account account = null;
		Map<String,Object> params = Maps.newHashMap();
		params.put("userNo",userNo);
		try {
			account = accountMapper.getBy(params);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return account;
	}
}