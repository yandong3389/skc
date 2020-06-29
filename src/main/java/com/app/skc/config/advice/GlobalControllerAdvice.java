package com.app.skc.config.advice;

import com.app.skc.exception.BusinessException;
import com.app.skc.utils.viewbean.ResponseResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) //自定义浏览器返回状态码
    public ResponseResult catchException(BusinessException e) {
        return ResponseResult.fail().setErrMsg("500", e.getMessage());
    }
}
