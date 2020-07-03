package com.app.skc.service.Impl;

import com.alibaba.fastjson.JSONArray;
import com.app.skc.enums.KlineEum;
import com.app.skc.enums.TransTypeEum;
import com.app.skc.mapper.KlineMapper;
import com.app.skc.model.Kline;
import com.app.skc.model.Transaction;
import com.app.skc.service.KlineService;
import com.app.skc.service.TransactionService;
import com.app.skc.utils.BaseUtils;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.app.skc.utils.SkcConstants.*;

@Service("klineService")
public class KlineServiceImpl extends ServiceImpl<KlineMapper, Kline> implements KlineService {

    @Autowired
    private TransactionService transactionService;

    @Override
    public ResponseResult kline(KlineEum klineEum, Date start, Date end, Integer limit, String type) {
        EntityWrapper<Kline> entityWrapper = new EntityWrapper<>();
        entityWrapper.eq(INTERVAL,klineEum.getCode());
        if(start != null){
            entityWrapper.ge(START_TIME,start);
        }else{
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -3); //得到前一天
            Date date = calendar.getTime();
            entityWrapper.ge(START_TIME,date);
        }
        if(end != null){
            entityWrapper.le(END_TIME,end);
        }else{
            entityWrapper.le(END_TIME,new Date());
        }
        entityWrapper.last(" limit " + limit);
        List<Kline> klineList = this.selectList(entityWrapper);
        if (klineList == null){
            klineList = new ArrayList<>();
        }
        if (end == null || (end.after(new Date()) && klineList.size() < limit)){
            klineList.add(nowKline(klineEum));
            if(klineList.size() > limit)
            {
                klineList.remove(0);
            }

        }
        return ResponseResult.success("成功", packageKline(klineList, type));
    }

    private JSONArray packageKline(List <Kline> klineList, String type) {
        DateFormat format = null;
        //格式化日 k 线
        format = new SimpleDateFormat("yyyyMMdd");
        JSONArray array = new JSONArray();
        for (Kline kline : klineList) {
            JSONArray a = new JSONArray();
            a.add(Long.parseLong(format.format(kline.getStartTime())));
            a.add(new BigDecimal(kline.getPreEndPrice()));
            a.add(new BigDecimal(kline.getStartPrice()));
            a.add(new BigDecimal(kline.getMaxPrice()));
            a.add(new BigDecimal(kline.getMinPrice()));
            a.add(new BigDecimal(kline.getEndPrice()));
            a.add(new BigDecimal(kline.getActiveQuantity()));
            a.add(new BigDecimal(kline.getActiveAmount()));
            if (type.equals(KlineEum.M15.getCode())) {
                a.add(BaseUtils.kLineDate(kline.getStartTime()));
            }
            array.add(a);
        }
        return array;
    }

    private Kline nowKline(KlineEum klineEum){
        Calendar now = Calendar.getInstance();
        Calendar pre = Calendar.getInstance();
        pre.set(Calendar.MILLISECOND,0);
        pre.set(Calendar.SECOND,0);
        if (KlineEum.M15.equals(klineEum)){
            int nowMinute = now.get(Calendar.MINUTE);
            if (nowMinute % 15 == 0){
                return null;
            }
            int i = (nowMinute / 15) * 15;
            pre.set(Calendar.MINUTE,i);
        }else if (KlineEum.D1.equals(klineEum)){
            int nowMinute = now.get(Calendar.MINUTE);
            int nowHour = now.get(Calendar.HOUR_OF_DAY);
            if (nowHour == 0 && nowMinute == 0){
                return null;
            }
            pre.set(Calendar.HOUR_OF_DAY,0);
            pre.set(Calendar.MINUTE,0);
        }
        return fillKline(klineEum, pre.getTime(), now.getTime());
    }

    @Override
    public Kline fillKline(KlineEum klineEum, Date start, Date end) {
        Kline kline = new Kline(start,end,klineEum);
        Kline previousKline = this.previousKline(klineEum);
        if (previousKline != null) {
            kline.setPreEndPrice(previousKline.getEndPrice());
            kline.setStartPrice(previousKline.getEndPrice());
        }
        EntityWrapper<Transaction> entityWrapper = new EntityWrapper<>();
        entityWrapper.ge(CREATE_TIME, start);
        entityWrapper.and().lt(CREATE_TIME,end);
        entityWrapper.and().in(TRANS_TYPE,new String[]{TransTypeEum.BUY.getCode(),TransTypeEum.SELL.getCode()});
        List<Transaction> transactionList = transactionService.selectList(entityWrapper);
        String previousPirc = previousKline.getEndPrice();
        if (CollectionUtils.isEmpty(transactionList)){
            kline.setMaxPrice(previousPirc);
            kline.setMinPrice(previousPirc);
            kline.setEndPrice(previousPirc);
            return kline;
        }
        kline.setTransNum(transactionList.size());
        BigDecimal minPrice = BigDecimal.valueOf(Double.MAX_VALUE);
        BigDecimal maxPrice = BigDecimal.ZERO;
        BigDecimal endPrice = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal activeQuantity = BigDecimal.ZERO;
        BigDecimal activeAmount = BigDecimal.ZERO;
        for (Transaction transaction : transactionList) {
            endPrice  = transaction.getPrice();
            if (minPrice.compareTo(transaction.getPrice()) > 0)
                minPrice = transaction.getPrice();
            if (maxPrice.compareTo(transaction.getPrice()) < 0)
                maxPrice = transaction.getPrice();
            totalQuantity = totalQuantity.add(transaction.getQuantity());
            totalAmount = totalAmount.add(transaction.getFromAmount());
            if (TransTypeEum.BUY.getCode().equals(transaction.getTransType())) {
                activeQuantity = activeQuantity.add(transaction.getQuantity());
                activeAmount = activeAmount.add(transaction.getFromAmount());
            }
        }
        kline.setEndPrice(String.format("%.4f",endPrice));
        kline.setMaxPrice(String.format("%.4f",maxPrice));
        kline.setMinPrice(String.format("%.4f",minPrice));
        kline.setTotalQuantity(String.format("%.4f",totalQuantity));
        kline.setTotalAmount(String.format("%.4f",totalAmount));
        kline.setActiveAmount(String.format("%.4f",activeAmount));
        kline.setActiveQuantity(String.format("%.4f",activeQuantity));
        kline.setCreateTime(new Date());
        kline.setModifyTime(new Date());
        return kline;
    }

    private Kline previousKline(KlineEum klineEum){
        EntityWrapper<Kline> entityWrapper = new EntityWrapper<>();
        entityWrapper.eq(INTERVAL, klineEum.getCode());
        entityWrapper.orderBy(END_TIME,false);
        entityWrapper.last(" limit 1");
        List<Kline> klineList = this.selectList(entityWrapper);
        if (CollectionUtils.isEmpty(klineList)){
            return null;
        }
        return klineList.get(0);
    }

}
