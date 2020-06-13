package com.app.skc.service;

import com.app.skc.model.Wallet;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.service.IService;
import org.web3j.crypto.CipherException;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Map;

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


}
