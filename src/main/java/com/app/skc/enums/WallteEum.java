package com.app.skc.enums;

public enum  WallteEum {
    ETH("01","ETH"),
    SKC("02","SKC"),
    USTD("03","USTD");
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
