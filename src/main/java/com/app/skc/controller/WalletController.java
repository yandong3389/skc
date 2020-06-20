package com.app.skc.controller;

import com.alibaba.fastjson.JSONObject;
import com.app.skc.enums.ApiErrEnum;
import com.app.skc.enums.TransStatusEnum;
import com.app.skc.enums.TransTypeEum;
import com.app.skc.service.TransactionService;
import com.app.skc.service.WalletService;
import com.app.skc.utils.SkcConstants;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.Web3j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户钱包
 */
@RestController
@RequestMapping("/wallet")
public class WalletController {
	private static final Logger logger = LoggerFactory.getLogger(WalletController.class);
	@Autowired
	private WalletService walletService;
	@Autowired
	private TransactionService transactionService;
	@Autowired
	private Web3j web3j;

	/**
	 * 获取用户钱包可用余额信息
	 *
	 * @param userId     用户 ID
	 * @param walletType 钱包类型
	 * @return
	 */
	@GetMapping("/balance")
	public ResponseResult queryBal(String userId, String walletType) {
		return walletService.getAvailBal(userId, walletType);
	}

	/**
	 * 获取用户钱包地址
	 * @param userId 用户 Id
	 * @return 用户钱包地址list
	 */
	@GetMapping("/address")
	public ResponseResult address(String userId) {
		return walletService.getAddress(userId);
	}

	/**
	 * 获取用户充值记录
	 *
	 * @param userId     用户 id
	 * @param walletType 钱包类型
	 * @param page       分页信息
	 * @return
	 */
	@GetMapping("/record/in")
	public ResponseResult in(String userId, String walletType, Page page) {
		Map<String, Object> params = buildTransQueryParam(userId, walletType, TransTypeEum.IN, page);
		if (params == null) {
			return ResponseResult.fail(ApiErrEnum.REQ_PARAM_NOT_NULL);
		}
		return transactionService.transQueryByPage(params);
	}

	/**
	 * 获取用户提现记录
	 *
	 * @param userId     用户 id
	 * @param walletType 用户类型
	 * @param page       分页信息
	 * @return
	 */
	@GetMapping("/record/out")
	public ResponseResult out(String userId, String walletType, Page page) {
		Map<String, Object> params = buildTransQueryParam(userId, walletType, TransTypeEum.OUT, page);
		if (params == null) {
			return ResponseResult.fail(ApiErrEnum.REQ_PARAM_NOT_NULL);
		}
		return transactionService.transQueryByPage(params);
	}

	/**
	 * 创建钱包
	 * @param userIdJson 用户 id - userId
	 * @return 返回的结果，0正确ERR500错误
	 */
	@PostMapping("/create")
	public ResponseResult createWallet(@RequestBody JSONObject userIdJson) throws Throwable {
		String userId = userIdJson.getString("userId");
		try{
			if (StringUtils.isBlank(userId)) {
				System.out.println(userId);
				return ResponseResult.fail(ApiErrEnum.REQ_PARAM_NOT_NULL);
			}else {
				return walletService.createWallet(userId);
			}

		} catch (IOException | CipherException io) {
			return ResponseResult.fail(ApiErrEnum.CREATE_WALLET_FAIL);
		}
	}

	/**
	 * 钱包用户提现<br/>
	 * 参数列表：userId用户Id  walletType 钱包类型  toAddress提现到账地址  amount 金额
	 *
	 * @param jsonObject userId用户Id  walletType 钱包类型  toAddress提现到账地址  amount 金额
	 * @return
	 * @Api
	 */
	@PostMapping("/cashOut")
	@ResponseBody
	public ResponseResult cashOut(@RequestBody JSONObject jsonObject) {
		logger.info("提现交易开始，请求参数{},", jsonObject.toJSONString());
		try {
			String userId = jsonObject.getString("userId");
			String walletType = jsonObject.getString("walletType");
			String toAddress = jsonObject.getString("toAddress");
			String amount = jsonObject.getString("amount");
			return transactionService.cashOut(userId, walletType, toAddress, amount);
		} catch (Exception e) {
			logger.error("提现交易异常", e);
			return ResponseResult.fail("-999", e.getMessage());
		}

	}

	private Map<String, Object> buildTransQueryParam(String userId, String walletType, TransTypeEum transType, Page page) {
		if (StringUtils.isBlank(userId) || StringUtils.isBlank(walletType)) {
			return null;
		}
		Map<String, Object> params = new HashMap<>();
		if (TransTypeEum.IN.getCode().equals(transType.getCode())) {
			params.put(SkcConstants.TO_USER_ID, userId);
			params.put(SkcConstants.TO_WALLET_TYPE, walletType);
			params.put(SkcConstants.TRANS_TYPE, transType.getCode());
		} else {
			params.put(SkcConstants.FROM_USER_ID, userId);
			params.put(SkcConstants.FROM_WALLET_TYPE, walletType);
			params.put(SkcConstants.TRANS_TYPE, transType.getCode());
		}
		if (page != null) {
			params.put(SkcConstants.PAGE_NUM, page.getPageNum());
			params.put(SkcConstants.PAGE_SIZE, page.getPageSize());
		} else {
			params.put(SkcConstants.PAGE_NUM, 1);
			params.put(SkcConstants.PAGE_SIZE, 10);
		}
		params.put(SkcConstants.TRANS_STATUS, TransStatusEnum.SUCCESS.getCode());
		return params;
	}

}

