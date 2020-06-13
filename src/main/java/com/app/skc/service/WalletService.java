package com.app.skc.service;

import com.app.skc.exception.BusinessException;
import com.app.skc.model.Wallet;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.service.IService;
import org.web3j.crypto.CipherException;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.ExecutionException;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 
 * @since 2020-02-05
 */
public interface WalletService extends IService<Wallet> {

    /**
     * 创建钱包
     * @param userId 用户信息
     * @return ResponseResult
     */
    ResponseResult createWallet(String userId) throws IOException, CipherException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException;

    /**
     * 获取eth 代币余额
     * @param address 账号地址
     * @param contractAddress 合约地址
     * @return
     */
    BigDecimal getERC20Balance(String address,String contractAddress);

    /**
     * 获取 eth 余额
     * @param address
     * @return
     * @throws IOException
     */
    BigDecimal getEthBalance(String address) throws IOException;

    /**
     * 提现（上链）
     * @param fromAddress 转账发起地址
     * @param toAddress 到账地址
     * @param amount 金额
     * @param fromPath 转账发起物理地址
     * @return
     */
    String withdraw(String fromAddress,String toAddress,BigDecimal amount,String fromPath) throws IOException, CipherException, BusinessException, ExecutionException, InterruptedException;



}
