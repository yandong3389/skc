package com.app.skc.enums;

import org.apache.commons.lang3.StringUtils;

public enum UserGradeEnum {
    FRESH("fresh", "小白(无等级)"),
    BRONZE("bronze", "青铜"),
    GOLD("gold", "黄金"),
    DIAMOND("diamond", "钻石"),
    KING("king", "王者"),
    GOD("god", "创神");

    /**
     * 等级描述
     */
    private String desc;
    /**
     * 等级代码
     */
    private String code;

    UserGradeEnum(String code, String desc) {
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

    public UserGradeEnum getByCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (UserGradeEnum eachGrade : UserGradeEnum.values()) {
            if (code.equals(eachGrade.getCode())) {
                return eachGrade;
            }
        }
        return null;
    }
}
