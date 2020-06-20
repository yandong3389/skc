package com.app.skc.enums;

public enum SysConfigEum {
    /**
     * USDT交易手续费
     */
    USDT_TRANS_FEE("USDT_TRANS_FEE", "USDT转账交易手续费"),
    SKC_TRANS_FEE("SKC_TRANS_FEE", "SKC转账交易手续费"),
    SKC_CASHOUT_FEE("SKC_CASHOUT_FEE", "SKC提现交易手续费"),
    USDT_CASHOUT_FEE("USDT_CASHOUT_FEE", "USDT提现交易手续费"),
    NEED_CASHOUT_VERIFY("NEED_CASHOUT_VERIFY", "提现交易是否需要审核"),
    ;


    /**
     * 名称
     */
    private String code;
    /**
     * 描述
     */
    private String desc;

    SysConfigEum(String code,String desc){
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
