package com.zhangpeng.account.api.service;

import com.zhangpeng.account.api.PageBean;
import com.zhangpeng.account.api.PageParam;
import com.zhangpeng.account.api.domain.Account;
import com.zhangpeng.account.api.domain.AccountHistory;

import java.util.List;
import java.util.Map;

/**
 * 账户查询service接口
 */
public interface AccountQueryService {

	/**
	 * 根据账户编号获取账户信息
	 * 
	 * @param accountNo
	 *            账户编号
	 * @return
	 */
	Account getAccountByAccountNo(String accountNo);

	/**
	 * 根据用户编号编号获取账户信息
	 * 
	 * @param userNo
	 *            用户编号
	 * @return
	 */
	Account getAccountByUserNo(String userNo);

	// /////////////////////账户历史/////////////////////////////

	/**
	 * 根据账户编号分页查询账户历史单商户.
	 * 
	 * @param pageParam
	 *            分页参数.
	 * @param accountNo
	 *            账户编号.
	 * @return AccountHistoryList.
	 */
	PageBean<AccountHistory> pageAccountHistoryByAccountNo(PageParam pageParam, String accountNo);
	
	/**
	 * 获取账户历史
	 * 
	 * @param accountNo
	 *            账户编号
	 * @param trxType
	 *            业务类型
	 * @return AccountHistory
	 */
	AccountHistory getAccountHistoryByAccountNo_TrxType(String accountNo, String trxType);


	/**
	 * 获取所有账户
	 * @return
	 */
	List<Account> listAll();

	/**
	 * 根据参数分页查询账户.
	 *
	 * @param pageParam
	 *            分页参数.
	 * @param params
	 *            查询参数，可以为null.
	 * @return AccountList.
	 */
	PageBean<Account> pageAccount(PageParam pageParam, Map<String, Object> params);

	/**
	 * 根据参数分页查询账户历史.
	 *
	 * @param pageParam
	 *            分页参数.
	 * @param params
	 *            查询参数，可以为null.
	 * @return AccountHistoryList.
	 */
	PageBean<AccountHistory> pageAccountHistory(PageParam pageParam, Map<String, Object> params);
}