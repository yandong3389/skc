package com.app.skc.enums;

public enum InfuraInfo {

    SKC_CONTRACT_ADDRESS("0x2532671395eddda28cf60770a4f6df0b19dc0087"),
    USDT_CONTRACT_ADDRESS("0xdac17f958d2ee523a2206206994597c13d831ec7"),
    ETH_FINNEY("1000"),
    MDC_ETH("1000000000000000000"),
    USDT_ETH("1000000"),
    GAS_PRICE("45"),
    GAS_SIZE("100000");






    private String desc;
    InfuraInfo(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public InfuraInfo setDesc(String desc) {
        this.desc = desc;
        return this;
    }
}
