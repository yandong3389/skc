package com.app.skc.common;

import com.alibaba.fastjson.JSON;
import com.app.skc.enums.TransStatusEnum;
import com.app.skc.enums.TransTypeEum;
import com.app.skc.enums.WalletEum;
import com.app.skc.model.Transaction;
import com.app.skc.model.Wallet;
import com.app.skc.service.FeeService;
import com.app.skc.service.WalletService;
import com.app.skc.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 交易所交易数据中心
 */
@Component
public class ExchangeCenter {

    private static final String BUYING_LEADS = "buyingLeads";
    private static final String SELL_LEADS = "sellLeads";
    //最新成交价
    private static final String LAST_PRICE = "lastPrice";

    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private WalletService walletService;
    @Autowired
    private FeeService feeService;

    public String price() {
        return redisUtils.get(LAST_PRICE);
    }

    public List<Exchange> queryBuy() {
        long size = redisUtils.lGetListSize(BUYING_LEADS);
        if (size == 0) {
            return null;
        }
        List<String> strings = redisUtils.lGet(BUYING_LEADS, 0, -1);
        List<Exchange> exchanges = mergeList(strings);
        return exchanges.subList(0,Math.min(exchanges.size(),5));
    }

    private List<Exchange> mergeList(List<String> exchanges) {
        List<Exchange> retList = new ArrayList<>();
        for (String exchangeStr : exchanges) {
            Exchange exchange = JSON.parseObject(exchangeStr, Exchange.class);
            Exchange ex = new Exchange();
            ex.setQuantity(exchange.getQuantity());
            ex.setPrice(exchange.getPrice());
            if (retList.size() == 0) {
                retList.add(ex);
                continue;
            }
            Exchange lastExchange = retList.get(retList.size() - 1);
            if (lastExchange.getPrice().equals(ex.getPrice())) {
                lastExchange.setQuantity(lastExchange.getQuantity().add(ex.getQuantity()));
            } else {
                retList.add(ex);
            }
        }
        return retList;
    }

    public List<Exchange> querySell() {
        long size = redisUtils.lGetListSize(SELL_LEADS);
        if (size == 0) {
            return null;
        }
        List<String> strings = redisUtils.lGet(SELL_LEADS, 0, -1);
        List<Exchange> exchanges = mergeList(strings);
        Collections.reverse(exchanges);
        return exchanges.subList(Math.max(exchanges.size() - 5 , 0), exchanges.size());
    }

    public List<Transaction> buy(String buyUserId, BigDecimal buyPrice, BigDecimal buyQuantity) {
        List<Transaction> transactionList = new ArrayList<>();
        Exchange exchange = new Exchange(buyUserId, TransTypeEum.BUY, buyPrice, buyQuantity);
        Transaction transaction = buyFirst(exchange);
        while (transaction != null) {
            transactionList.add(transaction);
            transaction = buyFirst(exchange);
        }
        return transactionList;
    }

    public List<Transaction> sell(String buyUserId, BigDecimal buyPrice, BigDecimal sellQuantity) {
        List<Transaction> transactionList = new ArrayList<>();
        Exchange exchange = new Exchange(buyUserId, TransTypeEum.SELL, buyPrice, sellQuantity);
        Transaction transaction = sellFirst(exchange);
        while (transaction != null) {
            transactionList.add(transaction);
            transaction = sellFirst(exchange);
        }
        return transactionList;
    }

