
package com.zhangpeng.account.core.service;

import com.zhangpeng.account.api.domain.Account;
import com.zhangpeng.account.api.service.AccountService;
import com.zhangpeng.account.core.mapper.AccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 账户service实现类
 */
@Slf4j
@Service("accountService")
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

}