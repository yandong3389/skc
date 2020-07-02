package com.app.skc.service.Impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.app.skc.common.ExchangeCenter;
import com.app.skc.enums.SysConfigEum;
import com.app.skc.enums.TransStatusEnum;
import com.app.skc.enums.TransTypeEum;
import com.app.skc.enums.UserGradeEnum;
import com.app.skc.exception.BusinessException;
import com.app.skc.mapper.IncomeMapper;
import com.app.skc.mapper.TransactionMapper;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.Income;
import com.app.skc.model.Transaction;
import com.app.skc.model.UserShareVO;
import com.app.skc.model.Wallet;
import com.app.skc.model.system.Config;
import com.app.skc.service.ContractProfitService;
import com.app.skc.service.system.ConfigService;
import com.app.skc.utils.BaseUtils;
import com.app.skc.utils.date.DateUtil;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service("contractProfitService")
public class ContractProfitServiceImpl extends ServiceImpl<IncomeMapper, Income> implements ContractProfitService {
    private static final Logger logger = LoggerFactory.getLogger(ContractProfitServiceImpl.class);
    private static final String LOG_PREFIX = "[合约收益释放] - ";
    private static final String PARAM_USER_ID = "userId";
    @Autowired
    private IncomeMapper incomeMapper;
    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private TransactionMapper transMapper;
    @Autowired
    private ExchangeCenter exchangeCenter;
    @Autowired
    private ConfigService configService;
    // 用户伞下有效用户列表API
    @Value("#{'${contract.api-tree-users:http://www.skgame.top/v1/Trade/Get_TreeUsers}'}")
    private String API_TREE_USERS;
    // 用户等级列表API
    @Value("#{'${contract.api-grade-list:http://www.skgame.top/v1/Trade/Get_Grade_List}'}")
    private String API_GRADE_LIST;
    // 修改用户有效性API
    @Value("#{'${contract.api-change-user-status:http://www.skgame.top/v1/Trade/ChangeUserStatus}'}")
    private String API_CHANGE_USER_STATUS;

    // 用户等级ID、代码映射map
    private static Map<String, String> userGradeCodeMap = new HashMap<>();

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void userTreeTrans(UserShareVO userShare) {
        // 1、初始数据准备
        Map<String, Income> incomeMap = new HashMap<>();
        Map<String, UserShareVO> allShareMap = new HashMap<>();
        Map<Integer, List<UserShareVO>> allLevelMap = new HashMap<>();
        fulfillLevelMap(allLevelMap, userShare, 1);
        Map<String, Transaction> allShareTrans = new HashMap<>();
        fulfillAllMap(allShareMap, userShare);
        fulfillAllShareTransMap(allShareTrans, userShare);
        // 2、用户树遍历处理：静态 + 分享 + 社区
        for (int i = allLevelMap.size(); i > 0; i--) {
            List<UserShareVO> levelShareList = allLevelMap.get(i);
            for (UserShareVO user : levelShareList) {
                Transaction contractTrans = allShareTrans.get(user.getId());
                if (contractTrans == null) {
                    continue;
                }
                List<UserShareVO> allSubUserList = new ArrayList<>();
                fulfillAllSubUserList(allSubUserList, user);
                allSubUserList.remove(user);
                Wallet contractWallet = getContractWallet(contractTrans);
                if (contractWallet == null) {
                    continue;
                }
                Income contractIncome = getContractIncome(user);
                if (contractIncome != null) {
                    incomeMap.put(user.getId(), contractIncome);
                    continue;
                } else {
                    String contrInId = BaseUtils.get64UUID();
                    contractIncome = new Income();
                    contractIncome.setId(contrInId);
                    contractIncome.setUserId(user.getId());
                    contractIncome.setUserName(user.getName());
                    contractIncome.setContractId(contractTrans.getTransId());
                    incomeMap.put(user.getId(), contractIncome);
                }
                // 2.1 静态收益
                if (dealStaticProfit(contractTrans, contractWallet, contractIncome)) {
                    continue;
                }
                // 2.2 分享收益
                List<UserShareVO> directSubUserList = dealShareProfit(incomeMap, allShareTrans, user, contractWallet, allSubUserList);
                if (directSubUserList == null) {
                    saveProfitInfo(contractTrans, contractWallet, contractIncome, contractIncome.getStaticIn(), BigDecimal.ZERO, BigDecimal.ZERO, false);
                    continue;
                }
                // 2.3 社区收益
                dealMngProfit(incomeMap, allShareTrans, user, contractTrans, allSubUserList, contractWallet, contractIncome, directSubUserList);
            }
        }
        // 3、所有释放收益转换为sk入库
        for (Income eachIncome : incomeMap.values()) {
            eachIncome.setStaticIn(eachIncome.getStaticIn().multiply(getCurExRate()));
            eachIncome.setShareIn(eachIncome.getShareIn().multiply(getCurExRate()));
            eachIncome.setManageIn(eachIncome.getManageIn().multiply(getCurExRate()));
            eachIncome.setTotal(eachIncome.getTotal().multiply(getCurExRate()));
            incomeMapper.insert(eachIncome);
        }
        logger.info("{}用户合约分享树收益计算完毕，树总用户数[{}], 树高[{}]", LOG_PREFIX, allShareMap.size(), allLevelMap.size());
    }

