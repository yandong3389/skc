package com.app.skc.enums;

/**
 * 交易状态枚举
 */
public enum TransStatusEnum {
    INIT("INIT", "初始(待审核)"),
    APPROVED("APPROVED", "审核通过"),
    REJECTED("REJECTED", "驳回"),
    SUCCESS("SUCCESS", "交易成功"),
    FAILED("FAILED", "交易失败"),
    EFFECT("EFFECT", "合约生效"),
    UNEFFECT("UNEFFECT", "合约出局");
    /**
     * 描述
     */
    private String desc;
    /**
     * code代码
     */

    private String code;

    TransStatusEnum(String code, String desc) {
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
