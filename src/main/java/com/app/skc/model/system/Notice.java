package com.app.skc.model.system;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
@TableName("sys_notice")
@Data
public class Notice  extends Model <Notice> {
    private static final long serialVersionUID = 1L;
    /** 公告ID */
    @TableId(value = "notice_id", type = IdType.AUTO)
    private Long noticeId;

    /** 公告标题 */
    private String noticeTitle;

    /** 公告类型（1通知 2公告） */
    private String noticeType;

    /** 公告内容 */
    private String noticeContent;

    private Date createTime;

    @Override
    protected Serializable pkVal() {
        return null;
    }
}
