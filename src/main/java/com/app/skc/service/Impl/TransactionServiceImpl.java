package com.app.skc.service.Impl;

import com.alibaba.fastjson.JSON;
import com.app.skc.enums.InfuraInfo;
import com.app.skc.exception.BusinessException;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.Transaction;
import com.app.skc.mapper.TransactionMapper;
import com.app.skc.model.Wallet;
import com.app.skc.model.system.Config;
import com.app.skc.service.TransactionService;
import com.app.skc.service.system.ConfigService;
import com.app.skc.utils.date.DateUtil;
import com.app.skc.utils.jdbc.SqlUtils;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
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
 *  服务实现类
 * </p>
 *
 * @author 
 * @since 2020-02-05
 */
@Service("transactionService")
public class TransactionServiceImpl extends ServiceImpl<TransactionMapper, Transaction> implements TransactionService {

    private  WalletMapper walletMapper;
    private  TransactionMapper transactionMapper;
    private  Web3j web3j;
    private  ConfigService configService;

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
    public ResponseResult transETH(String toWalletAddress,String transferNumber,String payPassword,String userId,String walletType,String verCode, String verId) throws InterruptedException, ExecutionException, BusinessException, CipherException, IOException {
        if(!toWalletAddress.startsWith("0x") || toWalletAddress.length() != 42){
            return ResponseResult.fail(null);
        }
        EntityWrapper<Wallet> entityWrapper = new EntityWrapper<>();
        entityWrapper.eq("user_id",userId);
        List<Wallet> fromWallets = walletMapper.selectList(entityWrapper);
        Wallet fromWallet;
        if(fromWallets.size() > 0){
            fromWallet = fromWallets.get(0);
        }else{
            return ResponseResult.fail();
        }
        EntityWrapper<Wallet> toWrapper = new EntityWrapper<>();
        toWrapper.eq("address",toWalletAddress);
        List<Wallet> toWallets = walletMapper.selectList(toWrapper);
        Wallet toWallet = new Wallet();
        if(toWallets.size() > 0){
            toWallet = toWallets.get(0);
        }


        BigDecimal trans = new BigDecimal(transferNumber);
        BigDecimal fee = new BigDecimal(0);
        if("0".equals(walletType)){
            Config config = configService.getByKey("USDT_TRANS_FEE");
            String value = config.getConfigValue();
            fee = trans.multiply(new BigDecimal(value));
/*

            if(trans.add(fee).doubleValue() > fromWallet.getUstdBlance().doubleValue()){
                return ResponseResult.fail(ApiErrEnum.ERR205);
            }
            BigDecimal usdtFrom = fromWallet.getUstdBlance();
*/

          /*  fromWallet.setUstdBlance(usdtFrom.subtract(trans).subtract(fee));
            if(toWallet.getUserId() != null){
                BigDecimal usdtTo = toWallet.getUstdBlance();
                toWallet.setUstdBlance(usdtTo.add(trans));
            }*/

        }else if("1".equals(walletType)){
            Config config = configService.getByKey("MDC_TRANS_FEE");
            String value = config.getConfigValue();
            fee = trans.multiply(new BigDecimal(value));
            /*if(trans.doubleValue() > fromWallet.getMdcBlance().doubleValue()){
                return ResponseResult.fail(ApiErrEnum.ERR206);
            }

            BigDecimal usdtFrom = fromWallet.getMdcBlance();

            fromWallet.setMdcBlance(usdtFrom.subtract(trans).subtract(fee));
            if(toWallet.getUserId() != null){
                BigDecimal usdtTo = toWallet.getMdcBlance();
                toWallet.setMdcBlance(usdtTo.add(trans));
            }*/

        }

        Transaction transaction = new Transaction();
        transaction.setFeeAmount(fee);
        transaction.setCreateTime(new Date());
        transaction.setFromAmount(trans);
        transaction.setFromUserId(Integer.parseInt(userId));
        transaction.setFromWalletAddress(fromWallet.getAddress());
        transaction.setFromWalletType(walletType);
        //0-usdt
        //transaction.setFromWalletType("0");
        transaction.setToAmount(trans);
        if(toWallet.getUserId() != null){
            transaction.setToUserId(toWallet.getUserId());
        }
        transaction.setToWalletAddress(toWalletAddress);
        transaction.setToWalletType(walletType);
        //1-交易完成
        transaction.setTransactionStatus("1");
        //1-提现
        transaction.setTransactionType("2");
        transactionMapper.insert(transaction);
       /* if(toWallet.getUserId() != null && fromWallet.getUserId().intValue() == toWallet.getUserId().intValue()){
            if("0".equals(walletType)){
                Config config = configService.getByKey("USDT_TRANS_FEE");
                String value = config.getConfigValue();
                fee = trans.multiply(new BigDecimal(value));
                fromWallet = walletMapper.selectById(fromWallet.getWalletId());
                BigDecimal usdtFrom = fromWallet.getUstdBlance();

                fromWallet.setUstdBlance(usdtFrom.subtract(fee));

            }else if("1".equals(walletType)){
                Config config = configService.getByKey("MDC_TRANS_FEE");
                String value = config.getConfigValue();
                fee = trans.multiply(new BigDecimal(value));
                if(trans.doubleValue() > fromWallet.getMdcBlance().doubleValue()){
                    return ResponseResult.fail(ApiErrEnum.ERR206);
                }

                BigDecimal usdtFrom = fromWallet.getMdcBlance();

                fromWallet.setMdcBlance(usdtFrom.subtract(fee));


            }
            walletMapper.updateById(fromWallet);
        }else if(toWallet.getUserId() != null && fromWallet.getUserId().intValue() != toWallet.getUserId().intValue()){
            walletMapper.updateById(fromWallet);
            walletMapper.updateById(toWallet);
        }else{
            walletMapper.updateById(fromWallet);
        }
*/
        if(toWallets.size() == 0){
            //若是没有钱包记录表示转账外部地址
            Config walletAddress = configService.getByKey("WALLET_ADDRESS");
            Config walletPath = configService.getByKey("WALLET_PATH");
            if("1".equals(walletType)){
                BigDecimal mdcBalance = getBalance(walletAddress.getConfigValue(),InfuraInfo.MDC_CONTRACT_ADDRESS.getDesc());
                if(mdcBalance.doubleValue() < trans.doubleValue()){
                    throw new BusinessException("交易失败,MDC不足");
                }

            }else if("0".equals(walletType)){
                BigDecimal usdtBalance = getBalance(walletAddress.getConfigValue(),InfuraInfo.USDT_CONTRACT_ADDRESS.getDesc());
                if(usdtBalance.doubleValue() < trans.doubleValue()){
                    throw new BusinessException("交易失败,USDT不足");
                }
            }

            transfer(userId,payPassword,"123456",transferNumber,walletPath.getConfigValue(),walletAddress.getConfigValue(),toWalletAddress,walletType);
        }
        return ResponseResult.success();
    }

