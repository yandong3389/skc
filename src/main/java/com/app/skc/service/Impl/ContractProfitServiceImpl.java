package com.app.skc.service.Impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.app.skc.enums.SysConfigEum;
import com.app.skc.enums.TransStatusEnum;
import com.app.skc.enums.TransTypeEum;
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
    @Autowired
    private final IncomeMapper incomeMapper;
    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private TransactionMapper transMapper;

    @Autowired
    public ContractProfitServiceImpl(IncomeMapper incomeMapper) {
        this.incomeMapper = incomeMapper;
    }

    @Autowired
    private ConfigService configService;
    // 用户伞下有效用户列表API
    @Value("#{'${contract.api-grade-list:http://www.skgame.top/v1/Trade/Get_Grade_List}'}")
    private String API_GRADE_LIST;
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

        // 2、用户树遍历处理：静态 + 分享
        for (int i = allLevelMap.size(); i > 0; i--) {
            List<UserShareVO> levelShareList = allLevelMap.get(i);
            for (UserShareVO user : levelShareList) {
                Transaction contractTrans = allShareTrans.get(user.getId());
                List<UserShareVO> allSubUserList = new ArrayList<>();
                fulfillAllSubUserList(allSubUserList, user);
                if (contractTrans == null) {
                    continue;
                }
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
                    logger.info("{}新增合约收益记录，userName为[{}], 收益id为[{}]", LOG_PREFIX, user.getName(), contrInId);
                    contractIncome = new Income();
                    contractIncome.setId(contrInId);
                    contractIncome.setUserId(user.getId());
                    contractIncome.setContractId(contractTrans.getTransId());
                    incomeMap.put(user.getId(), contractIncome);
                }
                // 2.1 静态收益
                if (dealStaticProfit(contractTrans, contractWallet, contractIncome)) {
                    continue;
                }
                // 2.2 分享收益
                List<UserShareVO> directSubUserList = dealShareProfit(incomeMap, allShareTrans, user, contractTrans, contractWallet, contractIncome);
                if (directSubUserList == null) {
                    continue;
                }
                // 2.3 社区收益
                int directShare = directSubUserList.size();
                BigDecimal totalContract = BigDecimal.ZERO;
                for (String userId : allShareTrans.keySet()) {
                    totalContract = totalContract.add(allShareTrans.get(userId).getPrice());
                }
                if (directShare >= 10 || totalContract.compareTo(new BigDecimal(80000)) >= 0) {
                    BigDecimal mngRate = getMngRate(directSubUserList, directShare, totalContract);
                    BigDecimal totalProfit = BigDecimal.ZERO;
                    for (String userId : incomeMap.keySet()) {
                        Income eachIncome = incomeMap.get(userId);
                        totalProfit = totalProfit.add(eachIncome.getStaticIn());
                    }
                    BigDecimal mngProfit = BigDecimal.ZERO;
                    for (UserShareVO eachSubUser : allSubUserList) {
                        if (eachSubUser.getId().equals(user.getId())) {
                            continue;
                        }
                        if (allShareTrans.get(user.getId()).getPrice().compareTo(allShareTrans.get(eachSubUser.getId()).getPrice()) < 0) {
                            mngProfit = mngProfit.add(totalProfit.multiply(mngRate).multiply(allShareTrans.get(user.getId()).getPrice().divide(allShareTrans.get(eachSubUser.getId()).getPrice(), 6, RoundingMode.DOWN)));
                        } else {
                            mngProfit = mngProfit.add(totalProfit.multiply(mngRate));
                        }
                    }
                    if (mngProfit.add(contractIncome.getStaticIn()).add(contractIncome.getShareIn()).compareTo(contractWallet.getSurplusContract()) < 0) {
                        contractIncome.setManageIn(mngProfit);
                    } else {
                        saveProfitInfo(contractTrans, contractWallet, contractIncome, contractIncome.getStaticIn(), contractIncome.getShareIn(), contractWallet.getSurplusContract().subtract(contractIncome.getStaticIn()).subtract(contractIncome.getShareIn()), true);
                    }
                }
                saveProfitInfo(contractTrans, contractWallet, contractIncome, contractIncome.getStaticIn(), contractIncome.getShareIn(), contractIncome.getManageIn(), false);
            }
        }
    }

    /**
     * 获取社区收益比例
     *
     * @param subUserShareList
     * @param directShare
     * @param totalContract
     * @return
     * @throws BusinessException
     */
    private BigDecimal getMngRate(List<UserShareVO> subUserShareList, int directShare, BigDecimal totalContract) {
        Config mngRateConfig;
        int bronzeCommCnt = 0;
        int goldCommCnt = 0;
        int diamondCommCnt = 0;
        int kingCommCnt = 0;
        for (UserShareVO userShareVO : subUserShareList) {
            Map<String, List<UserShareVO>> eachCommGradeMap = new HashMap<>();
            fulfillAllGradeMap(eachCommGradeMap, userShareVO);
            if (!CollectionUtils.isEmpty(eachCommGradeMap.get("king"))) {
                ++kingCommCnt;
                continue;
            }
            if (!CollectionUtils.isEmpty(eachCommGradeMap.get("diamond"))) {
                ++diamondCommCnt;
                continue;
            }
            if (!CollectionUtils.isEmpty(eachCommGradeMap.get("gold"))) {
                ++goldCommCnt;
                continue;
            }
            if (!CollectionUtils.isEmpty(eachCommGradeMap.get("bronze"))) {
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
     * @param contractTrans
     * @param contractWallet
     * @param contractIncome
     * @return
     */
    private List<UserShareVO> dealShareProfit(Map<String, Income> incomeMap, Map<String, Transaction> allShareTrans, UserShareVO user, Transaction contractTrans, Wallet contractWallet, Income contractIncome) {
        List<UserShareVO> directSubUserList = user.getSubUsers();
        List<UserShareVO> allSubUserList = new ArrayList<>();
        fulfillAllSubUserList(allSubUserList, user);
        BigDecimal shareProfit = BigDecimal.ZERO;
        if (CollectionUtils.isEmpty(directSubUserList)) {
            return null;
        } else {
            Map<Integer, List<UserShareVO>> subLevelMap = new HashMap<>();
            for (UserShareVO eachUserVO : allSubUserList) {
                if (eachUserVO.getId().equals(user.getId())) {
                    continue;
                }
                List<UserShareVO> subLevelUserList = subLevelMap.get(Integer.parseInt(eachUserVO.getLevel()));
                if (CollectionUtils.isEmpty(subLevelUserList)) {
                    subLevelUserList = new ArrayList<>();
                }
                subLevelUserList.add(eachUserVO);
                subLevelMap.put(Integer.parseInt(eachUserVO.getLevel()), subLevelUserList);
            }
            int directShareNum = directSubUserList.size();
            int curUserLevel = Integer.parseInt(user.getLevel());
            for (int j = curUserLevel + 1; j <= curUserLevel + directShareNum; j++) {
                List<UserShareVO> curLevelShareList = subLevelMap.get(j);
                if (CollectionUtils.isEmpty(curLevelShareList)) {
                    break;
                }
                Config shareRateConfig;
                switch (j - curUserLevel) {
                    case 1:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_G1.getCode());
                    case 2:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_G2.getCode());
                    case 3:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_G3.getCode());
                    case 4:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_G4.getCode());
                    case 5:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_G5.getCode());
                    default:
                        shareRateConfig = configService.getByKey(SysConfigEum.CONTR_SHARE_RATE_GX.getCode());
                }
                BigDecimal shareRate = new BigDecimal(shareRateConfig.getConfigValue());
                for (UserShareVO eachUser : curLevelShareList) {
                    Income income = incomeMap.get(eachUser.getId());
                    if (income != null) {
                        if (contractTrans.getPrice().compareTo(allShareTrans.get(eachUser.getId()).getPrice()) < 0) {
                            shareProfit = shareProfit.add(income.getStaticIn().multiply(shareRate).multiply(contractTrans.getPrice().divide(allShareTrans.get(eachUser.getId()).getPrice(), 6, RoundingMode.DOWN)));
                        } else {
                            shareProfit = shareProfit.add(income.getStaticIn().multiply(shareRate));
                        }
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
     * 保存出局用户收益信息
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
            contractIncome.setStaticIn(contractWallet.getSurplusContract());
            contractTrans.setTransStatus(TransStatusEnum.UNEFFECT.getCode());
            contractTrans.setModifyTime(new Date());
            // TODO
//            transMapper.updateById(contractTrans);
        }
        // 钱包更新
        BigDecimal totalProfit = staticProfit.add(shareProfit).add(mngProfit);
        BigDecimal balChange = totalProfit.multiply(getCurExRate());
        contractWallet.setComsumedContract(contractWallet.getComsumedContract().add(totalProfit));
        contractWallet.setSurplusContract(contractWallet.getSurplusContract().subtract(totalProfit));
        contractWallet.setBalTotal(contractWallet.getBalTotal().add(balChange));
        contractWallet.setBalAvail(contractWallet.getBalAvail().add(balChange));
        contractWallet.setModifyTime(new Date());
        // TODO
//        walletMapper.updateById(contractWallet);
        // 当日收益记录插入
        contractIncome.setStaticIn(staticProfit);
        contractIncome.setShareIn(shareProfit);
        contractIncome.setManageIn(mngProfit);
        contractIncome.setDateAcct(DateUtil.getCurDate());
        contractIncome.setCreateTime(new Date());
        incomeMapper.insert(contractIncome);
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
        incomeWrapper.eq("userId", userShareVO.getId());
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
        transWrapper.eq("from_user_id", userShareVO.getName());
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
        // TODO
        return new BigDecimal(10);
    }

}
