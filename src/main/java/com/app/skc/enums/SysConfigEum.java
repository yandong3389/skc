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
    CONTRACT_DOUBLE("CONTRACT_DOUBLE", "合约倍率"),
    CONTR_STATIC_RATE("CONTR_STATIC_RATE", "合约静态收益率"),
    CONTR_STATIC_RATE_DISCOUNT("CONTR_STATIC_RATE_DISCOUNT", "合约静态收益率-损益"),
    CONTR_SHARE_RATE_G1("CONTR_SHARE_RATE_G1", "合约分享一代收益率"),
    CONTR_SHARE_RATE_G2("CONTR_SHARE_RATE_G2", "合约分享二代收益率"),
    CONTR_SHARE_RATE_G3("CONTR_SHARE_RATE_G3", "合约分享三代收益率"),
    CONTR_SHARE_RATE_G4("CONTR_SHARE_RATE_G4", "合约分享四代收益率"),
    CONTR_SHARE_RATE_G5("CONTR_SHARE_RATE_G5", "合约分享五代收益率"),
    CONTR_SHARE_RATE_GX("CONTR_SHARE_RATE_GX", "合约分享6-10代收益率"),
    CONTR_MNG_RATE_BRONZE("CONTR_MNG_RATE_BRONZE", "合约青铜社区收益率"),
    CONTR_MNG_RATE_GOLD("CONTR_MNG_RATE_GOLD", "合约黄金社区收益率"),
    CONTR_MNG_RATE_DIAMOND("CONTR_MNG_RATE_DIAMOND", "合约钻石社区收益率"),
    CONTR_MNG_RATE_KING("CONTR_MNG_RATE_KING", "合约王者社区收益率"),
    CONTR_MNG_RATE_GOD("CONTR_MNG_RATE_GOD", "合约创神社区收益率"),

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
