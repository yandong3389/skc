package com.app.skc.service.Impl;

import com.alibaba.fastjson.JSON;
import com.app.skc.enums.*;
import com.app.skc.exception.BusinessException;
import com.app.skc.mapper.TransactionMapper;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.Transaction;
import com.app.skc.model.Wallet;
import com.app.skc.model.system.Config;
import com.app.skc.service.TransactionService;
import com.app.skc.service.WalletService;
import com.app.skc.service.system.ConfigService;
import com.app.skc.utils.date.DateUtil;
import com.app.skc.utils.jdbc.SqlUtils;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author
 * @since 2020-02-05
 */
@Service("transactionService")
public class TransactionServiceImpl extends ServiceImpl <TransactionMapper, Transaction> implements TransactionService {

    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private TransactionMapper transactionMapper;
    @Autowired
    private WalletService walletService;
    @Autowired
    private Web3j web3j;
    @Autowired
    private ConfigService configService;
    private static String ADDRESS = "address";
    private static String USER_ID = "user_id";
    private static String WALLTE_TYPE = "wallet_type";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";


/*
    @Autowired
    public TransactionServiceImpl(ConfigService configService,TransactionMapper transactionMapper,WalletMapper walletMapper){
        this.configService = configService;
        this.walletMapper = walletMapper;
        this.transactionMapper = transactionMapper;
        Config config = configService.getByKey("INFURA_ADDRESS");
        this.web3j = Web3j.build(new HttpService(config.getConfigValue()));
    }*/

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult transfer(String toWalletAddress, String amount, String userId, String walletType) throws InterruptedException, ExecutionException, BusinessException, CipherException, IOException {
        if (!toWalletAddress.startsWith("0x") || toWalletAddress.length() != 42) {
            return ResponseResult.fail(ApiErrEnum.ADDRESS_WALLET_FAIL);
        }
        if (!StringUtils.isNumeric(amount)) {
            return ResponseResult.fail(ApiErrEnum.TRANS_AMOUNT_INVALID);
        }
        if (WalletEum.getByCode(walletType) == null) {
            return ResponseResult.fail(ApiErrEnum.WALLET_TYPE_NOT_SUPPORTED);
        }
        //格式化转账金额
        BigDecimal transAmt = new BigDecimal(amount);
        BigDecimal fee = new BigDecimal(0);

        //获取发起转账钱包
        EntityWrapper<Wallet> fromWalletWrapper = new EntityWrapper<>();
        fromWalletWrapper.eq(USER_ID, userId);
        fromWalletWrapper.eq(WALLTE_TYPE, walletType);
        List<Wallet> fromWallets = walletMapper.selectList(fromWalletWrapper);
        Wallet fromWallet;
        if (fromWallets.size() > 0) {
            fromWallet = fromWallets.get(0);
        } else {
            return ResponseResult.fail();
        }
        //获取到账钱包
        EntityWrapper<Wallet> toWalletWrapper = new EntityWrapper<>();
        toWalletWrapper.eq(ADDRESS, toWalletAddress);
        toWalletWrapper.eq(WALLTE_TYPE, walletType);
        List<Wallet> toWallets = walletMapper.selectList(toWalletWrapper);
        Wallet toWallet = new Wallet();
        if (toWallets.size() > 0) {
            //转账
            toWallet = toWallets.get(0);
            Config config = configService.getByKey(SysConfigEum.SKC_TRANS_FEE.getCode());
            String value = config.getConfigValue();
            fee = transAmt.multiply(new BigDecimal(value));
            if (transAmt.doubleValue() > fromWallet.getBalance().doubleValue()) {
                return ResponseResult.fail(ApiErrEnum.NOT_ENOUGH_WALLET);
            }
            setTransBalance(transAmt, fee, fromWallet, toWallet);
        } else {
            //提现
            sysWalletOut(toWalletAddress, userId, walletType, transAmt);
        }
        saveTransaction(userId, walletType, fromWallet, toWallet, transAmt, fee);
        return ResponseResult.success();
    }

    /**
     * 设置转账钱包余额
     * @param trans 转账数量
     * @param fee 手续费
     * @param fromWallet 转账发起钱包
     * @param toWallet 到账钱包
     */
    private void setTransBalance(BigDecimal trans, BigDecimal fee, Wallet fromWallet, Wallet toWallet) {
        BigDecimal fromBalance = fromWallet.getBalance();
        fromWallet.setBalance(fromBalance.subtract(trans).subtract(fee));
        BigDecimal toBalance = toWallet.getBalance();
        toWallet.setBalance(toBalance.add(trans));
    }

