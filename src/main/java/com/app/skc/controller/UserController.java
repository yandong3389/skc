package com.app.skc.controller;

import com.app.skc.enums.ApiErrEnum;
import com.app.skc.mapper.TemporaryLevelMapper;
import com.app.skc.model.TemporaryLevel;
import com.app.skc.utils.viewbean.ResponseResult;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

import static com.app.skc.utils.SkcConstants.END_TIME;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    TemporaryLevelMapper temporaryLevelMapper;
    @GetMapping("/level/temp")
    public ResponseResult TempLevel(String userId){
      if (StringUtils.isNotBlank(userId)){
          Date now = new Date();
          EntityWrapper <TemporaryLevel> entityWrapper = new EntityWrapper <>();
          entityWrapper.gt(END_TIME,now);
          entityWrapper.eq("user_id",userId);
          List <TemporaryLevel>list = temporaryLevelMapper.selectList(entityWrapper);
          if (list.size()!=0){
              int level = list.get(0).getUserLevel();
              return ResponseResult.success("查询成功",level);
          }else {
               //如果返回值为-1 代表没有临时等级
               return ResponseResult.success("查询成功",-1);
          }
      }else {
          return ResponseResult.fail(ApiErrEnum.REQ_PARAM_NOT_NULL);
      }

    }
}