    @Override
    public ResponseResult getETHBlance(Page page, Map<String,Object> params) {
        PageHelper.startPage(Integer.parseInt(params.get("pageNum").toString()),Integer.parseInt(params.get("pageSize").toString()));
        params.remove("pageNum");
        params.remove("pageSize");
        String transactionType = (String) params.get("transaction_type");
        EntityWrapper<Transaction> entityWrapper = new EntityWrapper<>();
        params.forEach((k,v)->{
            if(!"transaction_type".equals(k)){
                entityWrapper.eq(k,v);
            }else{
                entityWrapper.in("transaction_type",transactionType.split(","));
            }
        });
        entityWrapper.orderDesc(SqlUtils.orderBy("create_time"));
        List<Transaction> transactionList = transactionMapper.selectList(entityWrapper);
        return ResponseResult.success().setData(new PageInfo<>(transactionList));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult investUSDT(String userId,String toAddress, String investMoney) {
        BigDecimal invest = new BigDecimal(investMoney);
        Transaction transaction = new Transaction();
        transaction.setCreateTime(new Date());
        transaction.setToAmount(invest);
        /*transaction.setToUserId(Integer.parseInt(userId));*/
        transaction.setToWalletAddress(toAddress);
        //0-usdt
        transaction.setToWalletType("0");
        //0-待交易
        transaction.setTransactionStatus("0");
        //0-充值
        transaction.setTransactionType("0");
        transactionMapper.insert(transaction);
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String usdtCOntractAddress = InfuraInfo.USDT_CONTRACT_ADDRESS.getDesc();
        BigDecimal balance = getBalance(toAddress,usdtCOntractAddress);
        if(balance != null && balance.doubleValue()>= new Double(investMoney)){
            transaction.setTransactionStatus("1");
            transactionMapper.updateById(transaction);
        }else{
            confirm(new Date(),toAddress,usdtCOntractAddress,investMoney,userId,transaction.getTransactionId().toString());
        }
        return ResponseResult.success();
    }



    @Transactional(rollbackFor = Exception.class)
    public String transfer(String userId,String payPassword,String walletPassword,String transferNumber,String fromPath,String fromAddress,String toAddress,String walletType) throws IOException, CipherException, ExecutionException, InterruptedException, BusinessException {
        BigDecimal trans = new BigDecimal(transferNumber);

        //判断转出地址
        if(!toAddress.startsWith("0x") || toAddress.length() != 42){
            throw new BusinessException(null);
        }
        Credentials credentials = WalletUtils.loadCredentials(walletPassword, fromPath);
        /*Web3j web3j = Web3j.build(new HttpService(InfuraInfo.INFURA_ADDRESS.getDesc()));*/

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
        if("0".equals(walletType)){
            eth = new BigDecimal(InfuraInfo.USDT_ETH.getDesc());

            contractAddress = InfuraInfo.USDT_CONTRACT_ADDRESS.getDesc();
        }else if("1".equals(walletType)){
            eth = new BigDecimal(InfuraInfo.MDC_ETH.getDesc());
            contractAddress = InfuraInfo.MDC_CONTRACT_ADDRESS.getDesc();
        }else{
            throw new BusinessException("交易失败");
        }
        Uint256 value = new Uint256(new BigInteger(trans.multiply(eth).stripTrailingZeros().toPlainString()));
        List<Type> parametersList = new ArrayList<>();
        parametersList.add(transferAddress);
        parametersList.add(value);
        List<TypeReference<?>> outList = new ArrayList<>();
        Function transfer = new Function("transfer", parametersList, outList);
        String encodedFunction = FunctionEncoder.encode(transfer);
        BigInteger gasPrice = Convert.toWei(new BigDecimal(InfuraInfo.GAS_PRICE.getDesc()), Convert.Unit.GWEI).toBigInteger();

        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice,
                new BigInteger(InfuraInfo.GAS_SIZE.getDesc()),contractAddress, encodedFunction);
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        transactionHash = ethSendTransaction.getTransactionHash();
        if(transactionHash == null || "".equals(transactionHash)){
            throw new BusinessException("交易失败");
        }
        System.out.println(transactionHash);

        return transactionHash;
    }

