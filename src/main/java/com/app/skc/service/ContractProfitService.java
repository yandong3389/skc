package com.app.skc.service;

import com.app.skc.model.UserShareVO;

import java.util.List;
import java.util.Map;

public interface ContractProfitService {

    /**
     * 分享合约用户树收益处理
     *
     * @param userShare
     */
    void userTreeTrans(UserShareVO userShare);

    /**
     * 计算用户等级，key为用户的目标等级{@link com.app.skc.enums.UserGradeEnum}代码，
     * value为需要更新用户等级的userId列表
     *
     * @param userId
     * @return
     */
    Map<String, List<String>> calcUserGrade(String userId);

}
