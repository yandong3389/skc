package com.app.skc.service.Impl;

import com.alibaba.fastjson.JSON;
import com.app.skc.enums.ApiErrEnum;
import com.app.skc.enums.WalletEum;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.Wallet;
import com.app.skc.service.WalletService;
import com.app.skc.utils.BaseUtils;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;

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
    private static final String DATA_PREFIX = "0x70a08231000000000000000000000000";
    @Autowired
    private final WalletMapper walletMapper;
    private static final String LOG_PREFIX = "[钱包服务] - ";
    private static String walletStoreDir = "/Users/Dylan/Desktop/wallet";
    @Autowired
    public WalletServiceImpl(WalletMapper walletMapper){
        this.walletMapper = walletMapper;
    }
    @Autowired
    private Web3j web3j;

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
    public ResponseResult createWallet(String userId) throws IOException, CipherException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        log.info("{}开始为用户:{}创建钱包",LOG_PREFIX,userId);
        File file = new File(walletStoreDir);
        System.out.println(file.exists());
        if(!file.exists()){
            file.mkdirs();
        }
        String ethwalletName = WalletUtils.generateNewWalletFile("", new File(walletStoreDir), true);
        if (ethwalletName!=null){
            log.info("{}用户:{}钱包生成成功",LOG_PREFIX,userId);
            String walletFilePath =walletStoreDir+"/"+ethwalletName;
            Credentials credentials = WalletUtils.loadCredentials("",walletFilePath);
            String address = credentials.getAddress();
            Date date = new Date();
            BigInteger publicKey = credentials.getEcKeyPair().getPublicKey();
            BigInteger privateKey = credentials.getEcKeyPair().getPrivateKey();
            Wallet wallet = saveWallet(userId, walletFilePath, address, date, publicKey, privateKey);
            Map <String, String> map = getReturnWallet(wallet);
            log.info("{}用户:{}钱包创建完成",LOG_PREFIX,userId);
            return ResponseResult.success("创建成功",map);
        }else {
            log.info("{}用户:{}钱包创建失败",LOG_PREFIX,userId);
            return ResponseResult.fail(ApiErrEnum.CREATE_WALLET_FAIL);
        }

    }


    @Override
    public BigDecimal getERC20Balance(String fromAddress ,String contractAddress) {
        String methodName = "balanceOf";
        List <Type> inputParameters = new ArrayList <>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        Address address = new Address(fromAddress);
        inputParameters.add(address);

        TypeReference<Uint256> typeReference = new TypeReference <Uint256>() {
        };
        outputParameters.add(typeReference);
        Function function = new Function(methodName, inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);
        Transaction transaction = Transaction.createEthCallTransaction(fromAddress, contractAddress, data);

        EthCall ethCall;
        BigDecimal balanceValue = BigDecimal.ZERO;
        try {
            ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            balanceValue = (BigDecimal) results.get(0).getValue();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return balanceValue;

    }

    /**
     * 获取以太坊余额
     * @param address
     * @return
     * @throws IOException
     */
    @Override
    public BigDecimal getEthBalance(String address) throws IOException {
        EthGetBalance ethGetBlance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        String balance = Convert.fromWei(new BigDecimal(ethGetBlance.getBalance()), Convert.Unit.ETHER).toPlainString();
        return new BigDecimal(balance);
    }

    /**
     * 保存用户钱包
     * @param userId
     * @param walletFilePath
     * @param address
     * @param date
     * @param publicKey
     * @param privateKey
     * @return
     */
    private Wallet saveWallet(String userId, String walletFilePath, String address, Date date, BigInteger publicKey, BigInteger privateKey) {
        Wallet wallet = new Wallet();
        BigDecimal init = BigDecimal.ZERO;
        wallet.setAddress(address);
        wallet.setWalletPath(walletFilePath);
        wallet.setBalAvail(init);
        wallet.setBalFreeze(init);
        wallet.setBalReward(init);
        wallet.setBalTotal(init);
        wallet.setMnemonic(NULL);
        wallet.setPublicKey(publicKey.toString());
        wallet.setPrivateKey(privateKey.toString());
        wallet.setCreateTime(date);
        wallet.setModifyTime(date);
        wallet.setUserId(userId);
        wallet.setWalletType(WalletEum.ETH.getCode());
        log.info("{}开始保存用户:{}的ETH钱包[{}]",LOG_PREFIX,userId, JSON.toJSONString(wallet));
        wallet.setWalletId(BaseUtils.get64UUID());
        walletMapper.insert(wallet);
        log.info("{}开始保存用户:{}的USDT钱包[{}]",LOG_PREFIX,userId, JSON.toJSONString(wallet));
        wallet.setWalletId(BaseUtils.get64UUID());
        wallet.setWalletType(WalletEum.USDT.getCode());
        walletMapper.insert(wallet);
        log.info("{}开始保存用户:{}的SK钱包[{}]",LOG_PREFIX,userId, JSON.toJSONString(wallet));
        wallet.setWalletId(BaseUtils.get64UUID());
        wallet.setWalletType(WalletEum.SK.getCode());
        walletMapper.insert(wallet);
        log.info("{}用户:{}钱包保存成功",LOG_PREFIX,userId);
        return wallet;
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
