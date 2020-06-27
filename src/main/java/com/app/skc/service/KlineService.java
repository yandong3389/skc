package com.app.skc.service;

import com.app.skc.enums.KlineEum;
import com.app.skc.model.Kline;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.service.IService;

import java.util.Date;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 
 * @since 2020-02-05
 */
public interface KlineService extends IService<Kline> {
    ResponseResult kline(KlineEum klineEum, Date start, Date end, Integer limit);
    Kline fillKline(KlineEum klineEum, Date start, Date end);
}
