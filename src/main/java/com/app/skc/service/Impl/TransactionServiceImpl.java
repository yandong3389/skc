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
import com.app.skc.utils.SkcConstants;
import com.app.skc.utils.jdbc.SqlUtils;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.Web3j;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.app.skc.enums.ApiErrEnum.NO_COMMISSION;
import static com.app.skc.enums.ApiErrEnum.NO_DEAL_PRICE;

/**
 * <p>
 * 交易服务实现类
 * </p>
 *
 * @author
 * @since 2020-02-05
 */
@Service("transactionService")
public class TransactionServiceImpl extends ServiceImpl <TransactionMapper, Transaction> implements TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);
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
        ResponseResult paramsChkRes = transParamsChk(walletType, toWalletAddress, amount);
        if (paramsChkRes != null) {
            return paramsChkRes;
        }
        //格式化转账金额
        BigDecimal transAmt = new BigDecimal(amount);

        //获取发起转账钱包
        EntityWrapper<Wallet> fromWalletWrapper = new EntityWrapper<>();
        fromWalletWrapper.eq(SkcConstants.USER_ID, userId);
        fromWalletWrapper.eq(SkcConstants.WALLET_TYPE, walletType);
        List<Wallet> fromWallets = walletMapper.selectList(fromWalletWrapper);
        Wallet fromWallet;
        if (fromWallets.size() > 0) {
            fromWallet = fromWallets.get(0);
        } else {
            return ResponseResult.fail(ApiErrEnum.WALLET_NOT_MAINTAINED);
        }
        //获取到账钱包
        EntityWrapper<Wallet> toWalletWrapper = new EntityWrapper<>();
        toWalletWrapper.eq(SkcConstants.ADDRESS, toWalletAddress);
        toWalletWrapper.eq(SkcConstants.WALLET_TYPE, walletType);
        List<Wallet> toWallets = walletMapper.selectList(toWalletWrapper);
        Wallet toWallet;
        if (toWallets.size() > 0) {
            toWallet = toWallets.get(0);
        } else {
            return ResponseResult.fail(ApiErrEnum.WALLET_NOT_MAINTAINED);
        }
        // 计算转账手续费
        Config config;
        if (WalletEum.SK.getCode().equals(walletType)) {
            config = configService.getByKey(SysConfigEum.SKC_TRANS_FEE.getCode());
        } else {
            config = configService.getByKey(SysConfigEum.USDT_TRANS_FEE.getCode());
        }
        String value = config.getConfigValue();
        BigDecimal fee = new BigDecimal(value);
        if (transAmt.doubleValue() > fromWallet.getBalAvail().doubleValue()) {
            return ResponseResult.fail(ApiErrEnum.NOT_ENOUGH_WALLET);
        }
        setTransBalance(transAmt, fee, fromWallet, toWallet);
        saveTransfer(userId, walletType, fromWallet, toWallet, transAmt, fee);
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
     * 保存转账信息
     *
     * @param userId     用户 id
     * @param walletType 钱包类型
     * @param fromWallet 转账发起钱包
     * @param toWallet   到账钱包
     * @param trans      转账数量
     * @param fee        手续费
     */
    private void saveTransfer(String userId, String walletType, Wallet fromWallet, Wallet toWallet, BigDecimal trans, BigDecimal fee) {
        Transaction transaction = new Transaction();
        transaction.setTransId(BaseUtils.get64UUID());
        transaction.setFeeAmount(fee);
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
        transaction.setCreateTime(new Date());
        transaction.setModifyTime(new Date());
        transactionMapper.insert(transaction);
        walletMapper.updateById(fromWallet);
        walletMapper.updateById(toWallet);
    }

    /**
     * 根据交易类型分页查询交易记录
     * s
     *
     * @param params trans_type-交易类型(必选)；from_user_id-用户id(可选)；to_user_id-用户id(可选)；wallet_type-钱包类型(可选)；trans_status-交易状态(可选)
     * @return
     */
    @Override
    public ResponseResult transQueryByPage(Map<String, Object> params) {
        EntityWrapper<Transaction> entityWrapper = buildTransWrapper(params);
        List<Transaction> transactionList = transactionMapper.selectList(entityWrapper);
        return ResponseResult.success().setData(new PageInfo<>(transactionList));
    }

    private EntityWrapper<Transaction> buildTransWrapper(Map<String, Object> params) {
        Page page = new Page();
        if (StringUtils.isNotBlank((String) params.get(SkcConstants.PAGE_NUM))) {
            page.setPageNum(Integer.valueOf((String) params.get(SkcConstants.PAGE_NUM)));
            params.remove(SkcConstants.PAGE_NUM);
        }
        if (StringUtils.isNotBlank((String) params.get(SkcConstants.PAGE_SIZE))) {
            page.setPageSize(Integer.valueOf((String) params.get(SkcConstants.PAGE_SIZE)));
            params.remove(SkcConstants.PAGE_SIZE);
        }
        PageHelper.startPage(page);
        EntityWrapper<Transaction> entityWrapper = new EntityWrapper<>();
        if (StringUtils.isNotBlank((String) params.get(SkcConstants.FROM_USER_ID))) {
            entityWrapper.eq(SkcConstants.FROM_USER_ID, params.get(SkcConstants.FROM_USER_ID));
        }
        if (StringUtils.isNotBlank((String) params.get(SkcConstants.TO_USER_ID))) {
            entityWrapper.eq(SkcConstants.TO_USER_ID, params.get(SkcConstants.TO_USER_ID));
        }
        if (StringUtils.isNotBlank((String) params.get(SkcConstants.WALLET_TYPE))) {
            entityWrapper.eq(SkcConstants.WALLET_TYPE, params.get(SkcConstants.WALLET_TYPE));
        }
        if (StringUtils.isNotBlank((String) params.get(SkcConstants.TRANS_STATUS))) {
            entityWrapper.eq(SkcConstants.TRANS_STATUS, params.get(SkcConstants.TRANS_STATUS));
        }
        String transType = (String) params.get(SkcConstants.TRANS_TYPE);
        params.forEach((k, v) -> {
            if (!SkcConstants.TRANS_TYPE.equals(k)) {
                entityWrapper.eq(k, v);
            } else {
                entityWrapper.in(SkcConstants.TRANS_TYPE, transType.split(SkcConstants.COMMA_EN));
            }
        });
        entityWrapper.orderDesc(SqlUtils.orderBy("create_time"));
        return entityWrapper;
    }


    private void updateTransaction(String transactionId, String transactionStatus) {
        Transaction transaction = transactionMapper.selectById(transactionId);
        transaction.setTransStatus(transactionStatus);
        transaction.setModifyTime(new Date());
        transactionMapper.updateById(transaction);
    }

    /**
     * 从系统钱包提现出去
     *
     * @param toWalletAddress 到账地址
     * @param walletType      用户钱包
     * @param trans           提现数量
     * @throws BusinessException
     * @throws IOException
     * @throws CipherException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private String sysWalletOut(String fromWalletAddress, String toWalletAddress, String walletType, BigDecimal trans) throws BusinessException, IOException, CipherException, ExecutionException, InterruptedException {
        Config walletAddress = configService.getByKey(SkcConstants.SYS_WALLET_ADDRESS);
        Config walletPath = configService.getByKey(SkcConstants.SYS_WALLET_FILE);
        if (WalletEum.SK.getCode().equals(walletType)) {
            BigDecimal mdcBalance = walletService.getERC20Balance(walletAddress.getConfigValue(), InfuraInfo.SKC_CONTRACT_ADDRESS.getDesc());
            if (mdcBalance.doubleValue() < trans.doubleValue()) {
                logger.info("提现交易失败，SKC不足，当前SKC余额[{}]，提现金额[{}]", mdcBalance.doubleValue(), trans.doubleValue());
                throw new BusinessException("交易失败,SKC不足");
            }
        } else if (WalletEum.USDT.getCode().equals(walletType)) {
            BigDecimal usdtBalance = walletService.getERC20Balance(walletAddress.getConfigValue(), InfuraInfo.USDT_CONTRACT_ADDRESS.getDesc());
            if (usdtBalance.doubleValue() < trans.doubleValue()) {
                logger.info("提现交易失败，USDT不足，当前USDT余额[{}]，提现金额[{}]", usdtBalance.doubleValue(), trans.doubleValue());
                throw new BusinessException("交易失败,USDT不足");
            }
        }
        return walletService.withdraw(fromWalletAddress, toWalletAddress, trans, walletPath.getConfigValue());
    }

    /**
     * 系统提现到外部账户
     *
     * @param userId     用户id
     * @param walletType 钱包类型
     * @param toAddress  提现地址
     * @param amount     提现金额
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult cashOut(String userId, String walletType,String toAddress, String amount) throws InterruptedException, IOException, ExecutionException, CipherException, BusinessException {
        ResponseResult paramsChkRes = transParamsChk(walletType, toAddress, amount);
        if(paramsChkRes != null) {return paramsChkRes;}
        BigDecimal cashOutAmt = new BigDecimal(amount);
        EntityWrapper<Wallet> fromWalletWrapper = new EntityWrapper<>();
        fromWalletWrapper.eq(SkcConstants.USER_ID, userId);
        fromWalletWrapper.eq(SkcConstants.WALLET_TYPE, walletType);
        List<Wallet> fromWalletRes = walletMapper.selectList(fromWalletWrapper);
        Wallet fromWallet;
        //判断钱包是否存在
        if (fromWalletRes.size() > 0) {
            fromWallet = fromWalletRes.get(0);
        } else {
            return ResponseResult.fail(ApiErrEnum.WALLET_NOT_MAINTAINED);
        }
        logger.info("提现转出钱包查询成功，钱包地址为[{}]", fromWallet.getAddress());
        // 计算转账手续费
        Config feeConfig;
        if (WalletEum.SK.getCode().equals(walletType)) {
            feeConfig = configService.getByKey(SysConfigEum.SKC_CASHOUT_FEE.getCode());
        } else {
            feeConfig = configService.getByKey(SysConfigEum.USDT_CASHOUT_FEE.getCode());
        }
        String feeValue = feeConfig.getConfigValue();
        logger.info("提现手续费查询成功，值为[{}]", feeValue);
        BigDecimal fee = new BigDecimal(feeValue);
        if ((cashOutAmt.doubleValue() + fee.doubleValue()) > fromWallet.getBalAvail().doubleValue()) {
            logger.info("提现失败，余额不足，转出金额为[{}], 手续费为[{}]，当前钱包可用余额为[{}]", cashOutAmt.doubleValue(), feeValue, fromWallet.getBalAvail().doubleValue());
            return ResponseResult.fail(ApiErrEnum.NOT_ENOUGH_WALLET);
        }

        Config needApproval = configService.getByKey(SysConfigEum.NEED_CASHOUT_VERIFY.getCode());
        boolean needVerify = false;
        String transHash = "";
        if (needApproval != null && "Y".equals(needApproval.getConfigValue())) {
            logger.info("提现需要审批，保存待审核提现记录");
            needVerify = true;
        } else {
            logger.info("提现无需审批，开始提现流程");
            transHash = sysWalletOut(fromWallet.getAddress(), toAddress, walletType, cashOutAmt);
        }
        saveCashOut(userId, walletType, toAddress, cashOutAmt, fromWallet, fee, needVerify, transHash);
        return ResponseResult.success();
    }

    private void saveCashOut(String userId, String walletType, String toAddress, BigDecimal cashOutAmt, Wallet fromWallet, BigDecimal fee, boolean needVerify, String transHash) {
        Transaction transaction = new Transaction();
        transaction.setTransId(BaseUtils.get64UUID());
        transaction.setTransType(TransTypeEum.OUT.getCode());
        transaction.setFromUserId(userId);
        transaction.setFromWalletType(walletType);
        transaction.setFromWalletAddress(fromWallet.getAddress());
        transaction.setFromAmount(cashOutAmt);
        transaction.setToAmount(cashOutAmt);
        transaction.setToWalletAddress(toAddress);
        transaction.setFeeAmount(fee);
        if (needVerify) {
            transaction.setTransStatus(TransStatusEnum.INIT.getCode());
        } else {
            // 设置转出账户余额
            BigDecimal fromBalTotal = fromWallet.getBalTotal();
            BigDecimal fromBalAvail = fromWallet.getBalAvail();
            fromWallet.setBalTotal(fromBalTotal.subtract(cashOutAmt).subtract(fee));
            fromWallet.setBalAvail(fromBalAvail.subtract(cashOutAmt).subtract(fee));
            walletMapper.updateById(fromWallet);
            logger.info("提现交易 - 钱包相关余额更新成功。");
            // 交易记录保存
            transaction.setTransHash(transHash);
            transaction.setTransStatus(TransStatusEnum.SUCCESS.getCode());
        }
        transaction.setCreateTime(new Date());
        transaction.setModifyTime(new Date());
        transactionMapper.insert(transaction);
        logger.info("提现交易 - 交易记录保存成功");
    }

    private ResponseResult transParamsChk(String walletType, String toAddress, String amount) {
        if (!toAddress.startsWith("0x") || toAddress.length() != 42) {
            return ResponseResult.fail(ApiErrEnum.ADDRESS_WALLET_FAIL);
        }
        try {
            BigDecimal bigDecimal = new BigDecimal(amount);
        } catch (Exception e) {
            return ResponseResult.fail(ApiErrEnum.TRANS_AMOUNT_INVALID);
        }
        if (WalletEum.getByCode(walletType) == null) {
            return ResponseResult.fail(ApiErrEnum.WALLET_TYPE_NOT_SUPPORTED);
        }
        return null;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult buy(String userId, String priceStr, Integer quantity) {
        //获取到账钱包
        EntityWrapper<Wallet> toWalletWrapper = new EntityWrapper<>();
        toWalletWrapper.eq(SkcConstants.USER_ID, userId);
        toWalletWrapper.eq(SkcConstants.WALLET_TYPE, WalletEum.USDT.getCode());
        List<Wallet> toWallets = walletMapper.selectList(toWalletWrapper);
        if (CollectionUtils.isEmpty(toWallets)){
            return ResponseResult.fail(ApiErrEnum.WALLET_NOT_MAINTAINED);
        }
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
        //获取到账钱包
        EntityWrapper<Wallet> toWalletWrapper = new EntityWrapper<>();
        toWalletWrapper.eq(SkcConstants.USER_ID, userId);
        toWalletWrapper.eq(SkcConstants.WALLET_TYPE, WalletEum.SK.getCode());
        List<Wallet> toWallets = walletMapper.selectList(toWalletWrapper);
        if (CollectionUtils.isEmpty(toWallets)){
            return ResponseResult.fail(ApiErrEnum.WALLET_NOT_MAINTAINED);
        }
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
        if(buyList == null){
            return ResponseResult.fail(NO_COMMISSION);
        }
        return ResponseResult.success("", buyList);
    }

    @Override
    public ResponseResult querySell() {
        List<Exchange> sellList = ExchangeCenter.getInstance().querySell();
        if (sellList == null){
            return ResponseResult.fail(NO_COMMISSION);
        }
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

    @Override
    public ResponseResult getEntrust(String userId) {
        ExchangeCenter exchangeCenter = ExchangeCenter.getInstance();
        return ResponseResult.success("", exchangeCenter.getEntrust(userId));
    }

    @Override
    public ResponseResult kline() {
        ExchangeCenter exchangeCenter = ExchangeCenter.getInstance();
        return ResponseResult.success("成功",exchangeCenter.kline());
    }

    @Override
    public ResponseResult cancelEntrust(String userId, String entrustOrder) {
        ExchangeCenter exchangeCenter = ExchangeCenter.getInstance();
        boolean ret = exchangeCenter.cancelEntrust(userId, entrustOrder);
        if (ret) {
            return ResponseResult.success("取消成功", true);
        }else {
            return ResponseResult.fail(ApiErrEnum.NO_ENTRUST_ORDER);
        }
    }
}
