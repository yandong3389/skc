package com.app.skc.common;

import com.app.skc.enums.TransTypeEum;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 *
 */
@Data
public class Exchange {
    /**
     * 单号
     */
    private String entrustOrder;
    /**
     * 类型 买入 / 卖出
     */
    private String type;
    /**
     * 挂单用户
     */
    private String userId;
    /**
     * 挂单价格
     */
    private BigDecimal price;
    /**
     * 挂单数量
     */
    private BigDecimal quantity;

    public Exchange() {
    }

    public Exchange(String userId, TransTypeEum transType, BigDecimal price, BigDecimal quantity) {
        this.userId = userId;
        this.price = price;
        this.quantity = quantity;
        this.type = transType.getCode();
        this.entrustOrder = UUID.randomUUID().toString();
    }

    public Exchange(String userId, TransTypeEum transType, BigDecimal price, BigDecimal quantity, String entrustOrder) {
        this.userId = userId;
        this.price = price;
        this.quantity = quantity;
        this.type = transType.getCode();
        this.entrustOrder = entrustOrder;
    }
}
