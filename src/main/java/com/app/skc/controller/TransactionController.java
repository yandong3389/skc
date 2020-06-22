package com.app.skc.controller;

import com.alibaba.fastjson.JSONObject;
import com.app.skc.enums.ApiErrEnum;
import com.app.skc.service.TransactionService;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
	private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);
	@Autowired
	public TransactionController(TransactionService transactionService) {
		this.transactionService=transactionService;
	}
	/**
	 * 获取交易记录
	 * @param page 分页参数
	 * @param map 参数 trans_type-交易类型,多个用英文逗号分隔；
	 * @return
	 */
	@ApiOperation(value = "交易记录", notes = "获取交易记录")
	@GetMapping("/getTransaction")
	@ResponseBody
	public ResponseResult getTransaction(@RequestParam Map <String, Object> map, Page page) {
		logger.info("[交易] - 开始查询交易,param = [{}]", JSONObject.toJSONString(map));
		return transactionService.transQueryByPage(map, page);
	}

	/**
	 * 内部交易转账
	 *
	 * @param toWalletAddress 到账钱包地址
	 * @param transferNumber 转账金额
	 * @param userId 用户id
	 * @param walletType 钱包类型
	 * @return
	 */
    @PostMapping("/transfer")
	@ResponseBody
	public ResponseResult transfer(@RequestBody JSONObject jsonObject)
	{
		try {
			if (jsonObject == null) {
				return ResponseResult.fail(ApiErrEnum.REQ_PARAM_NOT_NULL);
			} else {
				String toWalletAddress = jsonObject.getString("toWalletAddress");
				String transferNumber = jsonObject.getString("transferNumber");
				String userId = jsonObject.getString("userId");
				String walletType = jsonObject.getString("walletType");
				return transactionService.transfer(toWalletAddress, transferNumber, userId, walletType);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseResult.fail("ERR500",e.getMessage());
		}
	}
}

