package com.app.skc.service.Impl;

import com.app.skc.mapper.ContractMapper;
import com.app.skc.model.Contract;
import com.app.skc.service.ContractService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Dylan
 */
@Service
public class ContractServiceImpl extends ServiceImpl <ContractMapper, Contract> implements ContractService {
    /**
     * 查询合约列表
     * @return
     */
    @Override
    public List <Contract> list() {
        Contract contract = new Contract();
        return contract.selectAll();
    }
}
