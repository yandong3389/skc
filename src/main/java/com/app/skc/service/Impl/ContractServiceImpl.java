package com.app.skc.service.Impl;

import com.app.skc.enums.SysConfigEum;
import com.app.skc.enums.TransStatusEnum;
import com.app.skc.enums.TransTypeEum;
import com.app.skc.enums.WalletEum;
import com.app.skc.exception.BusinessException;
import com.app.skc.mapper.ContractMapper;
import com.app.skc.mapper.TransactionMapper;
import com.app.skc.mapper.WalletMapper;
import com.app.skc.model.Contract;
import com.app.skc.model.Transaction;
import com.app.skc.model.Wallet;
import com.app.skc.service.ContractService;
import com.app.skc.service.system.ConfigService;
import com.app.skc.utils.BaseUtils;
import com.app.skc.utils.SkcConstants;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dylan
 */
@Service
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {

    @Autowired
    TransactionMapper transactionMapper;
    @Autowired
    ContractMapper contractMapper;
    @Autowired
    WalletMapper walletMapper;
    @Autowired
    ConfigService configService;

    @Transactional(rollbackFor = BusinessException.class)
    @Override
    public Boolean buy(String userId, String code) throws BusinessException {
        //获取用户钱包
        EntityWrapper <Wallet> fromWalletWrapper = new EntityWrapper <>();
        fromWalletWrapper.eq(SkcConstants.USER_ID, userId);
        fromWalletWrapper.eq(SkcConstants.WALLET_TYPE, WalletEum.USDT.getCode());
        List <Wallet> fromWalletRes = walletMapper.selectList(fromWalletWrapper);
        Wallet wallet = new Wallet();
        if (fromWalletRes.size() > 0) {
            wallet = fromWalletRes.get(0);
        } else {
            new BusinessException("获取用户钱包失败");
        }
        Map map = new HashMap();
        map.put("code", code);
        map.put("status", "0");
        BigDecimal price = BigDecimal.ZERO;
        List <Contract> list = contractMapper.selectByMap(map);
        if (list.size() > 0) {
            price = list.get(0).getPrice();
        } else {
            throw new BusinessException("合约购买失败,合约不存在或者未开放。");
        }
        if (wallet.getBalAvail().compareTo(price) >= 0) {
            BigDecimal oloContractPrice = queryPerformance(new HashMap(), userId);
            if (price.compareTo(oloContractPrice) >= 0) {
                saveBuy(userId, wallet, price);
            } else {
                throw new BusinessException("不能购买比原合约低的合约。");
            }
        } else {
            throw new BusinessException("合约购买失败,用户余额不足。");
        }

        return true;
    }

    /**
     * 保存购买
     *
     * @param userId
     * @param wallet
     * @param price
     */
    private void saveBuy(String userId, Wallet wallet, BigDecimal price) {
        BigDecimal contactDouble = new BigDecimal(configService.getByKey(SysConfigEum.CONTRACT_DOUBLE.getCode()).getConfigValue());
        Date date = new Date();
        //购买
        Transaction transaction = new Transaction();
        transaction.setFromUserId(userId);
        transaction.setTransId(BaseUtils.get64UUID());
        transaction.setTransType(TransTypeEum.CONTRACT.getCode());
        transaction.setTransStatus(TransStatusEnum.EFFECT.getCode());
        transaction.setModifyTime(date);
        transaction.setCreateTime(date);
        transaction.setFromWalletType(wallet.getWalletType());
        transaction.setFromWalletAddress(wallet.getAddress());
        transaction.setPrice(price);
        transactionMapper.insert(transaction);
        wallet.setBalTotal(wallet.getBalAvail().subtract(price));
        wallet.setBalAvail(wallet.getBalAvail().subtract(price));
        wallet.setTotalContract(price.multiply(contactDouble));
        walletMapper.updateById(wallet);
    }

    /**
     * 查询合约列表
     *
     * @return
     */
    @Override
    public List<Contract> list() {
        Contract contract = new Contract();
        return contract.selectAll();
    }

    /**
     * 查询团队业绩
     *
     * @param userIds
     * @return
     */
    @Override
    public BigDecimal teamPerformance(List <String> userIds) {
        BigDecimal performance = BigDecimal.ZERO;
        Map map = new HashMap();
        for (String userId : userIds) {
            performance = performance.add(queryPerformance(map, userId));
        }
        return performance;
    }

    private BigDecimal queryPerformance(Map map, String userId) {
        List <Transaction> transactions;
        map.put(SkcConstants.FROM_USER_ID, userId);
        map.put(SkcConstants.TRANS_TYPE, TransTypeEum.CONTRACT.getCode());
        map.put(SkcConstants.TRANS_STATUS, TransStatusEnum.EFFECT.getCode());
        transactions = transactionMapper.selectByMap(map);
        map.remove(SkcConstants.FROM_USER_ID);
        return transactions.size() > 0 ? transactions.get(0).getPrice() : BigDecimal.ZERO;
    }
}
