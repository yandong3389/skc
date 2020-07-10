package com.app.skc.model;

import com.baomidou.mybatisplus.annotations.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("skc_temporary_level")
public class TemporaryLevel {
    /**
     * id
     */
    private String id;
    /**
     * 用户 id
     */
    private String userId;
    /**
     * 用户姓名
     */
    private String userName;
    /**
     * 用户等级
     */
    private int userLevel;
    /**
     * 开通时间
     */
    private Date startTime;
    /**
     * 结束时间
     */
    private Date endTime;
}
