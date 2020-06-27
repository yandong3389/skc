package com.app.skc.model;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;
import org.apache.commons.lang3.time.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@TableName("skc_kline")
public class Kline {

    /**
     * 开盘时间
     */
    private Date startTime;
    /**
     * 收盘时间
     */
    private Date endTime;
    /**
     * 开盘价
     */
    private String startPrice = "0.0000";
    /**
     * 收盘价
     */
    private String endPrice = "0.0000";
    /**
     * 最高价
     */
    private String maxPrice = "0.0000";
    /**
     * 最低价
     */
    private String minPrice = "0.0000";
    /**
     * 成交量
     */
    private String quantity = "0.0000";
    /**
     * 成交额
     */
    private String totalAmount = "0.0000";
    /**
     * 成交笔数
     */
    private Integer transNum = 0;

    public Kline() {
    }

    public Kline(Date startTime, Date endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static void main(String[] args) {
        Date now = new Date();
        Kline kline = new Kline(DateUtils.addMinutes(now, -15), now);
        List<Kline> klineList = new ArrayList<>();
        klineList.add(kline);
        System.out.printf(JSON.toJSONString(klineList));
    }
}
