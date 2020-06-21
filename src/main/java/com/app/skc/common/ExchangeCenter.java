package com.app.skc.common;

import com.app.skc.enums.TransTypeEum;
import com.app.skc.enums.WalletEum;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.Transaction;
import com.app.skc.model.Wallet;
import com.app.skc.utils.SkcConstants;
import com.app.skc.utils.SpringContextHolder;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.apache.commons.lang3.time.DateUtils;
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
     * 当天K线数据
     * line-每分钟成交价 ,
     * 数组长度2440, 每分钟对应一个下标
     * 00:00对应line[0]
     * 23:59对应line[2339]
     */
    private Kline kline;

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

    public List<Exchange> queryBuy() {
        if (CollectionUtils.isEmpty(buyingLeads))
            return null;
        return mergeList(buyingLeads);
    }

    private List<Exchange> mergeList(List<Exchange> exchanges) {
        List<Exchange> retList = new ArrayList<>();
        for (Exchange exchange : exchanges) {
            Exchange ex = new Exchange();
            ex.setQuantity(exchange.getQuantity());
            ex.setPrice(exchange.getPrice());
            if (retList.size() == 0) {
                retList.add(ex);
                continue;
            }
            Exchange lastExchange = retList.get(retList.size() - 1);
            if (lastExchange.getPrice().equals(ex.getPrice())) {
                lastExchange.setQuantity(lastExchange.getQuantity() + ex.getQuantity());
            } else {
                retList.add(ex);
            }
        }
        return retList;
    }

    public List<Exchange> querySell() {
        if (CollectionUtils.isEmpty(sellLeads))
            return null;
        return mergeList(sellLeads);
    }

    public List<Transaction> buy(String buyUserId, BigDecimal buyPrice, Integer buyQuantity) {
        List<Transaction> transactionList = new ArrayList<>();
        Exchange exchange = new Exchange(buyUserId, TransTypeEum.BUY, buyPrice, buyQuantity);
        Transaction transaction = buyFirst(exchange);
        while (transaction != null) {
            transactionList.add(transaction);
            transaction = buyFirst(exchange);
        }
        return transactionList;
    }

    public List<Transaction> sell(String buyUserId, BigDecimal buyPrice, Integer sellQuantity) {
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
        for (Exchange buyingLead : buyingLeads) {
            if (userId.equals(buyingLead.getUserId())) {
                retList.add(buyingLead);
            }
        }
        for (Exchange buyingLead : sellLeads) {
            if (userId.equals(buyingLead.getUserId())) {
                retList.add(buyingLead);
            }
        }
        return retList;
    }

    public boolean cancelEntrust(String userId,String entrustOrder){
        if (userId == null || entrustOrder == null)
            return false;
        return cancelEntrust(buyingLeads, entrustOrder) || cancelEntrust(sellLeads, entrustOrder);
    }

    public void kline(int minute){
        if (kline == null || minute == 0){
            String[] line = new String[2440];
            Arrays.fill(line,"0.00");
            line[minute] = String.format("%.2f",lastPrice == null?0:lastPrice.doubleValue());
            kline = new Kline(DateUtils.truncate(new Date(), Calendar.DATE), line);
        }else {
            String[] line = kline.getLine();
            line[minute] = String.format("%.2f",lastPrice == null?0:lastPrice.doubleValue());
        }
    }

    public Kline kline(){
        return kline;
    }

    private boolean cancelEntrust(List<Exchange> exchanges , String order){
        Iterator<Exchange> iterator = exchanges.iterator();
        while (iterator.hasNext()) {
            if (order.equals(iterator.next().getEntrustOrder())) {
                iterator.remove();
                return true;
            }
        }
        return false;
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
        int transQuantity;
        if (sellQuantity <= buyQuantity) {
            buyExchange.setQuantity(buyQuantity - sellQuantity);
            sellLeads.removeFirst();
            transQuantity = sellQuantity;
        } else {
            buyExchange.setQuantity(0);
            sell.setQuantity(sellQuantity - buyQuantity);
            transQuantity = buyQuantity;
        }
        Transaction buyTrans = fillTransaction(buyExchange.getUserId(), sell.getUserId(), TransTypeEum.BUY, lastPrice, transQuantity);
        Transaction sellTrans = fillTransaction(sell.getUserId(), buyExchange.getUserId(), TransTypeEum.SELL, lastPrice, transQuantity);
        buyTrans.insert();
        sellTrans.insert();
        return buyTrans;
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
        int transQuantity;
        if (buyQuantity <= sellQuantity) {
            sellExchange.setQuantity(sellQuantity - buyQuantity);
            buyingLeads.removeFirst();
            transQuantity = buyQuantity;

        } else {
            sellExchange.setQuantity(0);
            buy.setQuantity(buyQuantity - sellQuantity);
            transQuantity = sellQuantity;
        }
        Transaction sellTrans = fillTransaction(sellExchange.getUserId(), buy.getUserId(), TransTypeEum.SELL, lastPrice, transQuantity);
        Transaction buyTrans = fillTransaction(buy.getUserId(), sellExchange.getUserId(), TransTypeEum.BUY, lastPrice, transQuantity);
        sellTrans.insert();
        buyTrans.insert();
        return sellTrans;
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

    private Transaction fillTransaction(String fromUserId, String toUserId, TransTypeEum transType, BigDecimal price, Integer quantity) {
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
        transaction.setTransType(transType.getCode());
        transaction.setCreateTime(new Date());
        transaction.setModifyTime(new Date());
        return transaction;
    }
}
