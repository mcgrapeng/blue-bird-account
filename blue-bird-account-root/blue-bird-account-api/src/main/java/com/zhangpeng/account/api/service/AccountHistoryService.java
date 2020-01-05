package com.zhangpeng.account.api.service;

import com.zhangpeng.account.api.domain.AccountHistory;

/**
 * 账户历史service接口
 */
public interface AccountHistoryService {
	
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