    private BigDecimal getETHBalance(String address) throws IOException {
        EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameter.valueOf("latest")).send();
        //格式转化 wei-ether
        String blanceETH = Convert.fromWei(balance.getBalance().toString(), Convert.Unit.ETHER).toPlainString().concat(" ether").replace("ether","");
        if(blanceETH == null || "".equals(blanceETH.trim())){
            return new BigDecimal(0);
        }
        return new BigDecimal(blanceETH.trim());
    }

    private BigDecimal getBalance(String fromAddress,String contractAddress){
        //查询余额变化
        String methodName = "balanceOf";
        List<Type> inputParameters = new ArrayList<>();
        List<TypeReference<?>> outputParameters = new ArrayList<>();
        Address address = new Address(fromAddress);
        inputParameters.add(address);

        TypeReference<Uint256> typeReference = new TypeReference<Uint256>() {
        };
        outputParameters.add(typeReference);
        Function function = new Function(methodName, inputParameters, outputParameters);
        String data = FunctionEncoder.encode(function);
        org.web3j.protocol.core.methods.request.Transaction transactions = org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(fromAddress, contractAddress, data);

        EthCall ethCall;
        BigDecimal balanceValue = new BigDecimal(0);
        try {
            ethCall = web3j.ethCall(transactions, DefaultBlockParameterName.LATEST).send();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            System.out.println(JSON.toJSON(results));
            balanceValue = new BigDecimal((BigInteger) results.get(0).getValue()).divide(new BigDecimal("1000000"));
            System.out.println(balanceValue);
            return balanceValue;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return balanceValue;
    }

    private void confirm(Date time,String fromAddress,String contractAddress,String money,String userId,String transactionId){
        //定时第一次15分钟后执行
        System.out.println(DateUtil.getDate("yyyy-MM-dd HH:mm:ss",0,time));
        System.out.println(DateUtil.getDate("yyyy-MM-dd HH:mm:ss",Calendar.MINUTE,15,time));
        EntityWrapper<Wallet> walletEntityWrapper = new EntityWrapper<>();
        walletEntityWrapper.eq("user_id",userId);
        Wallet wallet = walletMapper.selectList(walletEntityWrapper).get(0);
        Config config = configService.getByKey("WALLET_ADDRESS");
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();
        threadPoolTaskScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Date date = new Date();
                System.out.println(DateUtil.getDate("yyyy-MM-dd HH:mm:ss",0,date));
                BigDecimal balanceValue = getBalance(fromAddress,contractAddress);
                if(balanceValue == null || balanceValue.doubleValue() <= new Double(money)){
                    //钱未到账继续第二次定时
                    System.out.println(DateUtil.getDate("yyyy-MM-dd HH:mm:ss",Calendar.HOUR_OF_DAY,1,date));
                    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
                    scheduler.initialize();
                    scheduler.schedule(new Runnable() {
                        @Override
                        public void run() {
                            Date lastDate = new Date();
                            System.out.println(DateUtil.getDate("yyyy-MM-dd HH:mm:ss",0,lastDate));
                            BigDecimal balance = getBalance(fromAddress,contractAddress);
                            if(balance == null || balance.doubleValue() <= new Double(money)){
                                //钱未到账继续第三次定时
                                System.out.println(DateUtil.getDate("yyyy-MM-dd HH:mm:ss",Calendar.HOUR_OF_DAY,2,lastDate));
                                ThreadPoolTaskScheduler lastScheduler = new ThreadPoolTaskScheduler();
                                lastScheduler.initialize();
                                lastScheduler.schedule(new Runnable() {
                                    @Override
                                    public void run() {
                                        BigDecimal lastBalance = getBalance(fromAddress,contractAddress);
                                        if(lastBalance == null || lastBalance.doubleValue() <= new Double(money)){
                                            //钱未到账，交易失败
                                            updateTransaction(transactionId,"-1");
                                        }else{
                                            //钱已到账
                                            transact(userId,null,null,money,wallet.getWalletPath(),wallet.getAddress(),config.getConfigValue(),"0",transactionId,"1");
                                        }
                                    }
                                },DateUtil.getDate(Calendar.HOUR_OF_DAY,2,lastDate));
                            }else{
                                //钱已到账
                                transact(userId,null,null,money,wallet.getWalletPath(),wallet.getAddress(),config.getConfigValue(),"0",transactionId,"1");
                            }
                        }
                    },DateUtil.getDate(Calendar.HOUR_OF_DAY,1,date));
                }else{
                    //钱已到账
                    transact(userId,null,null,money,wallet.getWalletPath(),wallet.getAddress(),config.getConfigValue(),"0",transactionId,"1");
                }
            }
        }, DateUtil.getDate(Calendar.MINUTE,15,time));
    }

    private void transact(String userId,String payPassword,String walletPassword,String balance,String walletPath,String walletAddress,String toAddress,String walletType,String transactionId,String transactionStatus){
        try {
            transfer(userId,payPassword,walletPassword,balance,walletPath,walletAddress,toAddress,walletType);
            updateTransaction(transactionId,transactionStatus);
        } catch (IOException | CipherException | ExecutionException | InterruptedException | BusinessException e) {
            e.printStackTrace();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTransaction(String transactionId,String transactionStatus){
        Transaction transaction = transactionMapper.selectById(transactionId);
        transaction.setTransactionStatus(transactionStatus);
        transactionMapper.updateById(transaction);

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseResult cashOutUSDT(String userId,String payPassword, String toAddress, String cashOutMoney,String verCode, String verId) throws InterruptedException {
        if(!toAddress.startsWith("0x") || toAddress.length() != 42){
            return ResponseResult.fail(null);
        }
        BigDecimal cashOut = new BigDecimal(cashOutMoney);
        EntityWrapper<Wallet> walletEntityWrapper = new EntityWrapper<>();
        walletEntityWrapper.eq("user_id",userId);
        List<Wallet> wallets = walletMapper.selectList(walletEntityWrapper);
        Wallet wallet;
        //判断钱包是否存在
        Config config = configService.getByKey("USDT_CASH_OUT_FEE");
        if(wallets.size() > 0){
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
        }else{
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
