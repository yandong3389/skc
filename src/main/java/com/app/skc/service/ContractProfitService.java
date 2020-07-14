package com.app.skc.service;

import com.app.skc.exception.BusinessException;
import com.app.skc.model.UserShareVO;

import java.math.BigDecimal;

/**
 * @author Jerry
 */
public interface ContractProfitService {

    /**
     * 分享合约用户树收益处理
     *
     * @param userShare
     * @param rate
     * @param dateAcct
     * @throws BusinessException
     */
    void userTreeTrans(UserShareVO userShare, BigDecimal rate, String dateAcct) throws BusinessException;

}
