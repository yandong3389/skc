package com.app.skc.enums;

public enum SysConfigEum {
    /**
     * USDT交易手续费
     *
     */
    USDT_TRANS_FEE("USDT_TRANS_FEE","USDT交易转账手续费"),
    SKC_TRANS_FEE("SKC_TRANS_FEE","SKC交易转账手续费"),
    WALLET_ADDRESS("WALLET_ADDRESS","公司钱包地址"),
    WALLET_PATH("WALLET_PATH","钱包物理地址");


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