    /**
     * 从系统钱包提现出去
     *
     * @param toWalletAddress 到账地址
     * @param userId          用户 id
     * @param walletType      用户钱包
     * @param trans           提现数量
     * @throws BusinessException
     * @throws IOException
     * @throws CipherException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void sysWalletOut(String toWalletAddress, String userId, String walletType, BigDecimal trans) throws BusinessException, IOException, CipherException, ExecutionException, InterruptedException {
        //若是没有钱包记录表示转账外部地址 择交易为提现
        Config walletAddress = configService.getByKey(SysConfigEum.WALLET_ADDRESS.getCode());
        Config walletPath = configService.getByKey(SysConfigEum.WALLET_PATH.getCode());
        if (WalletEum.SK.getCode().equals(walletType)) {
            BigDecimal mdcBalance = walletService.getERC20Balance(walletAddress.getConfigValue(), InfuraInfo.SKC_CONTRACT_ADDRESS.getDesc());
            if (mdcBalance.doubleValue() < trans.doubleValue()) {
                throw new BusinessException("交易失败,SKC不足");
            }
        } else if (WalletEum.USDT.getCode().equals(walletType)) {
            BigDecimal usdtBalance = walletService.getERC20Balance(walletAddress.getConfigValue(), InfuraInfo.USDT_CONTRACT_ADDRESS.getDesc());
            if (usdtBalance.doubleValue() < trans.doubleValue()) {
                throw new BusinessException("交易失败,USDT不足");
            }
        }
        transfer( trans.toString(), walletPath.getConfigValue(), walletAddress.getConfigValue(), toWalletAddress, walletType);
    }

    /**
     * 保存交易信息
     *
     * @param userId     用户 id
     * @param walletType 钱包类型
     * @param fromWallet 转账发起钱包
     * @param toWallet   到账钱包
     * @param trans      转账数量
     * @param fee        手续费
     */
    private void saveTransaction(String userId, String walletType, Wallet fromWallet, Wallet toWallet, BigDecimal trans, BigDecimal fee) {
        Transaction transaction = new Transaction();
        transaction.setFeeAmount(fee);
        transaction.setCreateTime(new Date());
        transaction.setFromAmount(trans);
        transaction.setFromUserId(Integer.parseInt(userId));
        transaction.setFromWalletAddress(fromWallet.getAddress());
        transaction.setFromWalletType(walletType);
        transaction.setToAmount(trans);
        if (toWallet.getUserId() != null) {
            transaction.setToUserId(toWallet.getUserId());
        }
        transaction.setToWalletAddress(toWallet.getAddress());
        transaction.setToWalletType(walletType);
        transaction.setTransactionStatus(TransactionEum.FINISH.getCode());
        transaction.setTransactionType(TransactionEum.TRANSFER.getCode());
        transactionMapper.insert(transaction);
        walletMapper.updateById(fromWallet);
        walletMapper.updateById(toWallet);
    }

