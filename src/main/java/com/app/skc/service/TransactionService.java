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
     * ETH钱包余额查询
     * @return ResponseResult
     */
    ResponseResult getETHBlance(Page page, Map<String,Object> params);

    /**
     * 充值
     * @param userId 用户id
     * @param toAddress 钱包address
     * @param investMoney 充值金额
     * @return ResponseResult
     */
    ResponseResult investUSDT(String userId,String toAddress,String investMoney);

    /**
     * 提现
     * @param userId 用户id
     * @param payPassword 支付密码
     * @param toAddress 提现地址
     * @param cashOutMoney 提现金额
     * @return ResponseResult
     */
    ResponseResult cashOutUSDT(String userId,String payPassword,String toAddress,String cashOutMoney,String verCode, String verId) throws InterruptedException;

}
