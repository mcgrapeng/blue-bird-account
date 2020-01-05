package com.zhangpeng.account.api;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class AccountREQ {

    /*********扫码支付参数清单***********/

    /**支付金额（元）*/
    private BigDecimal totalAmount;

    /**支付成功回调地址*/
    private String  successNotifyUrl;

    /**订单失效时间（秒）*/
    private String  expireTime;

    /**交易签名*/
    private String  sign;

    /**交易备注*/
    private String  remark;

    /**分账信息*/
    private String  billSplitList;


    /******************支付回调参数清单*********************/
    /**业务结果*/
    private String  resultCode;
    /**错误代码*/
    private String errCode;
    /**错误代码描述*/
    private String errCodeDes;
    /**平台订单号*/
    //private String tradeNo;
    /**交易状态*/
    private String payStatus;
    /**币种*/
    private String currency;
    /**交易金额*/
    private String amount;
    /**回传参数*/
    private String returnParams;
    /**交易成功时间*/
    private String payFinishTime;

    /*******************统一下单参数菜单**********************/
    /**商户号*/
    private String  merchantNo;

    /**商户订单号*/
    private String  merchantOrderNo;
    /**商户支付流水号*/
    private String trxNo;

    /**第三方订单号*/
    private String  tradeOrderNo;
    /**第三方流水号*/
    private String  tradeNo;

    /**支付产品号*/
    private String  productNo;

    /**机构号*/
    private String  organNo;

    /**产品名称*/
    private String productName;

    /**商户名称*/
    private String merchantName;

    /**用户名称*/
    private String userName;

    /**下单日期*/
    private Date  orderDate;

    /**下单时间*/
    private Date  orderTime;

    /**下单IP*/
    private String orderIp;

    /**支付金额（元）*/
    private BigDecimal orderAmount;

    //private String merchantNo;
    /**支付回传订单号*/
    private String outTradeNo;
    private String appId;
    private String openId;
    /**支付类型*/
    private String payType;
    /**接入方式*/
    private String accessMode;
   // private String agencyCode;
    /**回调地址*/
    private String notifyUrl;
    /**页面回调地址*/
    private String paySuccessUrl;

}