    @Override
    public ResponseResult getETHBlance(Page page, Map <String, Object> params) {
        PageHelper.startPage(Integer.parseInt(params.get("pageNum").toString()), Integer.parseInt(params.get("pageSize").toString()));
        params.remove("pageNum");
        params.remove("pageSize");
        String transactionType = (String) params.get("transaction_type");
        EntityWrapper <Transaction> entityWrapper = new EntityWrapper <>();
        params.forEach((k, v) -> {
            if (!"transaction_type".equals(k)) {
                entityWrapper.eq(k, v);
            } else {
                entityWrapper.in("transaction_type", transactionType.split(","));
            }
        });
        entityWrapper.orderDesc(SqlUtils.orderBy("create_time"));
        List <Transaction> transactionList = transactionMapper.selectList(entityWrapper);
        return ResponseResult.success().setData(new PageInfo <>(transactionList));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult investUSDT(String userId, String toAddress, String investMoney) {
        BigDecimal invest = new BigDecimal(investMoney);
        Transaction transaction = new Transaction();
        transaction.setCreateTime(new Date());
        transaction.setToAmount(invest);
        /*transaction.setToUserId(Integer.parseInt(userId));*/
        transaction.setToWalletAddress(toAddress);
        //0-usdt
        transaction.setToWalletType(WalletEum.USDT.getCode());
        //0-待交易
        transaction.setTransactionStatus("0");
        //0-充值
        transaction.setTransactionType(TransactionEum.IN.getCode());
        transactionMapper.insert(transaction);
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String usdtCOntractAddress = InfuraInfo.USDT_CONTRACT_ADDRESS.getDesc();
        BigDecimal balance = walletService.getERC20Balance(toAddress, usdtCOntractAddress);
        if (balance != null && balance.doubleValue() >= new Double(investMoney)) {
            transaction.setTransactionStatus("1");
            transactionMapper.updateById(transaction);
        } else {
            confirm(new Date(), toAddress, usdtCOntractAddress, investMoney, userId, transaction.getTransactionId().toString());
        }
        return ResponseResult.success();
    }

    @Transactional(rollbackFor = Exception.class)
    public String transfer(String transferNumber, String fromPath, String fromAddress, String toAddress, String walletType) throws IOException, CipherException, ExecutionException, InterruptedException, BusinessException {
        BigDecimal trans = new BigDecimal(transferNumber);
        //判断转出地址
        if (!toAddress.startsWith("0x") || toAddress.length() != 42) {
            throw new BusinessException(null);
        }
        Credentials credentials = WalletUtils.loadCredentials(null, fromPath);
        Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().sendAsync().get();
        String clientVersion = web3ClientVersion.getWeb3ClientVersion();
        System.out.println("version=" + clientVersion);
        String transactionHash;

        BigDecimal eth;
        BigDecimal fee = new BigDecimal(0);
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                fromAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        Address transferAddress = new Address(toAddress);
        String contractAddress = "";
        if ("0".equals(walletType)) {
            eth = new BigDecimal(InfuraInfo.USDT_ETH.getDesc());
            contractAddress = InfuraInfo.USDT_CONTRACT_ADDRESS.getDesc();
        } else if ("1".equals(walletType)) {
            eth = new BigDecimal(InfuraInfo.MDC_ETH.getDesc());
            contractAddress = InfuraInfo.SKC_CONTRACT_ADDRESS.getDesc();
        } else {
            throw new BusinessException("交易失败");
        }
        Uint256 value = new Uint256(new BigInteger(trans.multiply(eth).stripTrailingZeros().toPlainString()));
        List <Type> parametersList = new ArrayList <>();
        parametersList.add(transferAddress);
        parametersList.add(value);
        List <TypeReference <?>> outList = new ArrayList <>();
        Function transfer = new Function("transfer", parametersList, outList);
        String encodedFunction = FunctionEncoder.encode(transfer);
        BigInteger gasPrice = Convert.toWei(new BigDecimal(InfuraInfo.GAS_PRICE.getDesc()), Convert.Unit.GWEI).toBigInteger();

        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice,
                new BigInteger(InfuraInfo.GAS_SIZE.getDesc()), contractAddress, encodedFunction);
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        transactionHash = ethSendTransaction.getTransactionHash();
        if (transactionHash == null || "".equals(transactionHash)) {
            throw new BusinessException("交易失败");
        }
        System.out.println(transactionHash);

        return transactionHash;
    }

