package com.app.skc.controller;

import com.alibaba.fastjson.JSONObject;
import com.app.skc.enums.ApiErrEnum;
import com.app.skc.service.TransactionService;
import com.app.skc.service.WalletService;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.CipherException;
import org.web3j.protocol.Web3j;

import java.io.IOException;

/**
 * 用户钱包
 */
@RestController
@RequestMapping("/wallet")
public class WalletController {
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
	 * @param userId 用户 id
	 * @param wallteType 钱包类型
	 * @param page 分页信息
	 * @return
	 */
	@GetMapping("/record/in")
	public ResponseResult in(String userId, String wallteType, Page page){
		return null;
	}

	/**
	 * 获取用户提现记录
	 * @param userId 用户 id
	 * @param wallteType 用户类型
	 * @param page 分页信息
	 * @return
	 */
	@GetMapping("/record/out")
	public ResponseResult out(String userId,String wallteType,Page page){
		return null;
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
	 *	钱包用户提现（对外）
	 * @param jsonObject userId用户Id  walletType 钱包类型  toAddress提现到账地址  amount 金额
	 * @return
	 */
	@PostMapping("/cashOut")
	@ResponseBody
	public ResponseResult cashOut(@RequestBody JSONObject jsonObject) {
		try {
			String userId = jsonObject.getString("userId");
			String walletType = jsonObject.getString("walletType");
			String toAddress = jsonObject.getString("toAddress");
			String amount = jsonObject.getString("amount");
			return transactionService.cashOut(userId, walletType, toAddress, amount);
		} catch (Exception e) {
			return ResponseResult.fail("-999", e.getMessage());
		}

	}








}

