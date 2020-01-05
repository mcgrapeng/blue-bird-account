package com.zhangpeng.account.api.service;

import com.zhangpeng.account.api.domain.Account;

/**
 *  账户service接口
 */
public interface AccountService {
	
	/**
	 * 保存
	 */
	void saveData(Account acount);

	/**
	 * 更新
	 */
	void updateData(Account account);

	/**
	 * 根据id获取数据
	 * 
	 * @param id
	 * @return
	 */
	Account getDataById(Integer id);
}