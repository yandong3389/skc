package com.app.skc.service.system;

import com.app.skc.model.system.Config;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.service.IService;

import java.util.Map;

/**
 * <p>
 * 参数配置表 服务类
 * </p>
 *
 * @author 
 * @since 2020-02-09
 */
public interface ConfigService extends IService<Config> {

    Config getByKey(String key);

    ResponseResult getConfig(Page page, Map<String,Object> params);
}
