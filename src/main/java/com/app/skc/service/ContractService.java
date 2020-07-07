package com.app.skc.service;

import com.app.skc.exception.BusinessException;
import com.app.skc.model.Contract;
import com.app.skc.model.Income;
import com.app.skc.model.Transaction;
import com.app.skc.utils.viewbean.Page;
import com.baomidou.mybatisplus.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface ContractService extends IService<Contract> {

    /**
     * 购买合约
     *
     * @param userId 用户 id
     * @param code   合约代码
     * @return
     */
    Boolean buy(String userId, String code) throws BusinessException;

    /**
     * 查询合约列表
     *
     * @return list
     */
    List<Contract> list();

    /**
     * 团队业绩
     *
     * @param userIds
     * @return
     */
    int teamPerformance(List <String> userIds) throws BusinessException;

    /**
     * 查询收益列表
     */
    List <Income> getIncome(String userId, Page page);

    BigDecimal queryContarct(String userId);


}
