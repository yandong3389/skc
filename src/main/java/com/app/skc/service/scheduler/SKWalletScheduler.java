package com.app.skc.service.scheduler;

import com.alibaba.fastjson.JSON;
import com.app.skc.enums.InfuraInfo;
import com.app.skc.enums.TransStatusEnum;
import com.app.skc.enums.TransTypeEum;
import com.app.skc.enums.WalletEum;
import com.app.skc.mapper.TransactionMapper;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.Transaction;
import com.app.skc.model.Wallet;
import com.app.skc.model.system.Config;
import com.app.skc.service.system.ConfigService;
import com.app.skc.utils.BaseUtils;
import com.app.skc.utils.SkcConstants;
import com.app.skc.utils.WebUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
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
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Configuration
@EnableScheduling
public class SKWalletScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SKWalletScheduler.class);
    private static final String LOG_PREFIX = "[交易充值] - ";


    @Autowired
    private TransactionMapper transactionMapper;
    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private ConfigService configService;
    @Value("${recharge.local-address}")
    private String localAddress;

    @Scheduled(cron = "0 */5 * * * ?")
    public void invest() throws ExecutionException, InterruptedException {
        logger.info("{}开始监听充值交易...", LOG_PREFIX);
        String address = WebUtils.getHostAddress();
        logger.info("{}获取本机地址:[{}]", LOG_PREFIX, address);
        if (!address.equals(localAddress)) {
            logger.info("{}监听充值交易结束,监听客户端地址错误");
            return;
        }
        String contractAddress = InfuraInfo.USDT_CONTRACT_ADDRESS.getDesc();
        Map<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put("wallet_type", WalletEum.USDT.getCode());
        List<Wallet> wallets = walletMapper.selectByMap(paramsMap);
        Config walletAddress = configService.getByKey(SkcConstants.SYS_WALLET_ADDRESS);
        String sysWalletPath = configService.getByKey(SkcConstants.SYS_WALLET_FILE).getConfigValue();
        Config config = configService.getByKey(SkcConstants.INFURA_ADDRESS);
//        Web3j web3j = Web3j.build(new HttpService("https://mainnet.infura.io/v3/2e130baab5ed43768780e3de46b44257"));
        Web3j web3j = Web3j.build(new HttpService(config.getConfigValue()));
        Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().sendAsync().get();
        String clientVersion = web3ClientVersion.getWeb3ClientVersion();
        System.out.println("version=" + clientVersion);
        for (Wallet wallet : wallets) {
            logger.info("{}开始钱包[{}]充值交易", LOG_PREFIX, wallet.getAddress());
            try {
                BigDecimal balance = getBalance(web3j, wallet.getAddress(), contractAddress);
                if (balance.doubleValue() > (double) 0) {
                    BigDecimal ethBalance = getEthBalance(web3j, wallet.getAddress());
                    if (ethBalance.doubleValue() <= 0.001) {
                            logger.info("{}钱包[{}]充值eth余额不足，当前余额[{}].", LOG_PREFIX, wallet.getAddress(), ethBalance.doubleValue());
                        //转手续费
                        Credentials credentials = WalletUtils.loadCredentials("", sysWalletPath);
                            Transfer.sendFunds(web3j, credentials, wallet.getAddress(), new BigDecimal(3), Convert.Unit.FINNEY).send();
                            logger.info("{}钱包[{}]充值eth手续费转账成功，待下个批次执行充值.", LOG_PREFIX, wallet.getAddress());
                        } else {
                        //充值
                            String transHash = transfer(web3j, wallet.getWalletPath(), wallet.getAddress(), walletAddress.getConfigValue(), contractAddress, balance);
                            if (StringUtils.isNotBlank(transHash)) {
                                logger.info("{}钱包[{}]充值成功，充值金额[{}].", LOG_PREFIX, wallet.getAddress(), balance.doubleValue());
                                // 交易记录
                                Transaction transaction = new Transaction();
                                transaction.setTransId(BaseUtils.get64UUID());
                                transaction.setToUserId(wallet.getUserId());
                                transaction.setToWalletType(WalletEum.USDT.getCode());
                                transaction.setToWalletAddress(wallet.getAddress());
                                transaction.setToAmount(balance);
                                transaction.setTransStatus(TransStatusEnum.SUCCESS.getCode());
                                transaction.setTransType(TransTypeEum.IN.getCode()); // 4-充值
                                transaction.setTransHash(transHash);
                                transaction.setRemark(TransTypeEum.IN.getDesc());
                                transaction.setCreateTime(new Date());
                                transaction.setModifyTime(new Date());
                                transactionMapper.insert(transaction);
                                logger.info("{}钱包[{}]充值成功，充值金额[{}].", LOG_PREFIX, wallet.getAddress(), balance.doubleValue());
                                // 钱包余额
                                wallet.setBalAvail(wallet.getBalAvail().add(balance));
                                wallet.setBalTotal(wallet.getBalTotal().add(balance));
                                walletMapper.updateById(wallet);
                                logger.info("{}钱包[{}]充值交易记录、余额更新成功.", LOG_PREFIX, wallet.getAddress());
                            } else {
                                logger.warn("{}钱包[{}]充值交易失败，交易Hash为[{}].", LOG_PREFIX, wallet.getAddress(), transHash);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("{}钱包[{}]充值交易失败.", LOG_PREFIX, wallet.getAddress(), e);
                }
            }
        logger.info("{}监听充值交易完成.", LOG_PREFIX);
        }

        private BigDecimal getEthBalance(Web3j web3j,String address) throws IOException {
            EthGetBalance balance = web3j.ethGetBalance(address, DefaultBlockParameter.valueOf("latest")).send();
            //格式转化 wei-ether
            String balanceETH = Convert.fromWei(balance.getBalance().toString(), Convert.Unit.ETHER).toPlainString().concat(" ether").replace("ether", "");
            if (StringUtils.isBlank(balanceETH)) {
                return new BigDecimal(0);
            }
            return new BigDecimal(balanceETH.trim());
        }

        private BigDecimal getBalance(Web3j web3j, String fromAddress, String contractAddress) throws IOException {
            //查询余额变化
            String methodName = "balanceOf";
            List<Type> inputParameters = new ArrayList <>();
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
            BigDecimal balanceValue;
            ethCall = web3j.ethCall(transactions, DefaultBlockParameterName.LATEST).send();
            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            System.out.println(JSON.toJSON(results));
            balanceValue = new BigDecimal((BigInteger) results.get(0).getValue()).divide(new BigDecimal("1000000"));
            System.out.println(balanceValue);
            return balanceValue;
        }

    private String transfer(Web3j web3j, String fromPath, String fromAddress, String toAddress, String contractAddress, BigDecimal trans) throws IOException, CipherException, ExecutionException, InterruptedException {
        Credentials credentials = WalletUtils.loadCredentials("", fromPath);
        /*Web3j web3j = Web3j.build(new HttpService(InfuraInfo.INFURA_ADDRESS.getDesc()));*/
        String transactionHash;

        BigDecimal eth = new BigDecimal(InfuraInfo.USDT_ETH.getDesc());
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                fromAddress, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        Address transferAddress = new Address(toAddress);
        Uint256 value = new Uint256(new BigInteger(trans.multiply(eth).stripTrailingZeros().toPlainString()));
        List<Type> parametersList = new ArrayList<>();
        parametersList.add(transferAddress);
        parametersList.add(value);
        List<TypeReference<?>> outList = new ArrayList<>();
        Function transfer = new Function("transfer", parametersList, outList);
        String encodedFunction = FunctionEncoder.encode(transfer);
        BigInteger gasPrice = Convert.toWei(new BigDecimal(InfuraInfo.GAS_PRICE.getDesc()), Convert.Unit.GWEI).toBigInteger();

        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice,
                new BigInteger(InfuraInfo.GAS_SIZE.getDesc()), contractAddress, encodedFunction);
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        transactionHash = ethSendTransaction.getTransactionHash();
        return transactionHash;
    }
}



