package com.app.skc.utils.viewbean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.app.skc.enums.ApiErrEnum;

/**
 * ajax数据response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseResult {

	private String status;
	private String code = "0";//code编码
	private String msg;//回传信息
	private Object data;//数据集合

	public String getCode() {
		return code;
	}

	public ResponseResult setCode(String code) {
		this.code = code;
		return this;
	}

	public String getMsg() {
		return msg;
	}

	public ResponseResult setMsg(String msg) {
		this.msg = msg;
		return this;
	}

	public Object getData() {
		return data;
	}

	public ResponseResult setData(Object data) {
		this.data = data;
		return this;
	}

	public ResponseResult setErrMsg(String code , String msg){
		this.code = code;
		this.msg = msg;
		return this;
	}

	/**
	 *添加data
	 */
	public ResponseResult add(Object data){
		this.data=data;
		return this;
	}

	/**
	 *默认成功
	 */
	public static ResponseResult success(){
		ResponseResult result=new ResponseResult();
		result.setCode("0");
		result.setMsg("请求成功");
		result.setStatus("0");
		return result;
	}

	/**
	 * 自定义返回
	 * @param msg
	 * @return
	 */
	public static ResponseResult success(String msg,Object data){
		ResponseResult result=new ResponseResult();
		result.setCode("0");
		result.setMsg(msg);
		result.setData(data);
		result.setStatus("0");
		return result;
	}


	/**
	 *默认失败
	 */
	public static ResponseResult fail(){
		ResponseResult result=new ResponseResult();
		result.setCode("ERR500");
		result.setMsg("请求失败");
		result.setStatus("-1");
		return result;
	}

	/**
	 *自定义失败
	 */
	public static ResponseResult fail(String code,String msg){
		ResponseResult result=new ResponseResult();
		result.setCode(code);
		result.setMsg(msg);
		result.setStatus("-1");
		return result;
	}

	/**
	 *自定义失败
	 */
	public static ResponseResult fail(ApiErrEnum errEnum){
		ResponseResult result=new ResponseResult();
		result.setCode(errEnum.getCode());
		result.setMsg(errEnum.getDesc());
		result.setStatus("-1");
		return result;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
