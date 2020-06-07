package com.app.skc.controller;


import com.app.skc.service.WalletService;
import com.app.skc.utils.viewbean.Page;
import com.app.skc.utils.viewbean.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author
 * @since 2019-06-12
 */
@Controller
@RequestMapping("/mdc/wallet")
public class WalletController {

	private final WalletService walletService;

	@Autowired
	public WalletController(WalletService walletService) {
		this.walletService = walletService;
	}
	
	/**
	 * 获取余额
	 * @param map
	 * @return 返回的结果，0正确ERR500错误
	 */
	@PostMapping("/getBalance")
	@ResponseBody
	public ResponseResult getBalance(@RequestParam Map<String, Object> map, Page page) {
		return walletService.getBalance(page,map);
	}

	/**
	 * 创建钱包
	 * @param
	 * @return 返回的结果，0正确ERR500错误
	 */
	@PostMapping("/createWallet")
	@ResponseBody
	public ResponseResult createWallet(@RequestParam int id,@RequestParam String password)  {
		try {
			return walletService.createWallet(id,password);
		} catch (Throwable e) {
			e.printStackTrace();
			return ResponseResult.fail("ERR505",e.getMessage());
		}
	}


}

