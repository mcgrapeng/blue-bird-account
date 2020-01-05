package com.zhangpeng.account.core.mapper;

import com.zhangpeng.account.api.domain.AccountHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 账户历史mapper
 */
@Mapper
public interface AccountHistoryMapper {

	List<AccountHistory> listPage(Map<String, Object> params);

	Long listPageCount(Map<String, Object> params);

	List<AccountHistory>  listBy(Map<String, Object> params);

	void insert(AccountHistory accountHistory);

	void update(AccountHistory accountHistory);

	AccountHistory selectById(Integer id);

	AccountHistory getBy(Map<String, Object> params);

	void deleteById(Integer id);
}