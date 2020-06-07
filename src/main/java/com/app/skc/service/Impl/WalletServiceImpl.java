package com.app.skc.service.Impl;
import com.app.skc.model.Wallet;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.service.WalletService;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.plugins.pagination.PageHelper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 
 * @since 2020-02-05
 */
@Service("walletService")
public class WalletServiceImpl extends ServiceImpl<WalletMapper, Wallet> implements WalletService {
    private static final Logger log = LoggerFactory.getLogger(WalletServiceImpl.class);
    @Autowired
    private final WalletMapper walletMapper;
    @Value("${web3j.client-address}")
    private String web3jAddress;

    private static String walletStoreDir = "/data/mdc/userWallet";
    @Autowired
    public WalletServiceImpl(WalletMapper walletMapper){
        this.walletMapper = walletMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult createWallet(int userId,String password) throws Throwable {
        System.out.println("开始创建ETH钱包，默认地址为"+walletStoreDir);
        File file = new File(walletStoreDir);
        System.out.println(file.exists());
        if(!file.exists()){
            file.mkdirs();
        }
        String name = WalletUtils.generateNewWalletFile(password, new File(walletStoreDir), true);
        System.out.println("创建成功，钱包名为："+name);
        String walletFilePath =walletStoreDir+"/"+name;
        Credentials credentials = WalletUtils.loadCredentials(password, walletFilePath);
        String address = credentials.getAddress();
        BigInteger publicKey = credentials.getEcKeyPair().getPublicKey();
        BigInteger privateKey = credentials.getEcKeyPair().getPrivateKey();
        Wallet wallet = new Wallet();
        wallet.setAddress(address);
        wallet.setWalletPath(walletFilePath);
        wallet.setUstdBlance(new BigDecimal(0));
        wallet.setMdcBlance(new BigDecimal(0));
        wallet.setPassword(password);
        wallet.setPublicKey(publicKey.toString());
        wallet.setPrivateKey(privateKey.toString());
        wallet.setCreateTime(new Date());
        wallet.setUserId(userId);
        walletMapper.insert(wallet);
        return ResponseResult.success();
    }

    @Override
    public ResponseResult getBalance(Page page, Map<String,Object> params) {
        params.remove("pageNum");
        params.remove("pageSize");
        PageHelper.startPage(page.getPageNum(),page.getPageSize());
        List<Wallet> walletList = walletMapper.selectByMap(params);
        return ResponseResult.success().setData(new PageInfo<>(walletList));
    }
}
