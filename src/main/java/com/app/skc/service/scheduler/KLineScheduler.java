package com.app.skc.service.scheduler;

import com.app.skc.common.ExchangeCenter;
import com.app.skc.mapper.TransactionMapper;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.Kline;
import com.app.skc.model.Transaction;
import com.app.skc.service.system.ConfigService;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.app.skc.utils.SkcConstants.CREATE_TIME;

//@Configuration
//@EnableScheduling
public class KLineScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KLineScheduler.class);
    private static final String LOG_PREFIX = "[K线] - ";

    @Autowired
    private TransactionMapper transactionMapper;
    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private ConfigService configService;
    @Autowired
    private ExchangeCenter exchangeCenter;

    /**
     * 每15分钟执行一次 , 保存上一组K线数据
     */
//    @Scheduled(cron = "0 */15 * * * ? ")
    public void kline(){
        logger.info("{}定时任务开始...", LOG_PREFIX);
        Date now = new Date();
        fillKline(DateUtils.addMinutes(now , -15),now);
        logger.info("{}定时任务全部结束.", LOG_PREFIX);

    }

    private Kline fillKline(Date start, Date end) {
        Kline kline = new Kline(start,end);

        EntityWrapper<Transaction> entityWrapper = new EntityWrapper<>();
        entityWrapper.ge(CREATE_TIME, start);
        entityWrapper.and().lt(CREATE_TIME,end);
        List<Transaction> transactionList = transactionMapper.selectList(entityWrapper);
        if (CollectionUtils.isEmpty(transactionList)){
            return kline;
        }
        kline.setTransNum(transactionList.size());

        for (Transaction transaction : transactionList) {
//            transaction.
        }
        return kline;
    }

    private int getMinuteOfDay(){
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        return (hour * 60 ) + min;
    }
}



