package com.app.skc.enums;

import org.apache.commons.lang3.StringUtils;

public enum WalletEum {
    ETH("1", "ETH"),
    SK("2", "SK"),
    USDT("3", "USDT");

    /**
     * code 钱包代码
     */
    private String code;

    /**
     * type 钱包类型
     */
    private String type;

    WalletEum(String type, String code) {
        this.code = code;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public WalletEum setCode(String code) {
        this.code = code;
        return this;
    }

    public WalletEum setType(String type) {
        this.type = type;
        return this;
    }

    public static WalletEum getByCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (WalletEum walletEum : WalletEum.values()) {
            if (walletEum.code.equals(code)) {
                return walletEum;
            }
        }
        return null;
    }

}
