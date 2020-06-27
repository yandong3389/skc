package com.app.skc.service.Impl;

import com.app.skc.mapper.IncomeMapper;
import com.app.skc.model.Income;
import com.app.skc.service.ContractProfitService;
import com.app.skc.service.system.ConfigService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.TreeMap;

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
    public void userTreeTrans(TreeMap<String, String> userTreeMap) {

    }

}
