package com.zhangpeng.account.core.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.zhangpeng.account.api.domain.AccountHistory;
import com.zhangpeng.account.api.service.AccountHistoryService;
import com.zhangpeng.account.core.mapper.AccountHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 账户历史service实现类
 */
@Slf4j
@Service(timeout = 60000,retries = 0,interfaceClass = AccountHistoryService.class)
@Component("accountHistoryService")
public class AccountHistoryServiceImpl implements AccountHistoryService {

	@Autowired
	private AccountHistoryMapper accountHistoryMapper;
	
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