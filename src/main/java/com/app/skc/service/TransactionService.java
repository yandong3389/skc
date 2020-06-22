package com.app.skc.service;

import com.app.skc.exception.BusinessException;
import com.app.skc.model.Transaction;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.service.IService;
import org.web3j.crypto.CipherException;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 
 * @since 2020-02-05
 */
public interface TransactionService extends IService<Transaction> {

    /**
     * ETH钱包转账
     *
     * @return ResponseResult
     */
    ResponseResult transfer(String toWalletAddress, String transferNumber, String userId, String walletType) throws InterruptedException, ExecutionException, BusinessException, CipherException, IOException;

    /**
     * 根据交易类型分页查询交易记录
     *
     * @return ResponseResult
     */
    ResponseResult transQueryByPage(Map <String, Object> params, Page page);

    /**
     * 提现
     *
     * @param userId       用户id
     * @param toAddress    提现地址
     * @param cashOutMoney 提现金额
     * @return ResponseResult
     */
    ResponseResult cashOut(String userId, String walletType, String toAddress, String cashOutMoney) throws InterruptedException, IOException, ExecutionException, CipherException, BusinessException;

    /**
     * 买入
     *
     * @param userId   用户id
     * @param price    单价
     * @param quantity 数量
     */
    ResponseResult buy(String userId, String price, Integer quantity);

    /**
     * 卖出
     *
     * @param userId   用户id
     * @param price    单价
     * @param quantity 数量
     */
    ResponseResult sell(String userId, String price, Integer quantity);

    /**
     * 查询买入列表
     */
    ResponseResult queryBuy();

    /**
     * 查询卖出列表
     */
    ResponseResult querySell();

    /**
     * 获取当前委托信息
     */
    ResponseResult getEntrust(String userId);

    /**
     * 最新成交价
     */
    ResponseResult price();

    /**
     * 获取K线数据
     */
    ResponseResult kline();

    /**
     * 取消委托
     */
    ResponseResult cancelEntrust(String userId, String entrustOrder);
}
