package com.app.skc.controller;


import com.app.skc.enums.TransTypeEum;
import com.app.skc.service.TransactionService;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author
 * @since 2020-06-12
 */
@Api(value = "ExchangeController", description = "交易接口-获取最新成交价格，买入，卖出，订单")
@Controller
@RequestMapping("/skc/api/exchange")
public class ExchangeController {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeController.class);

    private final TransactionService transactionService;

    @Autowired
    public ExchangeController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * 获取最新成交价格
     */
    @ApiOperation(value = "获取最新成交价格", notes = "获取最新成交价格")
    @GetMapping("/price")
    @ResponseBody
    public ResponseResult price() {
        return transactionService.price();
    }

    /**
     * 获取交易对列表
     */
    @ApiOperation(value = "获取交易对列表", notes = "获取交易对列表")
    @GetMapping("/coin/list")
    @ResponseBody
    public ResponseResult coinList(@RequestParam Map<String, Object> map, Page page) {
        if (page == null)
            page = new Page();
        if (map == null) {
            map = new HashMap<>();
        }
        map.putIfAbsent("trans_type", String.join(",", TransTypeEum.BUY.getCode(), TransTypeEum.SELL.getCode()));
        return transactionService.transQueryByPage(page, map);
    }

    /**
     * 获取交易对买入队列
     */
    @ApiOperation(value = "获取交易对买入队列", notes = "获取交易对买入队列")
    @GetMapping("/queue/buy")
    @ResponseBody
    public ResponseResult queueBuy() {
        return transactionService.queryBuy();
    }

    /**
     * 获取交易对卖出队列
     */
    @ApiOperation(value = "获取交易对卖出队列", notes = "获取交易对卖出队列")
    @GetMapping("/queue/sell")
    @ResponseBody
    public ResponseResult queueSell() {
        return transactionService.querySell();
    }

    /**
     * 主动买入
     */
    @ApiOperation(value = "主动买入", notes = "主动买入")
    @PostMapping("/order/buy")
    @ResponseBody
    public ResponseResult orderBuy(
            @RequestParam String userId,
            @RequestParam String price,
            @RequestParam Integer quantity) {
        try {
            return transactionService.buy(userId, price, quantity);
        } catch (Exception e) {
            logger.error("买入异常", e);
            return ResponseResult.fail("ERR500", e.getMessage());
        }
    }

    /**
     * 主动卖出
     */
    @ApiOperation(value = "主动卖出", notes = "主动卖出")
    @PostMapping("/order/sell")
    @ResponseBody
    public ResponseResult orderSell(
            @RequestParam String userId,
            @RequestParam String price,
            @RequestParam Integer quantity) {
        try {
            return transactionService.sell(userId, price, quantity);
        } catch (Exception e) {
            logger.error("卖出异常", e);
            return ResponseResult.fail("ERR500", e.getMessage());
        }
    }
}