    @Override
    public Map<String, List<String>> calcUserGrade(String userId) throws BusinessException {
        Map<String, List<String>> gradeUserListMap = new HashMap<>();
        // 1、查询所有用户树
        RestTemplate restTemplate = new RestTemplate();
        JSONObject jsonObj = restTemplate.getForObject(API_TREE_USERS, JSONObject.class);
        if (jsonObj == null) {
            logger.error("【用户等级计算】 - 处理失败，用户分享树API调用失败！");
            throw new BusinessException("用户分享树API调用失败");
        }
        JSONObject resultObject = jsonObj.getJSONObject("data");
        UserShareVO userShare = JSONObject.parseObject(resultObject.toJSONString(), UserShareVO.class);
        // 2、初始数据准备
        Map<String, UserShareVO> allUserMap = new HashMap<>();
        fulfillAllMap(allUserMap, userShare);
        UserShareVO curUserVO = allUserMap.get(userId);
        Map<Integer, List<UserShareVO>> allLevelMap = new HashMap<>();
        Map<String, Transaction> allShareTrans = new HashMap<>();
        fulfillLevelMap(allLevelMap, userShare, 1);
        fulfillAllShareTransMap(allShareTrans, userShare);
        // TODO
        return null;
    }

    /**
     * 社区收益处理
     *
     * @param incomeMap
     * @param allShareTrans
     * @param user
     * @param contractTrans
     * @param allSubUserList
     * @param contractWallet
     * @param contractIncome
     * @param directSubUserList
     */
    private void dealMngProfit(Map<String, Income> incomeMap, Map<String, Transaction> allShareTrans, UserShareVO user, Transaction contractTrans, List<UserShareVO> allSubUserList, Wallet contractWallet, Income contractIncome, List<UserShareVO> directSubUserList) {
        int directShare = directSubUserList.size();
        BigDecimal totalContract = allShareTrans.get(user.getId()).getPrice();
        for (UserShareVO subUser : allSubUserList) {
            totalContract = totalContract.add(allShareTrans.get(subUser.getId()).getPrice());
        }
        if (directShare >= 10 && totalContract.compareTo(new BigDecimal(80000)) >= 0) {
            BigDecimal mngRate = getMngRate(directSubUserList, totalContract);
            BigDecimal mngProfit = BigDecimal.ZERO;
            for (UserShareVO eachSubUser : allSubUserList) {
                BigDecimal userStaticIn = incomeMap.get(eachSubUser.getId()).getStaticIn();
                if (allShareTrans.get(user.getId()).getPrice().compareTo(allShareTrans.get(eachSubUser.getId()).getPrice()) < 0) {
                    mngProfit = mngProfit.add(userStaticIn.multiply(mngRate).multiply(allShareTrans.get(user.getId()).getPrice().divide(allShareTrans.get(eachSubUser.getId()).getPrice(), RoundingMode.DOWN)));
                } else {
                    mngProfit = mngProfit.add(userStaticIn.multiply(mngRate));
                }
            }
            if (mngProfit.add(contractIncome.getStaticIn()).add(contractIncome.getShareIn()).compareTo(contractWallet.getSurplusContract()) < 0) {
                contractIncome.setManageIn(mngProfit);
                saveProfitInfo(contractTrans, contractWallet, contractIncome, contractIncome.getStaticIn(), contractIncome.getShareIn(), contractIncome.getManageIn(), false);
            } else {
                saveProfitInfo(contractTrans, contractWallet, contractIncome, contractIncome.getStaticIn(), contractIncome.getShareIn(), contractWallet.getSurplusContract().subtract(contractIncome.getStaticIn()).subtract(contractIncome.getShareIn()), true);
            }
        } else {
            saveProfitInfo(contractTrans, contractWallet, contractIncome, contractIncome.getStaticIn(), contractIncome.getShareIn(), BigDecimal.ZERO, false);
        }
    }

