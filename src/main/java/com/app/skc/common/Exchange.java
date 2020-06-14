package com.app.skc.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

/**
 *
 */
@Data
public class Exchange {
    /**
     * 挂单用户
     */
    @JsonIgnore
    private String userId;
    /**
     * 挂单价格
     */
    private BigDecimal price;
    /**
     * 挂单数量
     */
    private Integer quantity;

    public Exchange() {
    }

    public Exchange(String userId, BigDecimal price, Integer quantity) {
        this.userId = userId;
        this.price = price;
        this.quantity = quantity;
    }
}
