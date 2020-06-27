package com.app.skc.service.Impl;

import com.app.skc.mapper.IncomeMapper;
import com.app.skc.model.Income;
import com.app.skc.model.UserShareVO;
import com.app.skc.service.ContractProfitService;
import com.app.skc.service.system.ConfigService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("contractProfitService")
public class ContractProfitServiceImpl extends ServiceImpl<IncomeMapper, Income> implements ContractProfitService {
    private static final Logger log = LoggerFactory.getLogger(ContractProfitServiceImpl.class);
    private static final String LOG_PREFIX = "[合约收益释放] - ";
    @Autowired
    private final IncomeMapper incomeMapper;

    @Autowired
    public ContractProfitServiceImpl(IncomeMapper incomeMapper) {
        this.incomeMapper = incomeMapper;
    }

    @Autowired
    private ConfigService configService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void userTreeTrans(UserShareVO userShare) {
        // 1、初始数据准备
        Map<String, Income> incomeMap = new HashMap<>();
        Map<String, UserShareVO> allShareMap = new HashMap<>();
        Map<Integer, List<UserShareVO>> allLevelMap = new HashMap<>();
        fulfillAllMap(allShareMap, userShare);
        fulfillLevelMap(allLevelMap, userShare, 1);
        for (int i = allLevelMap.size(); i > 0; i--) {
            List<UserShareVO> levelShareList = allLevelMap.get(i);
            for (UserShareVO user : levelShareList) {

            }
        }


    }

    /**
     * 递归填充所有分享用户map
     *
     * @param allShareMap
     * @param userShare
     */
    private void fulfillAllMap(Map<String, UserShareVO> allShareMap, UserShareVO userShare) {
        allShareMap.put(userShare.getId(), userShare);
        if (!CollectionUtils.isEmpty(userShare.getSubUsers())) {
            for (UserShareVO element : userShare.getSubUsers()) {
                fulfillAllMap(allShareMap, element);
            }
        }
    }

    /**
     * 递归填充所有分享用户等级map
     *
     * @param allShareLevelMap
     * @param userShare
     * @param pathLevel
     */
    private void fulfillLevelMap(Map<Integer, List<UserShareVO>> allShareLevelMap, UserShareVO userShare, Integer pathLevel) {
        List<UserShareVO> levelShareList = allShareLevelMap.get(pathLevel);
        if (levelShareList == null) {
            levelShareList = new ArrayList<>();
        }
        userShare.setLevel(pathLevel.toString());
        levelShareList.add(userShare);
        allShareLevelMap.put(pathLevel, levelShareList);
        if (!CollectionUtils.isEmpty(userShare.getSubUsers())) {
            for (UserShareVO element : userShare.getSubUsers()) {
                fulfillLevelMap(allShareLevelMap, element, ++pathLevel);
            }
        }
    }

}
