package com.app.skc.service.scheduler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.app.skc.mapper.TransactionMapper;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.UserShareVO;
import com.app.skc.service.ContractProfitService;
import com.app.skc.service.system.ConfigService;
import com.app.skc.utils.HttpClientUtil;
import com.app.skc.utils.WebUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableScheduling
public class ContractProfitScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ContractProfitScheduler.class);
    private static final String LOG_PREFIX = "[合约收益释放] - ";
    @Value("#{'${contract.job-address:172.19.16.11}'}")
    private String jobAddress;
    // 用户伞下有效用户列表API
    @Value("#{'${contract.api-tree-users:http://www.skgame.top/v1/Trade/Get_TreeUsers}'}")
    private String API_TREE_USERS;
    // 获取直推有效用户列表API
    @Value("#{'${contract.api-direct-users:http://www.skgame.top/v1/Trade/Get_DirectUsers}'}")
    private String API_DIRECT_USERS;
    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private ConfigService configService;
    @Autowired
    private TransactionMapper transactionMapper;
    @Autowired
    private ContractProfitService contractProfitService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void releaseProfit() {
        logger.info("{}job开始...", LOG_PREFIX);
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
            contractProfitService.userTreeTrans(userShare);
        }
        logger.info("{}job结束.", LOG_PREFIX);
    }

    private List<UserShareVO> queryUserTreeList() {
        List<UserShareVO> treeUserList = new ArrayList<>();
        String treeUsersRes = HttpClientUtil.sendGet(API_TREE_USERS);
        if (StringUtils.isBlank(treeUsersRes)) {
            return treeUserList;
        }
        JSONObject jsonObj = JSONObject.parseObject(treeUsersRes);
        if (jsonObj == null) {
            return treeUserList;
        }
        JSONArray tuDataJsonArray = jsonObj.getJSONArray("data");
        if (tuDataJsonArray != null && tuDataJsonArray.size() > 0) {
            for (int i = 0; i < tuDataJsonArray.size(); i++) {
                JSONObject resultObject = tuDataJsonArray.getJSONObject(i);
                UserShareVO userShareVO = JSONObject.parseObject(resultObject.toJSONString(), UserShareVO.class);
                treeUserList.add(userShareVO);
            }
        }
        return treeUserList;

    }

}
