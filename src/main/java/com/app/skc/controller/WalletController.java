package com.app.skc.controller;


import com.alibaba.fastjson.JSONObject;
import com.app.skc.enums.ApiErrEnum;
import com.app.skc.service.WalletService;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;

import java.io.IOException;

/**
 * 用户钱包
 */
@RestController
@RequestMapping("/skc/api/wallet")
public class WalletController {
	@Autowired
	private  WalletService walletService;
	@Autowired
	private Web3j web3j;

	/**
	 * 获取用户钱包余额信息
	 * @param userId 用户 ID
	 * @param walletType 钱包类型
	 * @return
	 */
	@GetMapping("/balance")
	public ResponseResult query(String userId, String walletType) throws IOException, CipherException {

		Credentials credentials = WalletUtils.loadCredentials("", "/Users/Dylan/Desktop/wallet/UTC--2020-06-13T06-41-47.382000000Z--5bc413403be2d5c0503e89b569b42c0b8f690273.json");
		System.out.println(JSONObject.toJSONString(credentials));
         return null;
	}

	/**
	 * 获取用户钱包地址
	 * @param userId 用户 Id
	 * @return
	 */
	@GetMapping("/address")
	public ResponseResult address(String userId){
		return null;
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
	 * @param userIdJson 用户 id
	 * @return 返回的结果，0正确ERR500错误
	 */
	@PostMapping("/create")
	public ResponseResult createWallet(@RequestBody JSONObject userIdJson) throws Throwable {
		String userId = userIdJson.getString("userId");
		try{
			if (StringUtils.isBlank(userId)) {
				System.out.println(userId);
				return ResponseResult.fail(ApiErrEnum.NOT_PARAM);
			}else {
				return walletService.createWallet(userId);
			}

		} catch (IOException | CipherException io) {
			return ResponseResult.fail(ApiErrEnum.CREATE_WALLET_FAIL);
		}
	}








}

