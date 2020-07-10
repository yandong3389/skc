package com.app.skc.service.Impl;

import com.alibaba.fastjson.JSON;
import com.app.skc.enums.ApiErrEnum;
import com.app.skc.enums.InfuraInfo;
import com.app.skc.enums.WalletEum;
import com.app.skc.exception.BusinessException;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.Wallet;
import com.app.skc.model.system.Config;
import com.app.skc.service.ContractService;
import com.app.skc.service.WalletService;
import com.app.skc.service.system.ConfigService;
import com.app.skc.utils.BaseUtils;
import com.app.skc.utils.SkcConstants;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;
import java.util.concurrent.ExecutionException;

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
    @Autowired
    public WalletServiceImpl(WalletMapper walletMapper) {
        this.walletMapper = walletMapper;
    }

    @Autowired
    private ConfigService configService;
    @Autowired
    private static Web3j web3j;
    @Autowired
    private ContractService  contractService;

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
        File file = new File(SkcConstants.NFS_WALLET_PATH);
        System.out.println(file.exists());
        if(!file.exists()){
            file.mkdirs();
        }
        String ethwalletName = WalletUtils.generateNewWalletFile("", new File(SkcConstants.NFS_WALLET_PATH), true);
        if (ethwalletName!=null){
            log.info("{}用户:{}钱包生成成功",LOG_PREFIX,userId);
            String walletFilePath = SkcConstants.NFS_WALLET_PATH + "/" + ethwalletName;
            Credentials credentials = WalletUtils.loadCredentials("", walletFilePath);
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
            initWeb3j();
            ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            String value =  results.get(0).getValue().toString();
            balanceValue = new BigDecimal(value);
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
        initWeb3j();
        EthGetBalance ethGetBlance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        String balance = Convert.fromWei(new BigDecimal(ethGetBlance.getBalance()), Convert.Unit.ETHER).toPlainString();
        return new BigDecimal(balance);
    }

    /**
     * 提现上链
     * @param fromAddress
     * @param toAddress
     * @param amount
     * @param fromPath 提现(上链)
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String withdraw(String fromAddress, String toAddress, BigDecimal amount,String fromPath) throws BusinessException, ExecutionException, InterruptedException, IOException, CipherException {
        //判断转出地址
        if (!toAddress.startsWith("0x") || toAddress.length() != 42) {
            throw new BusinessException(null);
        }
        String transactionHash;
        Credentials credentials = WalletUtils.loadCredentials("", fromPath);
        initWeb3j();
        BigDecimal eth = new BigDecimal(InfuraInfo.USDT_ETH.getDesc());
        String sysAddress = configService.getByKey(SkcConstants.SYS_WALLET_ADDRESS).getConfigValue();
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                sysAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        log.info("{} 系统钱包交易笔数:[{}]", LOG_PREFIX, nonce);
        Address transferAddress = new Address(toAddress);
        Uint256 value = new Uint256(new BigInteger(amount.multiply(eth).stripTrailingZeros().toPlainString()));
        List <Type> parametersList = new ArrayList <>();
        parametersList.add(transferAddress);
        parametersList.add(value);
        List <TypeReference <?>> outList = new ArrayList <>();
        Function transfer = new Function("transfer", parametersList, outList);
        String encodedFunction = FunctionEncoder.encode(transfer);
        BigInteger gasPrice = Convert.toWei(new BigDecimal(InfuraInfo.GAS_PRICE.getDesc()), Convert.Unit.GWEI).toBigInteger();

        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice,
                new BigInteger(InfuraInfo.GAS_SIZE.getDesc()), InfuraInfo.USDT_CONTRACT_ADDRESS.getDesc(), encodedFunction);
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        transactionHash = ethSendTransaction.getTransactionHash();
        if (StringUtils.isBlank(transactionHash)) {
            log.error("{}提现失败:{[]}", JSON.toJSONString(ethSendTransaction));
            throw new BusinessException("提现失败");
        }
        return transactionHash;
    }

    /**
     * 获取用户钱包可用余额
     *
     * @param userId
     * @param walletType
     * @return
     */
    @Override
    public ResponseResult getAvailBal(String userId, String walletType) {
        if (StringUtils.isBlank(userId) || StringUtils.isBlank(walletType)) {
            return ResponseResult.fail(ApiErrEnum.REQ_PARAM_NOT_NULL);
        }
        BigDecimal contartNum = contractService.queryContarct(userId);
        EntityWrapper<Wallet> walletWrapper = new EntityWrapper<>();
        walletWrapper.eq("user_id", userId);
        walletWrapper.eq("wallet_type", walletType);
        List<Wallet> walletList = walletMapper.selectList(walletWrapper);
        if (!CollectionUtils.isEmpty(walletList) && walletList.get(0) != null) {
            Wallet dbWallet = walletList.get(0);
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setWalletType(walletType);
            wallet.setWalletId(dbWallet.getWalletId());
            wallet.setBalTotal(dbWallet.getBalTotal());
            wallet.setBalAvail(dbWallet.getBalAvail());
            wallet.setBalFreeze(dbWallet.getBalFreeze());
            wallet.setComsumedContract(contartNum);
            wallet.setSurplusContract(dbWallet.getSurplusContract());
            wallet.setModifyTime(dbWallet.getModifyTime());
            return ResponseResult.success().setData(wallet);
        } else {
            return ResponseResult.fail(ApiErrEnum.USER_NOT_EXISTED);
        }
    }

    @Override
    public Wallet getWallet(String userId, WalletEum walletType) {
        EntityWrapper<Wallet> toWalletWrapper = new EntityWrapper<>();
        toWalletWrapper.eq(SkcConstants.USER_ID, userId);
        toWalletWrapper.eq(SkcConstants.WALLET_TYPE, walletType.getCode());
        List<Wallet> toWallets = walletMapper.selectList(toWalletWrapper);
        if (CollectionUtils.isEmpty(toWallets)){
            return null;
        }
        return toWallets.get(0);
    }

    /**
     * 获取用户所有钱包地址
     *
     * @param userId
     * @return
     */
    @Override
    public ResponseResult getAddress(String userId) {
        if (StringUtils.isBlank(userId)) {
            return ResponseResult.fail(ApiErrEnum.REQ_PARAM_NOT_NULL);
        }
        EntityWrapper<Wallet> walletWrapper = new EntityWrapper<>();
        walletWrapper.eq("user_id", userId);
        List<Wallet> walletList = walletMapper.selectList(walletWrapper);
        if (!CollectionUtils.isEmpty(walletList)) {
            List<Wallet> walletAdds = new ArrayList<>();
            for (Wallet wallet : walletList) {
                if (wallet != null) {
                    Wallet resWallet = new Wallet();
                    resWallet.setUserId(userId);
                    resWallet.setWalletType(wallet.getWalletType());
                    resWallet.setAddress(wallet.getAddress());
                    walletAdds.add(resWallet);
                }
            }
            return ResponseResult.success().setData(walletAdds);
        } else {
            return ResponseResult.fail(ApiErrEnum.USER_NOT_EXISTED);
        }
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
        wallet.setBalTotal(init);
        wallet.setSurplusContract(init);
        wallet.setComsumedContract(init);
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
     *
     * @param wallet
     * @return
     */
    private Map<String, String> getReturnWallet(Wallet wallet) {
        Map<String, String> map = new HashMap<>(2);
        map.put(ADDRESS, wallet.getAddress());
        map.put(MNEMONIC, wallet.getMnemonic());
        return map;
    }

    private void initWeb3j() {
        if (web3j == null) {
            Config config = configService.getByKey(SkcConstants.INFURA_ADDRESS);
            web3j = Web3j.build(new HttpService(config.getConfigValue()));
        }
    }

}
