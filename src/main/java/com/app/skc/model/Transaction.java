package com.app.skc.model;

import com.baomidou.mybatisplus.enums.IdType;
import java.math.BigDecimal;
import java.util.Date;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;

import java.io.Serializable;

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
    @TableId(value = "transaction_id", type = IdType.AUTO)
    private Integer transactionId;
    /**
     * 支出用户id
     */
    private Integer fromUserId;
    /**
     * 支出钱包id
     */
    private String fromWalletAddress;
    /**
     * 收取用户id
     */
    private String toUserId;
    /**
     * 收取钱包id
     */
    private String toWalletAddress;
    /**
     * 交易金额
     */
    private BigDecimal fromAmount;

    private String fromWalletType;

    private BigDecimal toAmount;

    private String toWalletType;

    private BigDecimal feeAmount;
    /**
     * 交易类型（0-转账）
     */
    private String transactionType;
    /**
     * 交易时间
     */
    private Date createTime;
    /**
     * 交易说明
     */
    private String remark;

    private String transactionStatus;

    private String transactionHash;

    private String contractType;

    @Override
    protected Serializable pkVal() {
        return null;
    }
}
