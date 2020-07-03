package com.app.skc.service.scheduler;

import com.app.skc.enums.KlineEum;
import com.app.skc.model.Kline;
import com.app.skc.service.KlineService;
import com.app.skc.utils.WebUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Calendar;

@Configuration
@EnableScheduling
public class KLineScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KLineScheduler.class);
    private static final String LOG_PREFIX = "[K线] - ";

    @Autowired
    private KlineService klineService;
    @Value("${recharge.local-address}")
    private String localAddress;
    /**
     * 每15分钟执行一次 , 保存上一组K线数据
     */
    @Scheduled(cron = "0 */15 * * * ? ")
    public void kline(){
        String address = WebUtils.getHostAddress();
        if (!address.equals(localAddress)) {
            logger.info("{}开始计算k线数据",LOG_PREFIX);
            Calendar now = Calendar.getInstance();
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            Kline kline = klineService.fillKline(KlineEum.M15,DateUtils.addMinutes(now.getTime() , -15),now.getTime());
            klineService.insert(kline);
            if (now.get(Calendar.MINUTE) == 0){
                Kline dayKline = klineService.fillKline(KlineEum.D1,DateUtils.addDays(now.getTime() , -1),now.getTime());
                klineService.insert(dayKline);
            }
            logger.info("{}计算 K 线结束.", LOG_PREFIX);
        }


    }
}



