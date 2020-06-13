package com.app.skc.utils;

import java.util.UUID;

public class BaseUtils {

    /**
     * 校验是否是空
     * @param o 校验参数
     * @return true：空（nullor空字符串），false：非空
     */
    protected boolean isEmpty(Object o){
        return o == null || "".equals(o);
    }

    /**
     * 生成数据Id
     * @return uuid
     */
    protected String getDataId(){
        return UUID.randomUUID().toString();
    }

    public static String get64UUID(){
        UUID uuid = UUID.randomUUID();
        return  uuid.toString().replaceAll("-", "");
    }

    /**
     * 校验非空参数
     */
    public static boolean checkEmpty(Object o){
        return o != null && !"".equals(o);
    }
}
