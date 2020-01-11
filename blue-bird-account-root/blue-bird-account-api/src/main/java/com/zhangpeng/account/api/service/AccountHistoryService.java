package com.zhangpeng.account.api.service;

import com.zhangpeng.account.api.domain.AccountHistory;

import java.math.BigDecimal;

/**
 * 账户历史service接口
 */
public interface AccountHistoryService {

	/**
	 * 为用户创建账单
	 */
	void createAccountHistory(String requestNo ,String userNo ,String trxType
			,String fundDirection, BigDecimal amount);
	
	/**
	 * 保存
	 */
	void saveData(AccountHistory accountHistory);

	/**
	 * 更新
	 */
	void updateData(AccountHistory accountHistory);

	/**
	 * 根据id获取数据
	 * 
	 * @param id
	 * @return
	 */
	AccountHistory getDataById(Integer id);
}