package com.app.skc.service.Impl;

import com.app.skc.common.Exchange;
import com.app.skc.common.ExchangeCenter;
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
import com.app.skc.utils.BaseUtils;
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
import org.web3j.crypto.CipherException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.app.skc.enums.ApiErrEnum.NO_COMMISSION;
import static com.app.skc.enums.ApiErrEnum.NO_DEAL_PRICE;

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



    /**
     * 系统内部转账
     * @param toWalletAddress
     * @param amount
     * @param userId
     * @param walletType
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws BusinessException
     * @throws CipherException
     * @throws IOException
     */
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
            if (transAmt.doubleValue() > fromWallet.getBalAvail().doubleValue()) {
                return ResponseResult.fail(ApiErrEnum.NOT_ENOUGH_WALLET);
            }
            setTransBalance(transAmt, fee, fromWallet, toWallet);
        }
        saveTransaction(userId, walletType, fromWallet, toWallet, transAmt, fee);
        return ResponseResult.success();
    }

    /**
     * 设置转账钱包余额
     *
     * @param transAmt   转账数量
     * @param fee 手续费
     * @param fromWallet 转账发起钱包
     * @param toWallet 到账钱包
     */
    private void setTransBalance(BigDecimal transAmt, BigDecimal fee, Wallet fromWallet, Wallet toWallet) {
        // 设置转出账户余额
        BigDecimal fromBalTotal = fromWallet.getBalTotal();
        BigDecimal fromBalAvail = fromWallet.getBalAvail();
        fromWallet.setBalTotal(fromBalTotal.subtract(transAmt).subtract(fee));
        fromWallet.setBalAvail(fromBalAvail.subtract(transAmt).subtract(fee));

        // 设置转入账户余额
        BigDecimal toBalTotal = toWallet.getBalTotal();
        BigDecimal toBalAvail = toWallet.getBalAvail();
        toWallet.setBalTotal(toBalTotal.add(transAmt));
        toWallet.setBalAvail(toBalAvail.add(transAmt));
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
    private void sysWalletOut(String fromWalletAddress,String toWalletAddress, String userId, String walletType, BigDecimal trans) throws BusinessException, IOException, CipherException, ExecutionException, InterruptedException {
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
        walletService.withdraw(fromWalletAddress,toWalletAddress,trans,walletPath.getConfigValue());
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
        transaction.setFromUserId(userId);
        transaction.setFromWalletAddress(fromWallet.getAddress());
        transaction.setFromWalletType(walletType);
        transaction.setToAmount(trans);
        if (toWallet.getUserId() != null) {
            transaction.setToUserId(toWallet.getUserId());
        }
        transaction.setToWalletAddress(toWallet.getAddress());
        transaction.setToWalletType(walletType);
        transaction.setTransStatus(TransStatusEnum.SUCCESS.getCode());
        transaction.setTransType(TransTypeEum.TRANSFER.getCode());
        transactionMapper.insert(transaction);
        walletMapper.updateById(fromWallet);
        walletMapper.updateById(toWallet);
    }

    @Override
    public ResponseResult getETHBlance(Page page, Map <String, Object> params) {
        PageHelper.startPage(Integer.parseInt(params.get("pageNum").toString()), Integer.parseInt(params.get("pageSize").toString()));
        params.remove("pageNum");
        params.remove("pageSize");
        String transactionType = (String) params.get("trans_type");
        EntityWrapper <Transaction> entityWrapper = new EntityWrapper <>();
        params.forEach((k, v) -> {
            if (!"trans_type".equals(k)) {
                entityWrapper.eq(k, v);
            } else {
                entityWrapper.in("trans_type", transactionType.split(","));
            }
        });
        entityWrapper.orderDesc(SqlUtils.orderBy("create_time"));
        List <Transaction> transactionList = transactionMapper.selectList(entityWrapper);
        return ResponseResult.success().setData(new PageInfo <>(transactionList));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult invest(String userId, String toAddress, String amount) {
        BigDecimal investAmt = new BigDecimal(amount);
        Transaction transaction = new Transaction();
        transaction.setTransId(BaseUtils.get64UUID());
        transaction.setToUserId(userId);
        transaction.setToWalletType(WalletEum.USDT.getCode());
        transaction.setToWalletAddress(toAddress);
        transaction.setToAmount(investAmt);
        transaction.setTransStatus(TransStatusEnum.INIT.getCode());
        transaction.setTransType(TransTypeEum.IN.getCode()); // 4-充值
        transaction.setRemark(TransTypeEum.IN.getDesc());
        transaction.setCreateTime(new Date());
        transaction.setModifyTime(new Date());
        transactionMapper.insert(transaction);
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String usdtContractAdd = InfuraInfo.USDT_CONTRACT_ADDRESS.getDesc();
        BigDecimal balance = walletService.getERC20Balance(toAddress, usdtContractAdd);
        if (balance != null && balance.doubleValue() >= new Double(amount)) {
            transaction.setTransStatus(TransStatusEnum.SUCCESS.getCode());
            transaction.setModifyTime(new Date());
            transactionMapper.updateById(transaction);
            // 缺少 - 更新账户余额
        } else {
            confirm(new Date(), toAddress, usdtContractAdd, amount, userId, transaction.getTransId().toString());
        }
        return ResponseResult.success();
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
                                            updateTransaction(transactionId, TransStatusEnum.FAILED.getCode());
                                        } else {
                                            //钱已到账
                                            transact(money, wallet.getWalletPath(), wallet.getAddress(), config.getConfigValue(), WalletEum.USDT.getCode(), transactionId, TransStatusEnum.SUCCESS.getCode());
                                        }
                                    }
                                }, DateUtil.getDate(Calendar.HOUR_OF_DAY, 2, lastDate));
                            } else {
                                //钱已到账
                                transact(money, wallet.getWalletPath(), wallet.getAddress(), config.getConfigValue(), WalletEum.USDT.getCode(), transactionId, TransStatusEnum.SUCCESS.getCode());
                            }
                        }
                    }, DateUtil.getDate(Calendar.HOUR_OF_DAY, 1, date));
                } else {
                    //钱已到账
                    transact(money, wallet.getWalletPath(), wallet.getAddress(), config.getConfigValue(), WalletEum.USDT.getCode(), transactionId, TransStatusEnum.SUCCESS.getCode());
                }
            }
        }, DateUtil.getDate(Calendar.MINUTE, 15, time));
    }

    private void transact(String balance, String walletPath, String walletAddress, String toAddress, String walletType, String transactionId, String transactionStatus) {
        try {
            transfer(balance, walletPath, walletAddress, toAddress);
            updateTransaction(transactionId, transactionStatus);
        } catch (IOException | CipherException | ExecutionException | InterruptedException | BusinessException e) {
            e.printStackTrace();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTransaction(String transactionId, String transactionStatus) {
        Transaction transaction = transactionMapper.selectById(transactionId);
        transaction.setTransStatus(transactionStatus);
        transaction.setModifyTime(new Date());
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
        transaction.setFromUserId(userId);
        /* transaction.setFromWalletAddress(wallet.getAddress());*/
        //0-usdt
        transaction.setFromWalletType("0");
        transaction.setToAmount(cashOut.subtract(transaction.getFeeAmount()));
        transaction.setToWalletAddress(toAddress);
        //0-usdt
        transaction.setToWalletType("0");
        //0-交易进行中
        transaction.setTransStatus("0");
        //1-提现
        transaction.setTransType("1");
        transactionMapper.insert(transaction);
        return ResponseResult.success();
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult buy(String userId, String priceStr, Integer quantity) {
        ResponseResult result = ResponseResult.success();
        BigDecimal price = new BigDecimal(priceStr);
        ExchangeCenter exchangeCenter = ExchangeCenter.getInstance();
        List<Transaction> transactions = exchangeCenter.buy(userId, price, quantity);
        result.setData(transactions);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult sell(String userId, String priceStr, Integer quantity) {
        ResponseResult result = ResponseResult.success();
        BigDecimal price = new BigDecimal(priceStr);
        ExchangeCenter exchangeCenter = ExchangeCenter.getInstance();
        List<Transaction> transactions = exchangeCenter.sell(userId, price, quantity);
        result.setData(transactions);
        return result;
    }

    @Override
    public ResponseResult queryBuy() {
        List<Exchange> buyList = ExchangeCenter.getInstance().queryBuy();
        if (buyList == null)
            return ResponseResult.fail(NO_COMMISSION);
        return ResponseResult.success("", buyList);
    }

    @Override
    public ResponseResult querySell() {
        List<Exchange> sellList = ExchangeCenter.getInstance().querySell();
        if (sellList == null)
            return ResponseResult.fail(NO_COMMISSION);
        return ResponseResult.success("", sellList);
    }

    @Override
    public ResponseResult price() {
        BigDecimal price = ExchangeCenter.getInstance().price();
        if (price == null) {
            return ResponseResult.fail(NO_DEAL_PRICE);
        }
        return ResponseResult.success("", price);
    }
}
