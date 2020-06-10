package com.app.skc.enums;

/**
 * 交易枚举
 */
public enum  TransactionEum {
    TRANSFER("0","转账"),
    TRAD("1","交易"),
    OUT("2","提现"),
    IN("3","充值"),
    FINISH("0","完成");
    /**
     * 描述
     */
    private String desc;
    /**
     * code代码
     */

    private String code;

    TransactionEum(String code,String desc)
    {
        this.code = code;
        this.desc = desc;
    }
    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
