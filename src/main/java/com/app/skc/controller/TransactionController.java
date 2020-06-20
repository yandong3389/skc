package com.app.skc.controller;


import com.app.skc.service.TransactionService;
import com.app.skc.utils.viewbean.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * <p>
 * 交易前端控制器
 * </p>
 *
 * @author
 * @since 2019-06-12
 */
@Api(value = "TransactionController")
@Controller
@RequestMapping("/transaction")
public class TransactionController {

	private final TransactionService transactionService;

	@Autowired
	public TransactionController(TransactionService transactionService) {
		this.transactionService=transactionService;
	}

	/**
	 * 获取交易记录
	 *
	 * @param map 参数 trans_type-交易类型,多个用英文逗号分隔；
	 * @return
	 */
	@ApiOperation(value = "交易记录", notes = "获取交易记录")
	@PostMapping("/getTransaction")
	@ResponseBody
	public ResponseResult getTransaction(@RequestParam Map<String, Object> map) {
		return transactionService.transQueryByPage(map);
	}

	/**
	 * 内部交易转账
	 *
	 * @param toWalletAddress 贷方钱包地址
	 * @param transferNumber 转账金额
	 * @param userId  借方用户id
	 * @param walletType 钱包类型
	 * @return
	 */
    @PostMapping("/transfer")
	@ResponseBody
	public ResponseResult transfer(
									 @RequestParam(required = true)String toWalletAddress,
									 @RequestParam(required = true)String transferNumber,
									 @RequestParam(required = true)String userId,
									 @RequestParam(required = true)String walletType)
	{
		try {
			return transactionService.transfer(toWalletAddress,transferNumber,userId,walletType);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseResult.fail("ERR500",e.getMessage());
		}
	}
}

