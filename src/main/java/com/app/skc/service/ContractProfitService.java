package com.app.skc.service;

import com.app.skc.exception.BusinessException;
import com.app.skc.model.UserShareVO;

public interface ContractProfitService {

    /**
     * 分享合约用户树收益处理
     *
     * @param userShare
     */
    void userTreeTrans(UserShareVO userShare) throws BusinessException;

}
