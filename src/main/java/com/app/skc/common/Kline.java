package com.app.skc.common;

import lombok.Data;

import java.util.Date;

@Data
public class Kline {

    /**
     * 今天的日期
     */
    private Date date;

    /**
     *date-日期 ;
     * line-每分钟成交价 :
     * 数组长度2440, 每分钟对应一个下标
     * 00:00对应line[0]
     * 23:59对应line[2339]
     */
    private String[] line;

    public Kline() {
    }

    public Kline(Date date, String[] line) {
        this.date = date;
        this.line = line;
    }
}
