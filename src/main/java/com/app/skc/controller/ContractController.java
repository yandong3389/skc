package com.app.skc.controller;

import com.app.skc.utils.viewbean.ResponseResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合约制度
 */
@RequestMapping("/skc/api/contract")
@RestController
public class ContractController {

    /**
     * 购买合约
     * @param id 合约 id
     * @param userId 用户id
     * @return
     */
    @RequestMapping("/buy")
    public ResponseResult buy(String id,String userId){
        return new ResponseResult();
    }

    /**
     * 获取合约信息
     * @return
     */
    @RequestMapping("/list")
    public ResponseResult getContracts(){
        return new ResponseResult();
    }

    /**
     * 获取合约购买记录
     * @param userId 用户 ID
     * @return
     */
    @RequestMapping("history")
    public ResponseResult getHistory(String userId){
        return new ResponseResult();
    }

}
