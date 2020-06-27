package com.app.skc.enums;

import org.apache.commons.lang3.StringUtils;

public enum KlineEum {
    M15("15m", "15分钟"),
    D1("1d", "1天"),
    ;

    /**
     * code 钱包代码D
     */
    private final String code;

    /**
     * type 钱包类型
     */
    private final String type;

    KlineEum( String code,String type) {
        this.code = code;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public static KlineEum getByCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (KlineEum walletEum : KlineEum.values()) {
            if (walletEum.code.equals(code)) {
                return walletEum;
            }
        }
        return null;
    }



}
