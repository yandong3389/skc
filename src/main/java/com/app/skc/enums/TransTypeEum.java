package com.app.skc.enums;

/**
 * 交易类型枚举
 */
public enum TransTypeEum {
    TRANSFER("0", "转账"),
    BUY("1", "买入"),
    SELL("2", "卖出"),
    OUT("3", "提现"),
    IN("4", "充值"),
    FREEZE("5", "冻结"),
    UNFREEZE("6", "解冻"),
    CONTRACT("7","购买合约");
    /**
     * 描述
     */
    private String desc;
    /**
     * code代码
     */

    private String code;

    TransTypeEum(String code, String desc) {
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
