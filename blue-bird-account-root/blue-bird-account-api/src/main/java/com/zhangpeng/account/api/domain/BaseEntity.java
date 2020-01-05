package com.zhangpeng.account.api.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BaseEntity {
    private Integer id;
    private String status;// 状态 PublicStatusEnum
    private String creater;// 创建人.
    private Date createTime = new Date();// 创建时间.
    private String editor;// 修改人.
    private Date editTime;// 修改时间.
    private String remark;// 描述
}