    /**
     * 获取社区收益比例
     *
     * @param directSubUserList
     * @param totalContract
     * @return
     * @throws BusinessException
     */
    private BigDecimal getMngRate(List<UserShareVO> directSubUserList, BigDecimal totalContract) {
        int directShare = directSubUserList.size();
        Config mngRateConfig;
        int bronzeCommCnt = 0;
        int goldCommCnt = 0;
        int diamondCommCnt = 0;
        int kingCommCnt = 0;
        for (UserShareVO userShareVO : directSubUserList) {
            Map<String, List<UserShareVO>> eachCommGradeMap = new HashMap<>();
            fulfillAllGradeMap(eachCommGradeMap, userShareVO);
            if (!CollectionUtils.isEmpty(eachCommGradeMap.get(UserGradeEnum.KING.getCode()))) {
                ++kingCommCnt;
                continue;
            }
            if (!CollectionUtils.isEmpty(eachCommGradeMap.get(UserGradeEnum.DIAMOND.getCode()))) {
                ++diamondCommCnt;
                continue;
            }
            if (!CollectionUtils.isEmpty(eachCommGradeMap.get(UserGradeEnum.GOLD.getCode()))) {
                ++goldCommCnt;
                continue;
            }
            if (!CollectionUtils.isEmpty(eachCommGradeMap.get(UserGradeEnum.BRONZE.getCode()))) {
                ++bronzeCommCnt;
            }
        }
        if (directShare >= 20 && kingCommCnt >= 3 && totalContract.compareTo(new BigDecimal(10000000)) >= 0) {
            mngRateConfig = configService.getByKey(SysConfigEum.CONTR_MNG_RATE_GOD.getCode());
        } else if (directShare >= 15 && diamondCommCnt >= 3 && totalContract.compareTo(new BigDecimal(3000000)) >= 0) {
            mngRateConfig = configService.getByKey(SysConfigEum.CONTR_MNG_RATE_KING.getCode());
        } else if (directShare >= 15 && goldCommCnt >= 3 && totalContract.compareTo(new BigDecimal(800000)) >= 0) {
            mngRateConfig = configService.getByKey(SysConfigEum.CONTR_MNG_RATE_DIAMOND.getCode());
        } else if (directShare >= 15 && bronzeCommCnt >= 2 && totalContract.compareTo(new BigDecimal(200000)) >= 0) {
            mngRateConfig = configService.getByKey(SysConfigEum.CONTR_MNG_RATE_GOLD.getCode());
        } else {
            mngRateConfig = configService.getByKey(SysConfigEum.CONTR_MNG_RATE_BRONZE.getCode());
        }
        return new BigDecimal(mngRateConfig.getConfigValue());
    }

