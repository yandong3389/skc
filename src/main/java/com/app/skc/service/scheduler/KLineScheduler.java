package com.app.skc.service.scheduler;

import com.app.skc.common.ExchangeCenter;
import com.app.skc.mapper.TransactionMapper;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.service.system.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Calendar;
import java.util.Date;

@Configuration
@EnableScheduling
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
     * 每分钟执行一次 , 保持当前最新成交价
     */
    @Scheduled(cron = "0 * * * * ?")
    public void kline(){
        logger.info("{}定时任务开始...", LOG_PREFIX);
        int minuteOfDay = getMinuteOfDay();
        logger.info("{}当前时间-{} , 分钟数-{}",LOG_PREFIX,new Date(),minuteOfDay);
        exchangeCenter.kline(minuteOfDay);
        logger.info("{}定时任务全部结束.", LOG_PREFIX);

    }

    private int getMinuteOfDay(){
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        return (hour * 60 ) + min;
    }
}



