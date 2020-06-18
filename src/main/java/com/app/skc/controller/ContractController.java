package com.app.skc.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
     * @param userId 用户 Id
     * @param code 合约代码
     * @return
     */
    @PostMapping("/buy")
    public ResponseResult buy(@RequestParam String userId,@RequestParam String code){
        logger.info("{}开始购买合约,请求参数[userId={},code={}]",LOG_PREFIX,userId,code);
        logger.info("{}购买完成",LOG_PREFIX);
        return  ResponseResult.success();
    }

    /**
     * 获取合约信息
     *
     * @return list
     */
    @GetMapping("/list")
    public ResponseResult getContracts() {
        logger.info("{}开始查询合约列表", LOG_PREFIX);
        List<Contract> list = contractService.list();
        logger.info("{}查询成功,查询到[{}]条记录", LOG_PREFIX, list.size());
        return ResponseResult.success("查询成功", list);
    }

    /**
     * 获取合约购买记录
     *
     * @param userId 用户 ID
     * @param page   分页信息
     * @return ResponseResult
     */
    @GetMapping("history")
    public ResponseResult getHistory(Page page, @RequestParam String userId) {
        logger.info("{}开始查询合约购买历史,page=[{}],userId=[{}]", LOG_PREFIX, JSON.toJSONString(page), userId);
        Transaction transaction = new Transaction();
        transaction.setFromUserId(userId);
        transaction.setPrice(new BigDecimal(1000));
        transaction.setTransType(TransTypeEum.CONTRACT.getCode());
        transaction.setCreateTime(new Date());
        transaction.setModifyTime(new Date());
        List list = new ArrayList();
        list.add(transaction);
        list.add(transaction);
        logger.info("{}查询成功,查询到[{}]条记录", LOG_PREFIX, list.size());
        return ResponseResult.success().setData(new PageInfo<>(list));
    }

}
