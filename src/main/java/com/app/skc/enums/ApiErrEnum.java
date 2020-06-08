package com.app.skc.enums;

/**
 * api 错误枚举
 */
public enum ApiErrEnum {
    CREATE_WALLET_FAIL("1001","创建钱包失败");
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