    private BigDecimal getETHBalance(String address) throws IOException {
        EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameter.valueOf("latest")).send();
        //格式转化 wei-ether
        String blanceETH = Convert.fromWei(balance.getBalance().toString(), Convert.Unit.ETHER).toPlainString().concat(" ether").replace("ether", "");
        if (blanceETH == null || "".equals(blanceETH.trim())) {
            return new BigDecimal(0);
        }
        return new BigDecimal(blanceETH.trim());
    }




    private void confirm(Date time, String fromAddress, String contractAddress, String money, String userId, String transactionId) {
        //定时第一次15分钟后执行
        System.out.println(DateUtil.getDate(DATE_FORMAT, 0, time));
        System.out.println(DateUtil.getDate(DATE_FORMAT, Calendar.MINUTE, 15, time));
        EntityWrapper <Wallet> walletEntityWrapper = new EntityWrapper <>();
        walletEntityWrapper.eq("user_id", userId);
        Wallet wallet = walletMapper.selectList(walletEntityWrapper).get(0);
        Config config = configService.getByKey(SysConfigEum.WALLET_ADDRESS.getCode());
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();
        threadPoolTaskScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Date date = new Date();
                System.out.println(DateUtil.getDate(DATE_FORMAT, 0, date));
                BigDecimal balanceValue = walletService.getERC20Balance(fromAddress, contractAddress);
                if (balanceValue == null || balanceValue.doubleValue() <= new Double(money)) {
                    //钱未到账继续第二次定时
                    System.out.println(DateUtil.getDate(DATE_FORMAT, Calendar.HOUR_OF_DAY, 1, date));
                    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
                    scheduler.initialize();
                    scheduler.schedule(new Runnable() {
                        @Override
                        public void run() {
                            Date lastDate = new Date();
                            System.out.println(DateUtil.getDate(DATE_FORMAT, 0, lastDate));
                            BigDecimal balance = walletService.getERC20Balance(fromAddress, contractAddress);
                            if (balance == null || balance.doubleValue() <= new Double(money)) {
                                //钱未到账继续第三次定时
                                System.out.println(DateUtil.getDate(DATE_FORMAT, Calendar.HOUR_OF_DAY, 2, lastDate));
                                ThreadPoolTaskScheduler lastScheduler = new ThreadPoolTaskScheduler();
                                lastScheduler.initialize();
                                lastScheduler.schedule(new Runnable() {
                                    @Override
                                    public void run() {
                                        BigDecimal lastBalance = walletService.getERC20Balance(fromAddress, contractAddress);
                                        if (lastBalance == null || lastBalance.doubleValue() <= new Double(money)) {
                                            //钱未到账，交易失败
                                            updateTransaction(transactionId, "-1");
                                        } else {
                                            //钱已到账
                                            transact(money, wallet.getWalletPath(), wallet.getAddress(), config.getConfigValue(), "0", transactionId, "1");
                                        }
                                    }
                                }, DateUtil.getDate(Calendar.HOUR_OF_DAY, 2, lastDate));
                            } else {
                                //钱已到账
                                transact(money, wallet.getWalletPath(), wallet.getAddress(), config.getConfigValue(), "0", transactionId, "1");
                            }
                        }
                    }, DateUtil.getDate(Calendar.HOUR_OF_DAY, 1, date));
                } else {
                    //钱已到账
                    transact(money, wallet.getWalletPath(), wallet.getAddress(), config.getConfigValue(), "0", transactionId, "1");
                }
            }
        }, DateUtil.getDate(Calendar.MINUTE, 15, time));
    }

    private void transact(String balance, String walletPath, String walletAddress, String toAddress, String walletType, String transactionId, String transactionStatus) {
        try {
            transfer(balance, walletPath, walletAddress, toAddress, walletType);
            updateTransaction(transactionId, transactionStatus);
        } catch (IOException | CipherException | ExecutionException | InterruptedException | BusinessException e) {
            e.printStackTrace();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTransaction(String transactionId, String transactionStatus) {
        Transaction transaction = transactionMapper.selectById(transactionId);
        transaction.setTransactionStatus(transactionStatus);
        transactionMapper.updateById(transaction);

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult cashOutUSDT(String userId, String payPassword, String toAddress, String cashOutMoney, String verCode, String verId) throws InterruptedException {
        if (!toAddress.startsWith("0x") || toAddress.length() != 42) {
            return ResponseResult.fail(null);
        }
        BigDecimal cashOut = new BigDecimal(cashOutMoney);
        EntityWrapper <Wallet> walletEntityWrapper = new EntityWrapper <>();
        walletEntityWrapper.eq("user_id", userId);
        List <Wallet> wallets = walletMapper.selectList(walletEntityWrapper);
        Wallet wallet;
        //判断钱包是否存在
        Config config = configService.getByKey("USDT_CASH_OUT_FEE");
        if (wallets.size() > 0) {
            wallet = wallets.get(0);
            //判断USTD余额和MDC余额 0-USDT 1-MDC
            String value = config.getConfigValue();
          /*  BigDecimal balance = wallet.getUstdBlance();
            if(cashOut.doubleValue() > balance.doubleValue()){
                return ResponseResult.fail(ApiErrEnum.ERR205);
            }
            if(cashOut.doubleValue() < new Double(value)){
                return ResponseResult.fail("ERR209","提现金额必须大于手续费");
            }
            wallet.setUstdBlance(balance.subtract(cashOut));*/
            walletMapper.updateById(wallet);
        } else {
            /*return ResponseResult.fail(ApiErrEnum.ERR204);*/
        }
        Transaction transaction = new Transaction();
        transaction.setFeeAmount(new BigDecimal(config.getConfigValue()));
        transaction.setCreateTime(new Date());
        transaction.setFromAmount(cashOut);
        transaction.setFromUserId(Integer.parseInt(userId));
        /* transaction.setFromWalletAddress(wallet.getAddress());*/
        //0-usdt
        transaction.setFromWalletType("0");
        transaction.setToAmount(cashOut.subtract(transaction.getFeeAmount()));
        transaction.setToWalletAddress(toAddress);
        //0-usdt
        transaction.setToWalletType("0");
        //0-交易进行中
        transaction.setTransactionStatus("0");
        //1-提现
        transaction.setTransactionType("1");
        transactionMapper.insert(transaction);
        return ResponseResult.success();
    }

}
