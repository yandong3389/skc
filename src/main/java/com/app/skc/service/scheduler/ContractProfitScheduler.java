package com.app.skc.service.scheduler;

import com.alibaba.fastjson.JSONObject;
import com.app.skc.exception.BusinessException;
import com.app.skc.model.UserShareVO;
import com.app.skc.service.ContractProfitService;
import com.app.skc.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableScheduling
public class ContractProfitScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ContractProfitScheduler.class);
    private static final String LOG_PREFIX = "[合约收益释放] - ";
    @Value("#{'${contract.job-address:172.19.16.12}'}")
    private String jobAddress;
    // 用户伞下有效用户列表API
    @Value("#{'${contract.api-tree-users:http://www.skgame.top/v1/Trade/Get_TreeUsers}'}")
    private String API_TREE_USERS;
    @Autowired
    private ContractProfitService contractProfitService;

    @Scheduled(cron = "0 0/2 * * * ?")
    public void releaseProfit() {
        logger.info("{}job开始...", LOG_PREFIX);
        long startTime = System.currentTimeMillis();
        // 1、过滤非job执行地址
        String curIpAdd = WebUtils.getHostAddress();
        if (!curIpAdd.equals(jobAddress)) {
            logger.info("{}非job执行地址[{}], 指定地址:[{}].", LOG_PREFIX, curIpAdd, jobAddress);
            return;
        }
        // 2、获取分享合约用户树列表
        List<UserShareVO> userShareList = queryUserTreeList();

        if (CollectionUtils.isEmpty(userShareList)) {
            logger.info("{}分享合约用户树列表获取结果集为空，job结束.", LOG_PREFIX);
            return;
        }
        for (UserShareVO userShare : userShareList) {
            try {
                contractProfitService.userTreeTrans(userShare);
            } catch (BusinessException e) {
                logger.error("{}收益分享树计算失败，根节点用户Id为[{}].", LOG_PREFIX, userShare.getId(), e);
            }
        }
        logger.info("{}job结束，总执行耗时[{}]ms.", LOG_PREFIX, System.currentTimeMillis() - startTime);
    }

    private List<UserShareVO> queryUserTreeList() {
        RestTemplate restTemplate = new RestTemplate();
        JSONObject jsonObj = restTemplate.getForObject(API_TREE_USERS, JSONObject.class);
        if (jsonObj == null) {
            return new ArrayList<>();
        }
        JSONObject resultObject = jsonObj.getJSONObject("data");
        UserShareVO userShareVO = JSONObject.parseObject(resultObject.toJSONString(), UserShareVO.class);
        return userShareVO.getSubUsers();
    }

}
