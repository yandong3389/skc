package com.app.skc.enums;

/**
 * api 错误枚举
 */
public enum ApiErrEnum {
    CREATE_WALLET_FAIL("1001", "创建钱包失败"),
    ADDRESS_WALLET_FAIL("1002", "地址错误"),
    NOT_ENOUGH_WALLET("1003", "账户余额不足"),
    REQ_PARAM_NOT_NULL("2001", "请求参数不能为空"),
    TRANS_AMOUNT_INVALID("1004", "交易金额非法"),
    WALLET_TYPE_NOT_SUPPORTED("1005", "不支持的钱包类型"),
    WALLET_NOT_MAINTAINED("1006", "钱包地址用户不存在"),
    USER_NOT_EXISTED("1007", "系统用户不存在"),

    NO_DEAL_PRICE("1101", "无最新成交价"),
    NO_COMMISSION("1102", "无委托交易"),
    NO_ENTRUST_ORDER("1103", "无委托交易"),
    ;
    /**
     * 描述
     */
    private String desc;
    /**
     * code
     */
    private String code;

    /**
     *
     * @param code
     * @param desc
     */
    ApiErrEnum(String code,String desc)
    {
        this.code = code;
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
    public String getCode() {
        return code;
    }
    public ApiErrEnum setDesc(String desc) {
        this.desc = desc;
        return this;
    }
    public ApiErrEnum setCode(String code) {
        this.code = code;
        return this;
    }
}
