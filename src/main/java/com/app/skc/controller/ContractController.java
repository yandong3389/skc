package com.app.skc.controller;

import com.app.skc.enums.TransTypeEum;
import com.app.skc.model.Contract;
import com.app.skc.model.Transaction;
import com.app.skc.service.ContractService;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 合约制度
 */
@RequestMapping("/contract")
@RestController
public class ContractController {
    @Autowired
    private ContractService contractService;
    private static final Logger logger = LoggerFactory.getLogger(ContractController.class);
    private static final String LOG_PREFIX = "[合约服务] - ";


    /**
     * 购买合约
     * @param id 合约 id
     * @param userId 用户id
     * @return
     */
    @PostMapping("/buy")
    public ResponseResult buy(String id,String userId){
        return  ResponseResult.success();
    }

    /**
     * 获取合约信息
     * @return list
     */
    @GetMapping("/list")
    public ResponseResult getContracts(){
        logger.info("{}开始查询合约列表",LOG_PREFIX);
        List<Contract> list = contractService.list();
        logger.info("{}查询成功,查询到[{}]条记录",LOG_PREFIX,list.size());
        return ResponseResult.success("查询成功",list);
    }

    /**
     * 获取合约购买记录
     * @param userId 用户 ID
     * @return
     */
    @GetMapping("history")
    public ResponseResult getHistory(Page page,@RequestParam String userId){
        Transaction transaction = new Transaction();
        transaction.setFromUserId(userId);
        transaction.setPrice(new BigDecimal(1000));
        transaction.setTransType(TransTypeEum.CONTRACT.getCode());
        transaction.setCreateTime(new Date());
        transaction.setModifyTime(new Date());
        List list = new ArrayList();
        list.add(transaction);
        list.add(transaction);
        return ResponseResult.success().setData(new PageInfo<>(list));
    }

}
