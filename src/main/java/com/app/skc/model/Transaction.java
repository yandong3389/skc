package com.app.skc.model;

import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 
 * </p>
 *
 * @author 
 * @since 2020-02-05
 */
@Data
@TableName("skc_transaction")
public class Transaction extends Model<Transaction> {

    private static final long serialVersionUID = 1L;

    /**
     * 交易id
     */
    @TableId(value = "trans_id")
    private Integer transId;
    /**
     * 支出用户id
     */
    private Integer fromUserId;

    private String fromWalletType;
    /**
     * 支出钱包id
     */
    private String fromWalletAddress;
    /**
     * 收取用户id
     */
    private String toUserId;
    private String toWalletType;
    /**
     * 收取钱包id
     */
    private String toWalletAddress;
    /**
     * 交易金额
     */
    private BigDecimal fromAmount;

    private BigDecimal toAmount;


    private BigDecimal feeAmount;
    /**
     * 交易类型（0-转账）
     */
    private String transType;
    private String transStatus;
    private String transHash;
    /**
     * 交易说明
     */
    private String remark;

    /**
     * 交易时间
     */
    private Date createTime;
    private Date modifyTime;


    @Override
    protected Serializable pkVal() {
        return null;
    }
}
