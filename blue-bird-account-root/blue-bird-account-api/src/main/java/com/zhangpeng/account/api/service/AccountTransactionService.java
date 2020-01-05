package com.zhangpeng.account.api.service;

import com.zhangpeng.account.api.domain.Account;

import java.math.BigDecimal;

/**
 *  账户操作service接口
 */
public interface AccountTransactionService {

	/** 加款:有银行流水 **/
	Account creditToAccount(String userNo, BigDecimal amount, String requestNo, String bankTrxNo, String trxType, String remark);

	/** 减款 :有银行流水**/
	Account debitToAccount(String userNo, BigDecimal amount, String requestNo, String bankTrxNo, String trxType, String remark);
	
	/** 加款 **/
	Account creditToAccount(String userNo, BigDecimal amount, String requestNo, String trxType, String remark);

	/** 减款 **/
	Account debitToAccount(String userNo, BigDecimal amount, String requestNo, String trxType, String remark);

	/** 冻结 **/
	Account freezeAmount(String userNo, BigDecimal freezeAmount);

	/** 结算成功：解冻+减款 **/
	Account unFreezeAmount(String userNo, BigDecimal amount, String requestNo, String trxType, String remark);
	
	/** 结算失败：解冻 **/
	Account unFreezeSettAmount(String userNo, BigDecimal amount);
}