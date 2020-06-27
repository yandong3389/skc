package com.app.skc.model;

import com.baomidou.mybatisplus.activerecord.Model;
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
public class Income extends Model <Income> {
    @Id
    private String id;
    /**
     * 用户 id
     */
    private String userId;
    /**
     * 合约 id
     */
    private String contractId;

    /**
     * 收益日期：yyyyMMdd
     */
    private String dateAcct;

    /**
     * 静态收益
     */
    private BigDecimal staticIn;
    /**
     * 分享收益
     */
    private BigDecimal shareIn;
    /**
     * 管理收益
     */
    private BigDecimal manageIn;

    /**
     * 总收益
     */
    private BigDecimal total;

    /**
     * 创建时间
     */
    private Date createTime;

    @Override
    protected Serializable pkVal() {
        return null;
    }
}
