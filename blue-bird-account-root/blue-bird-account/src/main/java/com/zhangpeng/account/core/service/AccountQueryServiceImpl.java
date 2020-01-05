package com.zhangpeng.account.core.service;

import com.google.common.collect.Maps;
import com.zhangpeng.account.api.PageBean;
import com.zhangpeng.account.api.PageParam;
import com.zhangpeng.account.api.domain.Account;
import com.zhangpeng.account.api.domain.AccountHistory;
import com.zhangpeng.account.api.enums.PublicStatusEnum;
import com.zhangpeng.account.api.ex.AccountBizException;
import com.zhangpeng.account.api.service.AccountQueryService;
import com.zhangpeng.account.api.utils.DateUtils;
import com.zhangpeng.account.core.mapper.AccountHistoryMapper;
import com.zhangpeng.account.core.mapper.AccountMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 账户查询service实现类
 */
@Slf4j
@Service("accountQueryService")
public class AccountQueryServiceImpl implements AccountQueryService {
	@Autowired
	private AccountMapper accountMapper;
	@Autowired
	private AccountHistoryMapper accountHistoryMapper;


	/**
	 * 根据账户编号获取账户信息
	 * 
	 * @param accountNo
	 *            账户编号
	 * @return
	 */
	@Override
	public Account getAccountByAccountNo(String accountNo) {
		log.info("根据账户编号查询账户信息");
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("accountNo", accountNo);
		log.info("根据用户编号查询账户信息");
		Account account = null;
		try {
			account = this.accountMapper.getBy(map);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		if (account == null) {
			throw AccountBizException.ACCOUNT_NOT_EXIT;
		}
		// 不是同一天直接清0
		if (!DateUtils.isSameDayWithToday(account.getEditTime())) {
			account.setTodayExpend(BigDecimal.ZERO);
			account.setTodayIncome(BigDecimal.ZERO);
			account.setEditTime(new Date());
			try {
				accountMapper.update(account);
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
		return account;
	}

	/**
	 * 根据用户编号编号获取账户信息
	 * 
	 * @param userNo
	 *            用户编号
	 * @return
	 */
	@Override
	public Account getAccountByUserNo(String userNo) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("userNo", userNo);
		log.info("根据用户编号查询账户信息");
		Account account = null;
		try {
			account = this.accountMapper.getBy(map);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		if (account == null) {
			throw AccountBizException.ACCOUNT_NOT_EXIT;
		}
		// 不是同一天直接清0
		if (!DateUtils.isSameDayWithToday(account.getEditTime())) {
			account.setTodayExpend(BigDecimal.ZERO);
			account.setTodayIncome(BigDecimal.ZERO);
			account.setEditTime(new Date());
			try {
				accountMapper.update(account);
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
		return account;
	}

	@Override
	public PageBean<AccountHistory> pageAccountHistoryByAccountNo(PageParam pageParam, String accountNo) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("accountNo", accountNo);
		PageBean<AccountHistory> accountHistoryPageBean = pageAccountHistory(pageParam, params);
		return accountHistoryPageBean;
	}


	/**
	 * 获取账户历史单角色
	 * 
	 * @param accountNo
	 *            账户编号
	 * @param trxType
	 *            业务类型
	 * @return AccountHistory
	 */
	@Override
	public AccountHistory getAccountHistoryByAccountNo_TrxType(String accountNo, String trxType) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("accountNo", accountNo);
		map.put("trxType", trxType);
		AccountHistory by = null;
		try {
			by = accountHistoryMapper.getBy(map);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return by;
	}
	
    /**
	 * 获取所有账户
	 * @return
	 */
    @Override
    public List<Account> listAll(){
    	Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("status", PublicStatusEnum.ACTIVE.name());
		List<Account> accounts = null;
		try {
			accounts = accountMapper.listBy(paramMap);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return accounts;
	}


	/**
	 * 根据参数分页查询账户.
	 *
	 * @param pageParam
	 *            分页参数.
	 * @param map
	 *            查询参数，可以为null.
	 * @return AccountList.
	 */
	@Override
	public PageBean<Account> pageAccount(PageParam pageParam, Map<String, Object> map) {
		return accountPage(pageParam, map);
	}

	/**
	 * 根据参数分页查询账户历史.
	 *
	 * @param pageParam
	 *            分页参数.
	 * @param map
	 *            查询参数，可以为null.
	 * @return AccountHistoryList.
	 */
	@Override
	public PageBean<AccountHistory> pageAccountHistory(PageParam pageParam, Map<String, Object> map) {
		return accountHistoryPage( pageParam,  map);
	}


	/**
	 * 分页查询数据 .
	 */
	private PageBean<AccountHistory> accountHistoryPage(PageParam pageParam, Map<String, Object> paramMap) {
		if (paramMap == null) {
			paramMap = Maps.newHashMap();
		}

		// 统计总记录数
		Long totalCount = null;
		try {
			totalCount = accountHistoryMapper.listPageCount(paramMap);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}

		// 校验当前页数
		int currentPage = PageBean.checkCurrentPage(totalCount.intValue(), pageParam.getNumPerPage(), pageParam.getPageNum());
		pageParam.setPageNum(currentPage); // 为当前页重新设值
		// 校验页面输入的每页记录数numPerPage是否合法
		int numPerPage = PageBean.checkNumPerPage(pageParam.getNumPerPage()); // 校验每页记录数
		pageParam.setNumPerPage(numPerPage); // 重新设值

		// 根据页面传来的分页参数构造SQL分页参数
		paramMap.put("pageFirst", (pageParam.getPageNum() - 1) * pageParam.getNumPerPage());
		paramMap.put("pageSize", pageParam.getNumPerPage());
/*		paramMap.put("startRowNum", (pageParam.getPageNum() - 1) * pageParam.getNumPerPage());
		paramMap.put("endRowNum", pageParam.getPageNum() * pageParam.getNumPerPage());*/

		// 获取分页数据集
		List<AccountHistory> list = null;
		try {
			list = accountHistoryMapper.listPage(paramMap);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}

		return new PageBean<AccountHistory>(pageParam.getPageNum(), pageParam.getNumPerPage(), totalCount.intValue(), list);
	}


	/**
	 * 分页查询数据 .
	 */
	private PageBean<Account> accountPage(PageParam pageParam, Map<String, Object> paramMap) {
		if (paramMap == null) {
			paramMap = Maps.newHashMap();
		}

		// 统计总记录数
		Long totalCount = null;
		try {
			totalCount = accountMapper.listPageCount(paramMap);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}

		// 校验当前页数
		int currentPage = PageBean.checkCurrentPage(totalCount.intValue(), pageParam.getNumPerPage(), pageParam.getPageNum());
		pageParam.setPageNum(currentPage); // 为当前页重新设值
		// 校验页面输入的每页记录数numPerPage是否合法
		int numPerPage = PageBean.checkNumPerPage(pageParam.getNumPerPage()); // 校验每页记录数
		pageParam.setNumPerPage(numPerPage); // 重新设值

		// 根据页面传来的分页参数构造SQL分页参数
		paramMap.put("pageFirst", (pageParam.getPageNum() - 1) * pageParam.getNumPerPage());
		paramMap.put("pageSize", pageParam.getNumPerPage());
/*		paramMap.put("startRowNum", (pageParam.getPageNum() - 1) * pageParam.getNumPerPage());
		paramMap.put("endRowNum", pageParam.getPageNum() * pageParam.getNumPerPage());*/

		// 获取分页数据集
		List<Account> list = null;
		try {
			list = accountMapper.listPage(paramMap);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}

		return new PageBean<>(pageParam.getPageNum(), pageParam.getNumPerPage(), totalCount.intValue(), list);
	}

}