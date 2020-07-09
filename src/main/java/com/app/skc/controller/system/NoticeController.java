package com.app.skc.controller.system;
import com.app.skc.mapper.system.NoticeMapper;
import com.app.skc.model.system.Notice;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.app.skc.utils.SkcConstants.*;

@RestController
@RequestMapping("/notice")

public class NoticeController {

    @Autowired
    NoticeMapper noticeMapper;
    @GetMapping("/last")
    public ResponseResult getLast(){
        EntityWrapper<Notice> entityWrapper = new EntityWrapper <>();
        entityWrapper.orderBy(CREATE_TIME,false);
        entityWrapper.last(" limit 1");
        List <Notice> notices = noticeMapper.selectList(entityWrapper);
        if (notices.size()>0){
            return ResponseResult.success("查询成功",notices.get(0));
        }
        return ResponseResult.success();
    }

    @GetMapping("/history")
    public ResponseResult getHistory(Page page){
        List <Notice> notices = null;
        if (page ==null){
            page = new Page();
        }else {
            PageHelper.startPage(page);
            EntityWrapper<Notice> entityWrapper = new EntityWrapper <>();
            entityWrapper.orderBy(CREATE_TIME,false);
            notices = noticeMapper.selectList(entityWrapper);
        }
        return ResponseResult.success().setData(new PageInfo <>(notices));
    }

}
