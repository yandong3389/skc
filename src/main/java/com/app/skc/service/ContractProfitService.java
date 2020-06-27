package com.app.skc.service;

import com.app.skc.model.UserShareVO;

public interface ContractProfitService {

    /**
     * 分享合约用户树收益处理
     *
     * @param userShare
     */
    public void userTreeTrans(UserShareVO userShare);

}
