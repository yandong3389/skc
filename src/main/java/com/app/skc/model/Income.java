package com.app.skc.model;

import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 收益表
 */
@Data
@TableName("skc_income")
public class Income extends Model<Income> {
    @Id
    private String id;
    /**
     * 用户 id
     */
    @TableField(value = "userId")
    private String userId;
    /**
     * 用户名称
     */
    @TableField(value = "userName")
    private String userName;
    /**
     * 合约 id
     */
    @TableField(value = "contractId")
    private String contractId;

    /**
     * 收益日期：yyyyMMdd
     */
    @TableField(value = "dateAcct")
    private String dateAcct;

    /**
     * 静态收益
     */
    @TableField(value = "staticIn")
    private BigDecimal staticIn;
    /**
     * 分享收益
     */
    @TableField(value = "shareIn")
    private BigDecimal shareIn;
    /**
     * 管理收益
     */
    @TableField(value = "manageIn")
    private BigDecimal manageIn;

    /**
     * 总收益
     */
    @TableField("total")
    private BigDecimal total;

    /**
     * 创建时间
     */
    @TableField("createTime")
    private Date createTime;

    @Override
    protected Serializable pkVal() {
        return null;
    }
}