    public List<Exchange> getEntrust(String userId){
        List<Exchange> retList = new ArrayList<>();
        if (userId == null)
            return  retList;
        List<String> buyList = redisUtils.lGet(BUYING_LEADS, 0, -1);
        List<String> sellList = redisUtils.lGet(SELL_LEADS, 0, -1);
        List<String> exchangeList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(buyList)) {
            exchangeList.addAll(buyList);
        }
        if (!CollectionUtils.isEmpty(sellList)) {
            exchangeList.addAll(sellList);
        }
        if (CollectionUtils.isEmpty(exchangeList))
            return retList;
        for (String exchangeStr : exchangeList) {
            Exchange exchange = JSON.parseObject(exchangeStr, Exchange.class);
            if (userId.equals(exchange.getUserId())) {
                retList.add(exchange);
            }
        }
        return retList;
    }

    public boolean cancelEntrust(String userId,String entrustOrder){
        if (userId == null || entrustOrder == null)
            return false;
        return cancelBuyEntrust( entrustOrder) || cancelSellEntrust(entrustOrder);
    }

    private boolean cancelBuyEntrust(String order){
        List<String> strings = redisUtils.lGet(BUYING_LEADS, 0, -1);
        if (CollectionUtils.isEmpty(strings))
            return false;
        for (String string : strings) {
            Exchange exchange = JSON.parseObject(string, Exchange.class);
            if (order.equals(exchange.getEntrustOrder())) {
                redisUtils.lRemove(BUYING_LEADS, 1, string);
                Wallet wallet = walletService.getWallet(exchange.getUserId(), WalletEum.USDT);
                BigDecimal buyAmount = exchange.getQuantity().multiply(exchange.getPrice());
                buyAmount = feeService.buyerAmount(buyAmount);
                unfreezeBalance(wallet, buyAmount);
                return true;
            }
        }
        return false;
    }

    private boolean cancelSellEntrust(String order){
        List<String> strings = redisUtils.lGet(SELL_LEADS, 0, -1);
        if (CollectionUtils.isEmpty(strings))
            return false;
        for (String string : strings) {
            Exchange exchange = JSON.parseObject(string, Exchange.class);
            if (order.equals(exchange.getEntrustOrder())) {
                redisUtils.lRemove(SELL_LEADS, 1, string);
                Wallet wallet = walletService.getWallet(exchange.getUserId(), WalletEum.SK);
                unfreezeBalance(wallet,exchange.getQuantity());
                return true;
            }
        }
        return false;
    }

    private Transaction buyFirst(Exchange buyExchange) {
        BigDecimal buyQuantity = buyExchange.getQuantity();
        if (buyQuantity.compareTo(BigDecimal.ZERO) <= 0)
            return null;
        String sellStr = redisUtils.lGetIndex(SELL_LEADS, 0);
        if (sellStr == null) {
            insertBuy(buyExchange);
            return null;
        }
        Exchange sell = JSON.parseObject(sellStr,Exchange.class);
        BigDecimal sellPrice = sell.getPrice();
        BigDecimal sellQuantity = sell.getQuantity();
        if (buyExchange.getPrice().compareTo(sellPrice) < 0) {
            insertBuy(buyExchange);
            return null;
        }
        redisUtils.set(LAST_PRICE,String.format("%.4f",sellPrice));
        BigDecimal transQuantity;
        if (buyQuantity.compareTo(sellQuantity) >= 0) {
            buyExchange.setQuantity(buyQuantity.subtract(sellQuantity));
            redisUtils.leftPop(SELL_LEADS);
            transQuantity = sellQuantity;
        } else {
            buyExchange.setQuantity(BigDecimal.ZERO);
            sell.setQuantity(sellQuantity.subtract(buyQuantity));
            redisUtils.lUpdateIndex(SELL_LEADS,0,JSON.toJSONString(sell));
            transQuantity = buyQuantity;
        }
        Transaction buyTrans = fillTransaction(buyExchange.getUserId(), sell.getUserId(), TransTypeEum.BUY, sellPrice, transQuantity);
        buyTrans.insert();
        return buyTrans;
    }

    private Transaction sellFirst(Exchange sellExchange) {
        BigDecimal sellQuantity = sellExchange.getQuantity();
        if (sellQuantity.compareTo(BigDecimal.ZERO) <= 0)
            return null;
        String buyStr = redisUtils.lGetIndex(BUYING_LEADS, 0);
        if (buyStr == null) {
            insertSell(sellExchange);
            return null;
        }
        Exchange buy = JSON.parseObject(buyStr,Exchange.class);
        BigDecimal buyPrice = buy.getPrice();
        BigDecimal buyQuantity = buy.getQuantity();
        if (sellExchange.getPrice().compareTo(buyPrice) > 0) {
            insertSell(sellExchange);
            return null;
        }
        redisUtils.set(LAST_PRICE,String.format("%.4f",buyPrice));
        BigDecimal transQuantity;
        if (sellQuantity.compareTo(buyQuantity) >= 0) {
            sellExchange.setQuantity(sellQuantity.subtract(buyQuantity));
            redisUtils.leftPop(BUYING_LEADS);
            transQuantity = buyQuantity;

        } else {
            sellExchange.setQuantity(BigDecimal.ZERO);
            buy.setQuantity(buyQuantity.subtract(sellQuantity));
            redisUtils.lUpdateIndex(BUYING_LEADS,0,JSON.toJSONString(buy));
            transQuantity = sellQuantity;
        }
        Transaction sellTrans = fillTransaction(sellExchange.getUserId(), buy.getUserId(), TransTypeEum.SELL, buyPrice, transQuantity);
        sellTrans.insert();
        return sellTrans;
    }

    private void insertBuy(Exchange buyExchange) {
        List<String> strings = redisUtils.lGet(BUYING_LEADS, 0, -1);
        if (strings == null) {
            redisUtils.lSet(BUYING_LEADS,JSON.toJSONString(buyExchange));
            return;
        }
        for (int i=0; i< strings.size();i++) {
            Exchange exchange = JSON.parseObject(strings.get(i), Exchange.class);
            if (exchange.getPrice().compareTo(buyExchange.getPrice()) < 0) {
                strings.add(i, JSON.toJSONString(buyExchange));
                redisUtils.del(BUYING_LEADS);
                redisUtils.lSet(BUYING_LEADS,strings);
                return;
            }
        }
        redisUtils.lSet(BUYING_LEADS,JSON.toJSONString(buyExchange));
    }

    private void insertSell(Exchange sellExchange) {
        List<String> strings = redisUtils.lGet(SELL_LEADS, 0, -1);
        if (strings == null) {
            redisUtils.lSet(SELL_LEADS,JSON.toJSONString(sellExchange));
            return;
        }
        for (int i = 0; i < strings.size(); i++) {
            Exchange exchange = JSON.parseObject(strings.get(i), Exchange.class);
            if (exchange.getPrice().compareTo(sellExchange.getPrice()) > 0) {
                strings.add(i, JSON.toJSONString(sellExchange));
                redisUtils.del(SELL_LEADS);
                redisUtils.lSet(SELL_LEADS,strings);
                return;
            }
        }
        redisUtils.lSet(SELL_LEADS,JSON.toJSONString(sellExchange));
    }

    private Transaction fillTransaction(String fromUserId, String toUserId, TransTypeEum transType, BigDecimal price, BigDecimal quantity) {
        String buUser = transType.equals(TransTypeEum.BUY)?fromUserId:toUserId;
        String sellUser = transType.equals(TransTypeEum.BUY)?toUserId:fromUserId;
        dealBuy(buUser,price,quantity);
        dealSell(sellUser,price,quantity);

        BigDecimal amount = price.multiply(quantity);
        WalletEum fromWalletType = transType.equals(TransTypeEum.BUY) ? WalletEum.USDT : WalletEum.SK;
        WalletEum toWalletType = transType.equals(TransTypeEum.BUY) ? WalletEum.SK : WalletEum.USDT;
        BigDecimal fromAmount = transType.equals(TransTypeEum.BUY) ? feeService.buyerAmount(amount) : quantity;
        BigDecimal toAmount = transType.equals(TransTypeEum.BUY) ? quantity : feeService.sellerAmount(amount);
        Wallet fromWallet = walletService.getWallet(fromUserId,fromWalletType);
        Wallet toWallet = walletService.getWallet(toUserId,toWalletType);
        Transaction transaction = new Transaction();
        if (fromWallet != null){
            transaction.setFromWalletAddress(fromWallet.getAddress());
        }
        if (toWallet != null){
            transaction.setToWalletAddress(toWallet.getAddress());
        }
        transaction.setFromWalletType(fromWalletType.getCode());
        transaction.setToWalletType(toWalletType.getCode());
        transaction.setTransId(UUID.randomUUID().toString());
        transaction.setFromUserId(fromUserId);
        transaction.setToUserId(toUserId);
        transaction.setPrice(price);
        transaction.setQuantity(quantity);
        transaction.setFromAmount(fromAmount);
        transaction.setToAmount(toAmount);
        transaction.setFeeAmount(feeService.fee(amount));
        transaction.setTransType(transType.getCode());
        transaction.setTransStatus(TransStatusEnum.SUCCESS.getCode());
        transaction.setCreateTime(new Date());
        transaction.setModifyTime(new Date());
        return transaction;
    }

    private void dealBuy(String userId, BigDecimal price ,BigDecimal quantity){
        BigDecimal amount = price.multiply(quantity);
        amount = feeService.buyerAmount(amount);
        Wallet subtractWallet = walletService.getWallet(userId, WalletEum.USDT);
        subtractWallet.setBalFreeze(subtractWallet.getBalFreeze().subtract(amount));
        subtractWallet.setBalTotal(subtractWallet.getBalTotal().subtract(amount));
        walletService.updateById(subtractWallet);
        Wallet addWallet = walletService.getWallet(userId, WalletEum.SK);
        addWallet.setBalTotal(addWallet.getBalTotal().add(quantity));
        addWallet.setBalAvail(addWallet.getBalAvail().add(quantity));
        walletService.updateById(addWallet);
    }

    private void dealSell(String userId, BigDecimal price ,BigDecimal quantity){
        BigDecimal amount = price.multiply(quantity);
        amount = feeService.sellerAmount(amount);
        Wallet addWallet = walletService.getWallet(userId, WalletEum.USDT);
        addWallet.setBalTotal(addWallet.getBalTotal().add(amount));
        addWallet.setBalAvail(addWallet.getBalAvail().add(amount));
        walletService.updateById(addWallet);
        Wallet subtractWallet = walletService.getWallet(userId, WalletEum.SK);
        subtractWallet.setBalFreeze(subtractWallet.getBalFreeze().subtract(quantity));
        subtractWallet.setBalTotal(subtractWallet.getBalTotal().subtract(quantity));
        walletService.updateById(subtractWallet);
    }

    private void unfreezeBalance(Wallet wallet, BigDecimal amount){
        wallet.setBalAvail(wallet.getBalAvail().add(amount));
        wallet.setBalFreeze(wallet.getBalFreeze().subtract(amount));
        wallet.setModifyTime(new Date());
        walletService.updateById(wallet);
    }
}
