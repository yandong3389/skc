package com.app.skc.common;

import com.app.skc.model.Transaction;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 交易所交易数据中心
 */
public class ExchangeCenter {

    private static ExchangeCenter exchangeCenter;
    /**
     * 当前买入队列
     */
    private LinkedList<Exchange> buyingLeads;

    /**
     * 当前卖出队列
     */
    private LinkedList<Exchange> sellLeads;

    /**
     * 最新成交价
     */
    private BigDecimal lastPrice;

    private ExchangeCenter() {
    }

    /**
     * 获取数据中心单例
     *
     * @return 数据中心
     */
    public static ExchangeCenter getInstance() {
        if (exchangeCenter != null)
            return exchangeCenter;
        exchangeCenter = init();
        return exchangeCenter;
    }

    /**
     * 初始化数据中心
     *
     * @return 数据中心
     */
    private static ExchangeCenter init() {
        ExchangeCenter exchangeCenter = new ExchangeCenter();
        exchangeCenter.buyingLeads = new LinkedList<>();
        exchangeCenter.sellLeads = new LinkedList<>();
        exchangeCenter.lastPrice = null;
        return exchangeCenter;
    }

    public BigDecimal price() {
        return lastPrice;
    }

    public List<Exchange> queryBuy(Integer top) {
        if (CollectionUtils.isEmpty(buyingLeads))
            return null;
        if (top == null || top == 0) {
            return buyingLeads;
        }
        return buyingLeads.subList(0, Math.min(top, buyingLeads.size()));
    }
//
//    private List<Exchange> mergeList(List<Exchange> exchanges,int top){
//        List<Exchange> retList = new ArrayList<>();
//        for (Exchange exchange : exchanges) {
//            exchange
//        }
//    }

    public List<Exchange> querySell(Integer top) {
        if (CollectionUtils.isEmpty(sellLeads))
            return null;
        if (top == null || top == 0) {
            return sellLeads;
        }
        return sellLeads.subList(0, Math.min(top, sellLeads.size()));
    }

    public List<Transaction> buy(String buyUserId, BigDecimal buyPrice, Integer buyQuantity) {
        List<Transaction> transactionList = new ArrayList<>();
        Exchange exchange = new Exchange(buyUserId, buyPrice, buyQuantity);
        Transaction transaction = buyFirst(exchange);
        while (transaction != null) {
            transactionList.add(transaction);
            transaction.insert();
            transaction = buyFirst(exchange);
        }
        return transactionList;
    }

    private Transaction buyFirst(Exchange buyExchange) {
        Integer buyQuantity = buyExchange.getQuantity();
        if (buyQuantity == 0)
            return null;
        if (CollectionUtils.isEmpty(sellLeads)) {
            insertBuy(buyExchange);
            return null;
        }
        Exchange sell = sellLeads.getFirst();
        BigDecimal sellPrice = sell.getPrice();
        Integer sellQuantity = sell.getQuantity();
        if (buyExchange.getPrice().compareTo(sellPrice) < 0) {
            insertBuy(buyExchange);
            return null;
        }
        lastPrice = sellPrice;
        if (sellQuantity <= buyQuantity) {
            buyExchange.setQuantity(buyQuantity - sellQuantity);
            sellLeads.removeFirst();
            return fillTransaction(buyExchange.getUserId(), sell.getUserId(), lastPrice, sellQuantity);
        } else {
            buyExchange.setQuantity(0);
            sell.setQuantity(sellQuantity - buyQuantity);
            return fillTransaction(buyExchange.getUserId(), sell.getUserId(), lastPrice, buyQuantity);
        }
    }

    public List<Transaction> sell(String buyUserId, BigDecimal buyPrice, Integer sellQuantity) {
        List<Transaction> transactionList = new ArrayList<>();
        Exchange exchange = new Exchange(buyUserId, buyPrice, sellQuantity);
        Transaction transaction = sellFirst(exchange);
        while (transaction != null) {
            transactionList.add(transaction);
            transaction.insert();
            transaction = sellFirst(exchange);
        }
        return transactionList;
    }

    private Transaction sellFirst(Exchange sellExchange) {
        Integer sellQuantity = sellExchange.getQuantity();
        if (sellQuantity == 0)
            return null;
        if (CollectionUtils.isEmpty(buyingLeads)) {
            insertSell(sellExchange);
            return null;
        }
        Exchange buy = buyingLeads.getFirst();
        BigDecimal buyPrice = buy.getPrice();
        Integer buyQuantity = buy.getQuantity();
        if (sellExchange.getPrice().compareTo(buyPrice) > 0) {
            insertSell(sellExchange);
            return null;
        }
        lastPrice = buyPrice;
        if (buyQuantity <= sellQuantity) {
            sellExchange.setQuantity(sellQuantity - buyQuantity);
            buyingLeads.removeFirst();
            return fillTransaction(sellExchange.getUserId(), buy.getUserId(), lastPrice, buyQuantity);
        } else {
            sellExchange.setQuantity(0);
            buy.setQuantity(buyQuantity - sellQuantity);
            return fillTransaction(sellExchange.getUserId(), buy.getUserId(), lastPrice, sellQuantity);
        }
    }

    private void insertBuy(Exchange buyExchange) {
        if (CollectionUtils.isEmpty(buyingLeads)) {
            buyingLeads.add(buyExchange);
            return;
        }
        for (int i = 0; i < buyingLeads.size(); i++) {
            if (buyingLeads.get(i).getPrice().compareTo(buyExchange.getPrice()) < 0) {
                buyingLeads.add(i, buyExchange);
                return;
            }
        }
        buyingLeads.addLast(buyExchange);
    }

    private void insertSell(Exchange sellExchange) {
        if (CollectionUtils.isEmpty(sellLeads)) {
            sellLeads.add(sellExchange);
            return;
        }
        for (int i = 0; i < sellLeads.size(); i++) {
            if (sellLeads.get(i).getPrice().compareTo(sellExchange.getPrice()) > 0) {
                sellLeads.add(i, sellExchange);
                return;
            }
        }
        sellLeads.addLast(sellExchange);
    }

    private Transaction fillTransaction(String buyUserId, String sellUserId, BigDecimal price, Integer quantity) {
        Transaction transaction = new Transaction();
        transaction.setTransId(UUID.randomUUID().toString());
        transaction.setFromUserId(buyUserId);
        transaction.setToUserId(sellUserId);
        transaction.setPrice(price);
        transaction.setQuantity(quantity);
        transaction.setCreateTime(new Date());
        transaction.setModifyTime(new Date());
        return transaction;
    }
}
