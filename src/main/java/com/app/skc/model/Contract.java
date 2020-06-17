package com.app.skc.model;

import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 合约
 */
@Data
@TableName("skc_contract")
public class Contract extends Model <Contract> {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @TableId(value = "id")
    private String id;
    /**
     * 合约代码
     */
    private String code;

    /**
     * 合约名称
     */
    private String name;

    /**
     * 合约价格
     */
    private Integer price;

    /**
     * 合约状态 0 启用 1 停用
     */
    private String status;


    @Override
    protected Serializable pkVal() {
        return null;
    }
}
