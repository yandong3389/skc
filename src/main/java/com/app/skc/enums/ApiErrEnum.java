package com.app.skc.enums;

/**
 * api 错误枚举
 */
public enum ApiErrEnum {
    CREATE_WALLET_FAIL("1001", "创建钱包失败"),
    ADDRESS_WALLET_FAIL("1002", "地址错误"),
    NOT_ENOUGH_WALLET("1003", "账户余额不足"),
    TRANS_AMOUNT_INVALID("1004", "交易金额非法");
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
