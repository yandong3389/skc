package com.app.skc.controller;


import com.app.skc.enums.ApiErrEnum;
import com.app.skc.enums.KlineEum;
import com.app.skc.enums.TransTypeEum;
import com.app.skc.service.KlineService;
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

import java.util.Date;
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
@RequestMapping("/exchange")
public class ExchangeController {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeController.class);

    private final TransactionService transactionService;

    @Autowired
    private KlineService klineService;

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
     * 获取最新成交价格
     *
     * @return date-日期 ; line-每分钟成交价 ,
     * 数组长度2440, 每分钟对应一个下标
     * 00:00对应line[0]
     * 23:59对应line[2339]
     *
     */
    /**
     * 获取最新成交价格
     * @param interval  string, 必填，间隔时间，[15m, 1d]，m -> 分钟；d -> 天；
     * @param startTime  long, 非必填，开始时间；
     * @param endTime long, 非必填，结束时间；
     * @param limit int, 非必填，默认 500; 最大 1000.
     * @return K线数据 , 示例:
     * [
     *     {
     *         "startTime": 1593101422745, //开盘时间
     *         "endTime": 1593102322745, //收盘时间
     *         "startPrice": "0.0000", //开盘价
     *         "endPrice": "0.0000", //收盘价
     *         "maxPrice": "0.0000", //最高价
     *         "minPrice": "0.0000", //最低价
     *         "quantity": "0.0000", //成交量
     *         "totalAmount": "0.0000", //成交额
     *         "transNum": 0 //成交笔数
     *     }
     * ]
     */
    @ApiOperation(value = "获取K线数据", notes = "获取K线数据")
    @GetMapping("/kline")
    @ResponseBody
    public ResponseResult kline(String interval,Long startTime ,Long endTime , Integer limit) {
        KlineEum klineEum = KlineEum.getByCode(interval);
        if (klineEum == null){
            return ResponseResult.fail(ApiErrEnum.REQ_PARAM_NOT_NULL.getCode(),"interval 参数不合法");
        }
        Date start = null;
        Date end = null;
        if (startTime != null)
            start = new Date(startTime);
        if (endTime != null)
            end = new Date(endTime);
        if (limit == null || limit <= 0)
            limit = 500;
        if (limit > 1000)
            limit = 1000;
        return klineService.kline(klineEum,start,end,limit);
    }

    /**
     * 获取交易对列表
     *
     * @param map trans_type-交易类型(必选)；from_user_id-用户id(可选)；to_user_id-用户id(可选)；trans_status-交易状态(可选),pageSize  , pageNum
     * @return
     */
    @ApiOperation(value = "获取交易对列表", notes = "获取交易对列表")
    @GetMapping("/coin/list")
    @ResponseBody
    public ResponseResult coinList(@RequestParam Map<String, Object> map) {
        if (map == null) {
            map = new HashMap<>();
        }
        map.putIfAbsent("trans_type", String.join(",", TransTypeEum.BUY.getCode(), TransTypeEum.SELL.getCode()));
        return transactionService.transQueryByPage(map, new Page());
    }

    /**
     * 获取交易对买入队列
     * @return
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
     * @param userId 用户 ID
     * @param price 价格
     * @param quantity 数量
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
     * 主动卖入
     * @param userId 用户 ID
     * @param price 价格
     * @param quantity 数量
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

    /**
     * 获取当前委托信息
     *
     * @param userId 用户id
     * @return ResponseResult
     */
    @ApiOperation(value = "获取当前委托信息", notes = "获取当前委托信息")
    @GetMapping("/order/getEntrust")
    @ResponseBody
    public ResponseResult getEntrust(@RequestParam String userId) {
        try {
            return transactionService.getEntrust(userId);
        } catch (Exception e) {
            logger.error("获取当前委托信息", e);
            return ResponseResult.fail("ERR500", e.getMessage());
        }
    }

    /**
     * 取消委托
     *
     * @param userId       用户 id
     * @param entrustOrder 委托订单号
     * @return ResponseResult
     */
    @ApiOperation(value = "取消委托", notes = "取消委托")
    @PostMapping("/order/cancelEntrust")
    @ResponseBody
    public ResponseResult cancelEntrust(@RequestParam String userId, @RequestParam String entrustOrder) {
        try {
            return transactionService.cancelEntrust(userId, entrustOrder);
        } catch (Exception e) {
            logger.error("取消委托", e);
            return ResponseResult.fail("ERR500", e.getMessage());
        }
    }


}

