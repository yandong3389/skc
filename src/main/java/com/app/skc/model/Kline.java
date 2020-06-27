package com.app.skc.model;

import com.app.skc.enums.KlineEum;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("skc_kline")
public class Kline extends Model<Kline> {

    /**
     * 开盘时间
     */
    private Date startTime;
    /**
     * 收盘时间
     */
    private Date endTime;
    /**
     * 时间间隔
     */
    private String interval;
    /**
     * 前收盘价
     */
    private String preEndPrice = "0.0000";
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
    private String totalQuantity = "0.0000";
    /**
     * 成交额
     */
    private String totalAmount = "0.0000";
    /**
     * 主动买入成交量
     */
    private String activeQuantity = "0.0000";
    /**
     * 主动买入成交额
     */
    private String activeAmount = "0.0000";
    /**
     * 成交笔数
     */
    private Integer transNum = 0;

    private Date createTime;
    private Date modifyTime;

    @Override
    protected Serializable pkVal() {
        return null;
    }

    public Kline() {
    }

    public Kline(Date startTime, Date endTime, KlineEum klineEum) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = klineEum.getCode();
    }
}
