package com.app.skc.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.app.skc.enums.TransStatusEnum;
import com.app.skc.enums.TransTypeEum;
import com.app.skc.exception.BusinessException;
import com.app.skc.mapper.TransactionMapper;
import com.app.skc.model.Contract;
import com.app.skc.model.Income;
import com.app.skc.model.Transaction;
import com.app.skc.service.ContractService;
import com.app.skc.utils.SkcConstants;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合约制度
 */
@RequestMapping("/contract")
@RestController
public class ContractController {
    @Autowired
    private ContractService contractService;
    @Autowired
    private TransactionMapper transactionMapper;
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
        try {
            contractService.buy(userId, code);
        } catch (BusinessException be) {
            logger.error("{}购买失败:[{}]", LOG_PREFIX, be);
            return ResponseResult.fail("ERR500", be.getMessage());
        }
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
        Map map = getQueryMap(userId, 5);
        List list = transactionMapper.selectByMap(map);
        logger.info("{}查询成功,查询到[{}]条记录", LOG_PREFIX, list.size());
        return ResponseResult.success().setData(new PageInfo<>(list));
    }

    private Map getQueryMap(@RequestParam String userId, int i) {
        Map map = new HashMap(i);
        map.put(SkcConstants.FROM_USER_ID, userId);
        map.put(SkcConstants.TRANS_TYPE, TransTypeEum.CONTRACT.getCode());
        return map;
    }

    @GetMapping("userContract")
    public ResponseResult getUserContract(String userId) {
        logger.info("{}开始查询用户合约,userId=[{}]", LOG_PREFIX, userId);
        Map map = getQueryMap(userId, 3);
        map.put(SkcConstants.TRANS_STATUS, TransStatusEnum.EFFECT.getCode());
        List <Transaction> list = transactionMapper.selectByMap(map);
        logger.info("{}查询成功,查询到[{}]条记录", LOG_PREFIX, list.size());
        BigDecimal contractPrice = BigDecimal.ZERO;
        if (list.size() > 0) {
            contractPrice = list.get(0).getPrice();
        }
        return ResponseResult.success().setData(contractPrice);

    }

    /**
     * 查询团队业绩
     *
     * @param userIds 自己和伞下所有用户Id的jsonArray
     * @return
     */
    @PostMapping("/performance")
    public ResponseResult teamPerformance(@RequestBody JSONObject userIds) {
        String ownerUserId = userIds.getString("ownerUserId");
        logger.info("{}开始查询用户:[{}]团队[{}]的业绩", LOG_PREFIX, ownerUserId, JSON.toJSONString(userIds));
        List list = userIds.getJSONArray("userIds");
        try {
            return ResponseResult.success("查询成功", contractService.teamPerformance(list));
        } catch (BusinessException be) {
            logger.error(be.getMessage());
            return ResponseResult.fail();
        }
    }

    @GetMapping("/income")
    public ResponseResult income(String userId, Page page) throws BusinessException {
        logger.info("{}开始查询用户收益参数 userId = [{}],Page =[{}]", LOG_PREFIX, userId, JSON.toJSONString(page));
        if (StringUtils.isBlank(userId)) {
            throw new BusinessException("用户 Id不能为空");
        }
        //查询收益
        List <Income> list = contractService.getIncome(userId, page);
        return ResponseResult.success("查询成功", new PageInfo <Income>(list));
    }






}
