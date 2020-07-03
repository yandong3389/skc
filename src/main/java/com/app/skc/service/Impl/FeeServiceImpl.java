package com.app.skc.service.Impl;

import com.app.skc.enums.SysConfigEum;
import com.app.skc.model.system.Config;
import com.app.skc.service.FeeService;
import com.app.skc.service.system.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author rwang
 * @since 2020-06-08
 */
@Service("feeService")
public class FeeServiceImpl implements FeeService {
    private static final Logger log = LoggerFactory.getLogger(FeeServiceImpl.class);

    @Autowired
    private ConfigService configService;

    @Override
    public BigDecimal buyerAmount(BigDecimal buyAmount) {
        BigDecimal fee = getFee();
        return buyAmount.add(buyAmount.multiply(fee));
    }

    @Override
    public BigDecimal sellerAmount(BigDecimal sellAmount) {
        BigDecimal fee = getFee();
        return sellAmount.subtract(sellAmount.multiply(fee));
    }

    @Override
    public BigDecimal fee(BigDecimal amount) {
        BigDecimal fee = getFee();
        return amount.multiply(fee);
    }

    private BigDecimal getFee() {
        BigDecimal fee = BigDecimal.ZERO;
        Config byKey = configService.getByKey(SysConfigEum.EXCHANGE_FEE.getCode());
        if (byKey!=null){
            try {
                fee = BigDecimal.valueOf(Double.parseDouble(byKey.getConfigValue()));
            } catch (NumberFormatException e) {
                log.error("交易手续费转换失败",e);
            }
        }
        return fee;
    }
}
