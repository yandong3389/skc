package com.app.skc.enums;

public enum InfuraInfo {

    SKC_CONTRACT_ADDRESS("0x2532671395eddda28cf60770a4f6df0b19dc0087"),
    USDT_CONTRACT_ADDRESS("0xdac17f958d2ee523a2206206994597c13d831ec7"),
    ETH_FINNEY("1000"),
    MDC_ETH("1000000000000000000"),
    USDT_ETH("1000000"),
    GAS_PRICE("18"),
    GAS_SIZE("100000"),
    WALLET_ID("11"),
    WALLET_ADDRESS("0xeb04131fbe988d43c0f9c0d8a30ccc3636994dda"),
    WALLET_PATH("D:/walletTrue/UTC--2020-02-07T13-31-22.32000000Z--eb04131fbe988d43c0f9c0d8a30ccc3636994dda.json"),
    INFURA_ADDRESS("https://ropsten.infura.io/v3/4098a0ceccd5421fa162fb549adea10a");






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
