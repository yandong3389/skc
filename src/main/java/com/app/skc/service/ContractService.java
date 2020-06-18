package com.app.skc.service;

import com.app.skc.model.Contract;
import com.baomidou.mybatisplus.service.IService;

import java.util.List;

public interface ContractService extends IService<Contract> {
    /**
     * 查询合约列表
     *
     * @return list
     */
    List<Contract> list();
}
