package com.app.skc.enums;

public enum  WallteEum {
    ETH("1","ETH"),
    SKC("2","SKC"),
    USDT("3","USDT");
    /**
     * type 钱包类型
     */
    private String type;
    /**
     * code 钱包代码
     */
    private String code;


    WallteEum(String type,String code){
        this.code = code;
        this.type = type;
    }
    public String getCode() {
        return code;
    }
    public String getType() { return type; }

    public WallteEum setCode(String code) {
        this.code = code;
        return this;
    }

    public WallteEum setType(String type){
        this.type = type;
        return this;
    }

}
