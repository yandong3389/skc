package com.app.skc.exception;

import com.app.skc.enums.ApiErrEnum;

/**
 * 自定义封装业务异常类
 */
public class BusinessException extends Exception{

	public BusinessException(Object object){
		super(object.toString());
	}

	public BusinessException(ApiErrEnum apiErrEnum){
		super(apiErrEnum.toString()+":"+apiErrEnum.getDesc());
	}
}
