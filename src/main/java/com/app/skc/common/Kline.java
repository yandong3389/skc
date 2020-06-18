package com.app.skc.common;

import lombok.Data;

import java.util.Date;

@Data
public class Kline {

    private Date date;

    private String[] line;

    public Kline() {
    }

    public Kline(Date date, String[] line) {
        this.date = date;
        this.line = line;
    }
}
