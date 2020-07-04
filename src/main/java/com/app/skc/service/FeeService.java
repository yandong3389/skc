package com.app.skc.service;

import java.math.BigDecimal;

/**
 * 手续费相关接口
 */
public interface FeeService {

    /**
     * 买入扣除手续费后总价
     * @param buyAmount 买入消耗总额
     * @return 扣除手续费后总价
     */
    BigDecimal buyerAmount(BigDecimal buyAmount);

    /**
     * 卖出扣除手续费后总价
     * @param sellAmount 卖出总额
     * @return 扣除手续费后总价
     */
    BigDecimal sellerAmount(BigDecimal sellAmount);
    /**
     * 交易手续费
     * @param amount 交易金额
     * @return 手续费
     */
    BigDecimal fee(BigDecimal amount);
}
