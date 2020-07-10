package com.app.skc.service;

import com.app.skc.exception.BusinessException;
import com.app.skc.model.UserShareVO;

import java.math.BigDecimal;

public interface ContractProfitService {

    /**
     * 分享合约用户树收益处理
     *
     * @param userShare
     */
    void userTreeTrans(UserShareVO userShare, BigDecimal rate) throws BusinessException;

}
