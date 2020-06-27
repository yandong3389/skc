package com.app.skc.common;

import com.alibaba.fastjson.JSON;
import com.app.skc.enums.TransTypeEum;
import com.app.skc.enums.WalletEum;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.Kline;
import com.app.skc.model.Transaction;
import com.app.skc.model.Wallet;
import com.app.skc.utils.RedisUtils;
import com.app.skc.utils.SkcConstants;
import com.app.skc.utils.SpringContextHolder;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 交易所交易数据中心
 */
@Component
public class ExchangeCenter {

    private static final String BUYING_LEADS = "buyingLeads";
    private static final String SELL_LEADS = "sellLeads";

    @Autowired
    private RedisUtils redisUtils;

    /**
     * 当天K线数据
     * line-每分钟成交价 ,
     * 数组长度2440, 每分钟对应一个下标
     * 00:00对应line[0]
     * 23:59对应line[2339]
     */
    private List<Kline> kline;

    /**
     * 最新成交价
     */
    private BigDecimal lastPrice;

    public BigDecimal price() {
        return lastPrice;
    }

    public List<Exchange> queryBuy() {
        long size = redisUtils.lGetListSize(BUYING_LEADS);
        if (size == 0) {
            return null;
        }
        List<String> strings = redisUtils.lGet(BUYING_LEADS, 0, -1);
        return mergeList(strings);
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
        return mergeList(strings);
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

    public void kline(int minute){
//        if (kline == null || minute == 0){
//            String[] line = new String[2440];
//            Arrays.fill(line,"0.00");
//            line[minute] = String.format("%.2f",lastPrice == null?0:lastPrice.doubleValue());
//            kline = new Kline(DateUtils.truncate(new Date(), Calendar.DATE), line);
//        }else {
//            String[] line = kline.getLine();
//            line[minute] = String.format("%.2f",lastPrice == null?0:lastPrice.doubleValue());
//        }
    }

    public List<Kline> kline(){
        kline = new ArrayList<>();
//        Date now = new Date();
//        Kline k1 = new Kline(DateUtils.addMinutes(now, -30), DateUtils.addMinutes(now, -15));
//        Kline k = new Kline(DateUtils.addMinutes(now, -15), now);
//        kline.add(k1);
//        kline.add(k);
        return kline;
    }

    private boolean cancelBuyEntrust(String order){
        List<String> strings = redisUtils.lGet(BUYING_LEADS, 0, -1);
        if (CollectionUtils.isEmpty(strings))
            return false;
        for (String string : strings) {
            Exchange exchange = JSON.parseObject(string, Exchange.class);
            if (order.equals(exchange.getEntrustOrder())) {
                redisUtils.lRemove(BUYING_LEADS, 1, string);
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
        lastPrice = sellPrice;
        BigDecimal transQuantity;
        if (buyQuantity.compareTo(sellQuantity) > 0) {
            buyExchange.setQuantity(buyQuantity.subtract(sellQuantity));
            redisUtils.leftPop(SELL_LEADS);
            transQuantity = sellQuantity;
        } else {
            buyExchange.setQuantity(BigDecimal.ZERO);
            sell.setQuantity(sellQuantity.subtract(buyQuantity));
            transQuantity = buyQuantity;
        }
        Transaction buyTrans = fillTransaction(buyExchange.getUserId(), sell.getUserId(), TransTypeEum.BUY, lastPrice, transQuantity);
        buyTrans.insert();
        return buyTrans;
//        Transaction sellTrans = fillTransaction(sell.getUserId(), buyExchange.getUserId(), TransTypeEum.SELL, lastPrice, transQuantity);
//        sellTrans.insert();
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
        lastPrice = buyPrice;
        BigDecimal transQuantity;
        if (sellQuantity.compareTo(buyQuantity) > 0) {
            sellExchange.setQuantity(sellQuantity.subtract(buyQuantity));
            redisUtils.leftPop(BUYING_LEADS);
            transQuantity = buyQuantity;

        } else {
            sellExchange.setQuantity(BigDecimal.ZERO);
            buy.setQuantity(buyQuantity.subtract(sellQuantity));
            transQuantity = sellQuantity;
        }
        Transaction sellTrans = fillTransaction(sellExchange.getUserId(), buy.getUserId(), TransTypeEum.SELL, lastPrice, transQuantity);
        sellTrans.insert();
        return sellTrans;
//        Transaction buyTrans = fillTransaction(buy.getUserId(), sellExchange.getUserId(), TransTypeEum.BUY, lastPrice, transQuantity);
//        buyTrans.insert();
    }

    private void insertBuy(Exchange buyExchange) {
        long size = redisUtils.lGetListSize(BUYING_LEADS);
        if (size == 0) {
            redisUtils.lSet(BUYING_LEADS,JSON.toJSONString(buyExchange));
            return;
        }
        for (int i = 0; i < size; i++) {
            String buy = redisUtils.lGetIndex(BUYING_LEADS, i);
            Exchange exchange = JSON.parseObject(buy, Exchange.class);
            if (exchange.getPrice().compareTo(buyExchange.getPrice()) < 0) {
                redisUtils.rightPush(BUYING_LEADS, buy, JSON.toJSONString(buyExchange));
                return;
            }
        }
        redisUtils.lSet(BUYING_LEADS,JSON.toJSONString(buyExchange));
    }

    private void insertSell(Exchange sellExchange) {
        long size = redisUtils.lGetListSize(SELL_LEADS);
        if (size == 0) {
            redisUtils.lSet(SELL_LEADS,JSON.toJSONString(sellExchange));
            return;
        }
        for (int i = 0; i < size; i++) {
            String sell = redisUtils.lGetIndex(SELL_LEADS, i);
            Exchange exchange = JSON.parseObject(sell, Exchange.class);
            if (exchange.getPrice().compareTo(sellExchange.getPrice()) > 0) {
                redisUtils.rightPush(BUYING_LEADS, sell, JSON.toJSONString(sellExchange));
                return;
            }
        }
        redisUtils.lSet(SELL_LEADS,JSON.toJSONString(sellExchange));
    }

    private Transaction fillTransaction(String fromUserId, String toUserId, TransTypeEum transType, BigDecimal price, BigDecimal quantity) {
        String fromWalletType = transType.equals(TransTypeEum.BUY)?WalletEum.USDT.getCode():WalletEum.SK.getCode();
        String toWalletType = transType.equals(TransTypeEum.BUY)?WalletEum.SK.getCode():WalletEum.USDT.getCode();;

        WalletMapper walletMapper = SpringContextHolder.applicationContext.getBean(WalletMapper.class);
        EntityWrapper<Wallet> fromWalletWrapper = new EntityWrapper<>();
        fromWalletWrapper.eq(SkcConstants.USER_ID, fromUserId);
        fromWalletWrapper.eq(SkcConstants.WALLET_TYPE, fromWalletType);
        List<Wallet> fromWallets = walletMapper.selectList(fromWalletWrapper);

        EntityWrapper<Wallet> toWalletWrapper = new EntityWrapper<>();
        toWalletWrapper.eq(SkcConstants.USER_ID, toUserId);
        toWalletWrapper.eq(SkcConstants.WALLET_TYPE, toWalletType);
        List<Wallet> toWallets = walletMapper.selectList(toWalletWrapper);

        Transaction transaction = new Transaction();
        if (!CollectionUtils.isEmpty(fromWallets)){
            transaction.setFromWalletAddress(fromWallets.get(0).getAddress());
        }
        if (!CollectionUtils.isEmpty(toWallets)){
            transaction.setToWalletAddress(toWallets.get(0).getAddress());
        }
        transaction.setFromWalletType(fromWalletType);
        transaction.setFromWalletType(toWalletType);
        transaction.setTransId(UUID.randomUUID().toString());
        transaction.setFromUserId(fromUserId);
        transaction.setToUserId(toUserId);
        transaction.setPrice(price);
        transaction.setQuantity(quantity);
        transaction.setFromAmount(price.multiply(quantity));
        transaction.setTransType(transType.getCode());
        transaction.setCreateTime(new Date());
        transaction.setModifyTime(new Date());
        return transaction;
    }
}