    /**
     * 分享收益处理
     *
     * @param incomeMap
     * @param allShareTrans
     * @param user
     * @param contractWallet
     * @param allSubUserList
     * @return
     */
    private List<UserShareVO> dealShareProfit(Map<String, Income> incomeMap, Map<String, Transaction> allShareTrans, UserShareVO user, Wallet contractWallet, List<UserShareVO> allSubUserList) {
        List<UserShareVO> directSubUserList = user.getSubUsers();
        if (CollectionUtils.isEmpty(directSubUserList)) {
            return null;
        } else {
            Transaction contractTrans = allShareTrans.get(user.getId());
            Income contractIncome = incomeMap.get(user.getId());
            Map<Integer, List<UserShareVO>> subLevelMap = new HashMap<>();
            for (UserShareVO eachUserVO : allSubUserList) {
                List<UserShareVO> subLevelUserList = subLevelMap.get(Integer.parseInt(eachUserVO.getLevel()));
                if (CollectionUtils.isEmpty(subLevelUserList)) {
                    subLevelUserList = new ArrayList<>();
                    subLevelMap.put(Integer.parseInt(eachUserVO.getLevel()), subLevelUserList);
                }
                subLevelUserList.add(eachUserVO);
            }
            BigDecimal shareProfit = BigDecimal.ZERO;
            int directShareNum = directSubUserList.size();
            int curUserLevel = Integer.parseInt(user.getLevel());
            for (Integer subUserLevel : subLevelMap.keySet()) {
                int generation = subUserLevel - curUserLevel;
                // 直推分享人数小于分享代数时跳过
                if (directShareNum < generation) {
                    continue;
                }
                Config shareRateConfig;
                switch (generation) {
                    case 1:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_G1.getCode());
                        break;
                    case 2:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_G2.getCode());
                        break;
                    case 3:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_G3.getCode());
                        break;
                    case 4:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_G4.getCode());
                        break;
                    case 5:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_G5.getCode());
                        break;
                    default:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_GX.getCode());
                }
                BigDecimal shareRate = new BigDecimal(shareRateConfig.getConfigValue());
                List<UserShareVO> curLevelShareList = subLevelMap.get(subUserLevel);
                for (UserShareVO curLevelUser : curLevelShareList) {
                    Income income = incomeMap.get(curLevelUser.getId());
                    if (income != null) {
                        BigDecimal subShareProfit;
                        if (contractTrans.getPrice().compareTo(allShareTrans.get(curLevelUser.getId()).getPrice()) < 0) {
                            subShareProfit = income.getStaticIn().multiply(shareRate).multiply(contractTrans.getPrice().divide(allShareTrans.get(curLevelUser.getId()).getPrice(), RoundingMode.DOWN));
                        } else {
                            subShareProfit = income.getStaticIn().multiply(shareRate);
                        }
                        shareProfit = shareProfit.add(subShareProfit);
                    }
                }
            }
            if (shareProfit.add(contractIncome.getStaticIn()).compareTo(contractWallet.getSurplusContract()) < 0) {
                contractIncome.setShareIn(shareProfit);
            } else {
                saveProfitInfo(contractTrans, contractWallet, contractIncome, contractIncome.getStaticIn(), contractWallet.getSurplusContract().subtract(contractIncome.getStaticIn()), BigDecimal.ZERO, true);
                return null;
            }
        }
        return directSubUserList;
    }

    /**
     * 静态收益处理
     *
     * @param contractTrans
     * @param contractWallet
     * @param contractIncome
     * @return
     */
    private boolean dealStaticProfit(Transaction contractTrans, Wallet contractWallet, Income contractIncome) {
        Config staticRateConfig;
        if (contractWallet.getComsumedContract().compareTo(contractTrans.getPrice()) < 0) {
            staticRateConfig = configService.getByKey(SysConfigEum.CONTR_STATIC_RATE.getCode());
        } else {
            staticRateConfig = configService.getByKey(SysConfigEum.CONTR_STATIC_RATE_DISCOUNT.getCode());
        }
        BigDecimal staticRate = new BigDecimal(staticRateConfig.getConfigValue());
        BigDecimal staticProfit = contractTrans.getPrice().multiply(staticRate);
        if (staticProfit.compareTo(contractWallet.getSurplusContract()) < 0) {
            contractIncome.setStaticIn(staticProfit);
        } else {
            saveProfitInfo(contractTrans, contractWallet, contractIncome, contractWallet.getSurplusContract(), BigDecimal.ZERO, BigDecimal.ZERO, true);
            return true;
        }
        return false;
    }

    /**
     * 保存用户收益信息
     *
     * @param contractTrans
     * @param contractWallet
     * @param contractIncome
     * @param staticProfit
     * @param shareProfit
     * @param mngProfit
     * @param isOutofProfit
     */
    private void saveProfitInfo(Transaction contractTrans, Wallet contractWallet, Income contractIncome, BigDecimal staticProfit, BigDecimal shareProfit, BigDecimal mngProfit, boolean isOutofProfit) {
        if (isOutofProfit) {
            // 合约交易记录更新
            contractTrans.setTransStatus(TransStatusEnum.UNEFFECT.getCode());
            contractTrans.setModifyTime(new Date());
            transMapper.updateById(contractTrans);
            // 更改用户为无效用户
            inactiveUser(contractIncome.getUserId());
        }
        // 钱包更新
        BigDecimal totalProfit = staticProfit.add(shareProfit).add(mngProfit);
        BigDecimal balChange = totalProfit.multiply(getCurExRate());
        contractWallet.setComsumedContract(contractWallet.getComsumedContract().add(totalProfit));
        contractWallet.setSurplusContract(contractWallet.getSurplusContract().subtract(totalProfit));
        contractWallet.setBalTotal(contractWallet.getBalTotal().add(balChange));
        contractWallet.setBalAvail(contractWallet.getBalAvail().add(balChange));
        contractWallet.setModifyTime(new Date());
        walletMapper.updateById(contractWallet);
        // 当日收益记录插入
        contractIncome.setStaticIn(staticProfit);
        contractIncome.setShareIn(shareProfit);
        contractIncome.setManageIn(mngProfit);
        contractIncome.setTotal(totalProfit);
        contractIncome.setDateAcct(DateUtil.getCurDate());
        contractIncome.setCreateTime(new Date());
        // 此处只做记录数据模型更新，稍后统一转为sk收益入库
//        incomeMapper.insert(contractIncome);
    }

    /**
     * 获取当前合约用户的收益记录
     *
     * @param userShareVO
     * @return
     */
    private Income getContractIncome(UserShareVO userShareVO) {
        EntityWrapper<Income> incomeWrapper = new EntityWrapper<>();
        String dateAcct = DateUtil.getCurDate();
        incomeWrapper.eq(PARAM_USER_ID, userShareVO.getId());
        incomeWrapper.eq("dateAcct", dateAcct);
        List<Income> incomeList = incomeMapper.selectList(incomeWrapper);
        if (CollectionUtils.isEmpty(incomeList)) {
            return null;
        } else {
            logger.info("{}当前用户[{}]合约记录[{}]收益已释放，跳过收益计算。", LOG_PREFIX, userShareVO.getId(), dateAcct);
            return incomeList.get(0);
        }
    }

    /**
     * 获取购买合约钱包
     *
     * @param trans
     * @return
     */
    private Wallet getContractWallet(Transaction trans) {
        EntityWrapper<Wallet> walletWrapper = new EntityWrapper<>();
        walletWrapper.eq("address", trans.getFromWalletAddress());
        if (StringUtils.isNotBlank(trans.getFromWalletType())) {
            walletWrapper.eq("wallet_type", trans.getFromWalletType());
        }
        List<Wallet> walletList = walletMapper.selectList(walletWrapper);
        if (CollectionUtils.isEmpty(walletList)) {
            logger.info("{}未找到合约记录的关联钱包[{}]，跳过收益计算。", LOG_PREFIX, trans.getFromWalletAddress());
            return null;
        } else {
            return walletList.get(0);
        }
    }

    /**
     * 获取有效的合约购买记录
     *
     * @param userShareVO
     * @return
     */
    private Transaction getContractTrans(UserShareVO userShareVO) {
        EntityWrapper<Transaction> transWrapper = new EntityWrapper<>();
        transWrapper.eq("from_user_id", userShareVO.getId());
        transWrapper.eq("trans_type", TransTypeEum.CONTRACT.getCode());
        transWrapper.eq("trans_status", TransStatusEnum.EFFECT.getCode());
        List<Transaction> transList = transMapper.selectList(transWrapper);
        if (CollectionUtils.isEmpty(transList)) {
            logger.info("{}未找到用户[{}]合约购买记录，跳过收益计算。", LOG_PREFIX, userShareVO.getId());
            return null;
        } else {
            return transList.get(0);
        }
    }

    /**
     * 递归填充所有分享用户map
     *
     * @param allShareMap
     * @param userShare
     */
    private void fulfillAllMap(Map<String, UserShareVO> allShareMap, UserShareVO userShare) {
        allShareMap.put(userShare.getId(), userShare);
        if (!CollectionUtils.isEmpty(userShare.getSubUsers())) {
            for (UserShareVO element : userShare.getSubUsers()) {
                fulfillAllMap(allShareMap, element);
            }
        }
    }

    /**
     * 递归填充所有分享用户层级map
     *
     * @param allShareLevelMap
     * @param userShare
     * @param pathLevel
     */
    private void fulfillLevelMap(Map<Integer, List<UserShareVO>> allShareLevelMap, UserShareVO userShare, Integer pathLevel) {
        List<UserShareVO> levelShareList = allShareLevelMap.get(pathLevel);
        if (levelShareList == null) {
            levelShareList = new ArrayList<>();
        }
        userShare.setLevel(pathLevel.toString());
        levelShareList.add(userShare);
        allShareLevelMap.put(pathLevel, levelShareList);
        if (!CollectionUtils.isEmpty(userShare.getSubUsers())) {
            ++pathLevel;
            for (UserShareVO element : userShare.getSubUsers()) {
                fulfillLevelMap(allShareLevelMap, element, pathLevel);
            }
        }
    }

    /**
     * 递归填充所有分享用户合约购买记录
     *
     * @param allShareTrans
     * @param userShare
     */
    private void fulfillAllShareTransMap(Map<String, Transaction> allShareTrans, UserShareVO userShare) {
        Transaction shareTrans = getContractTrans(userShare);
        if (shareTrans != null) {
            allShareTrans.put(userShare.getId(), shareTrans);
        }
        if (!CollectionUtils.isEmpty(userShare.getSubUsers())) {
            for (UserShareVO element : userShare.getSubUsers()) {
                fulfillAllShareTransMap(allShareTrans, element);
            }
        }
    }

    /**
     * 递归填充所有分享用户等级map
     *
     * @param allGradeMap
     * @param userShare
     */
    private void fulfillAllGradeMap(Map<String, List<UserShareVO>> allGradeMap, UserShareVO userShare) {
        String gradeCode = getGradeCode(userShare.getGradeId());
        if (StringUtils.isBlank(gradeCode)) {
            gradeCode = userShare.getGradeId();
            //throw new BusinessException("用户等级API调用失败或未知等级ID");
        }
        List<UserShareVO> userGradeList = allGradeMap.get(gradeCode);
        if (userGradeList == null) {
            userGradeList = new ArrayList<>();
        }
        userGradeList.add(userShare);
        allGradeMap.put(gradeCode, userGradeList);
        if (!CollectionUtils.isEmpty(userShare.getSubUsers())) {
            for (UserShareVO element : userShare.getSubUsers()) {
                fulfillAllGradeMap(allGradeMap, element);
            }
        }
    }

    /**
     * 获取用户等级code
     *
     * @param gradeId
     * @return
     */
    private String getGradeCode(String gradeId) {
        if (CollectionUtils.isEmpty(userGradeCodeMap)) {
            RestTemplate restTemplate = new RestTemplate();
            JSONObject jsonObj = restTemplate.getForObject(API_GRADE_LIST, JSONObject.class);
            if (jsonObj == null) {
                return StringUtils.EMPTY;
            }
            JSONArray ugDataJsonArray = jsonObj.getJSONArray("data");
            if (ugDataJsonArray != null && ugDataJsonArray.size() > 0) {
                for (int i = 0; i < ugDataJsonArray.size(); i++) {
                    JSONObject resultObject = ugDataJsonArray.getJSONObject(i);
                    userGradeCodeMap.put(resultObject.getString("gradeId"), resultObject.getString("gradeCode"));
                }
            }
        }
        return userGradeCodeMap.get(gradeId);
    }

    /**
     * 获取指定用户下的所有分享子用户（包括用户本身）
     *
     * @param allSubUserList
     * @param userShareVO
     */
    private void fulfillAllSubUserList(List<UserShareVO> allSubUserList, UserShareVO userShareVO) {
        allSubUserList.add(userShareVO);
        if (!CollectionUtils.isEmpty(userShareVO.getSubUsers())) {
            for (UserShareVO eachSubUser : userShareVO.getSubUsers()) {
                fulfillAllSubUserList(allSubUserList, eachSubUser);
            }
        }
    }

    /**
     * 获取U对SK的汇率，如1U=10SK时，汇率为10
     *
     * @return
     */
    private BigDecimal getCurExRate() {
        String price = exchangeCenter.price();
        return new BigDecimal(price);
    }

    /**
     * 将用户置为无效
     *
     * @param userId
     */
    private void inactiveUser(String userId) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put(PARAM_USER_ID, userId);
        paramsMap.put("status", 0);
        restTemplate.put(API_CHANGE_USER_STATUS, paramsMap);
    }

}
