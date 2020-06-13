package com.app.skc.service.Impl;

import com.alibaba.fastjson.JSON;
import com.app.skc.enums.ApiErrEnum;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.Wallet;
import com.app.skc.service.WalletService;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author rwang
 * @since 2020-06-08
 */
@Service("walletService")
public class WalletServiceImpl extends ServiceImpl<WalletMapper, Wallet> implements WalletService {
    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);
    private static final String NULL = null;
    private static final String ADDRESS = "address";
    private static final String MNEMONIC = "mnemonic";
    @Autowired
    private final WalletMapper walletMapper;
    private static final String LOG_PREFIX = "[钱包服务] - ";
    @Value("${web3j.client-address}")
    private String web3jAddress;

    private static String walletStoreDir = "/data/skc/userWallet";
    @Autowired
    public WalletServiceImpl(WalletMapper walletMapper){
        this.walletMapper = walletMapper;
    }

    /**
     * 创建钱包
     *
     * @param userId 用户信息
     * @return
     * @throws IOException
     * @throws CipherException
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult createWallet(String userId) throws IOException, CipherException {
        log.info("{}开始为用户:{}创建钱包",LOG_PREFIX,userId);
        File file = new File(walletStoreDir);
        System.out.println(file.exists());
        if(!file.exists()){
            file.mkdirs();
        }
        Bip39Wallet ethwallet = WalletUtils.generateBip39Wallet(NULL, new File(walletStoreDir));
        if (ethwallet!=null){
            log.info("{}用户:{}钱包创建成功",LOG_PREFIX,userId);
            String walletFilePath =walletStoreDir+"/"+ethwallet.getFilename();
            Credentials credentials = WalletUtils.loadCredentials(ethwallet.getMnemonic(), walletFilePath);
            String address = credentials.getAddress();
            BigInteger publicKey = credentials.getEcKeyPair().getPublicKey();
            BigInteger privateKey = credentials.getEcKeyPair().getPrivateKey();
            Wallet wallet = new Wallet();
            wallet.setAddress(address);
            wallet.setWalletPath(walletFilePath);
            wallet.setBalance(new BigDecimal(0));
            wallet.setMnemonic(ethwallet.getMnemonic());
            wallet.setPublicKey(publicKey.toString());
            wallet.setPrivateKey(privateKey.toString());
            wallet.setCreateTime(new Date());
            wallet.setUserId(userId);
            log.info("{}开始保存用户:{}的钱包[{}]",LOG_PREFIX,userId, JSON.toJSONString(wallet));
            walletMapper.insert(wallet);
            log.info("{}用户:{}钱包保存成功",LOG_PREFIX,userId);
            Map <String, String> map = getReturnWallet(wallet);
            return ResponseResult.success("创建成功",map);
        }else {
            log.info("{}用户:{}钱包创建失败",LOG_PREFIX,userId);
            return ResponseResult.fail(ApiErrEnum.CREATE_WALLET_FAIL);
        }

    }

    /**
     * 获取钱包创建成功的返回值
     * @param wallet
     * @return
     */
    private Map <String, String> getReturnWallet(Wallet wallet) {
        Map <String, String> map = new HashMap <>(2);
        map.put(ADDRESS,wallet.getAddress());
        map.put(MNEMONIC,wallet.getMnemonic());
        return map;
    }

    /*@Override
    public ResponseResult getBalance(Page page, Map<String,Object> params) {
        params.remove("pageNum");
        params.remove("pageSize");
        PageHelper.startPage(page.getPageNum(),page.getPageSize());
        List<Wallet> walletList = walletMapper.selectByMap(params);
        return ResponseResult.success().setData(new PageInfo<>(walletList));
    }*/
}
