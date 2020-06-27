package com.app.skc.service;

import java.util.TreeMap;

public interface ContractProfitService {

    /**
     * 分享合约用户树收益处理
     *
     * @param userTreeMap
     */
    public void userTreeTrans(TreeMap<String, String> userTreeMap);

}
