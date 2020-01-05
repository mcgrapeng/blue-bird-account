package com.zhangpeng.account.core.mapper;

import com.zhangpeng.account.api.domain.Account;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 账户mapper
 */
@Mapper
public interface AccountMapper{

	Account getBy(Map<String, Object> map);

	List<Account> listPage(Map<String, Object> params);

	Long listPageCount(Map<String, Object> params);

	List<Account> listBy(Map<String, Object> params);

	void insert(Account accountHistory);

	void update(Account accountHistory);

	Account selectById(Integer id);

	void deleteById(Integer id);

}