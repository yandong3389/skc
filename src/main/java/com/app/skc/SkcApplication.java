package com.app.skc;


import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@MapperScan({ "com.app.skc.mapper" })
@EnableTransactionManagement
@PropertySource(value = {"classpath:application.yml"})
@SpringBootApplication
@EnableScheduling
public class SkcApplication extends SpringBootServletInitializer {

	private static Logger logger = LoggerFactory.getLogger(SkcApplication.class);
	public static void main(String[] args) throws Exception {
        SpringApplication.run(SkcApplication.class, args);
	}


    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application)
    {
        return application.sources(SkcApplication.class);
    }

    /**
     * 设置 web3j
     *//*
    @Value("${web3j.client-address}")
    private String web3jAddress;
    public Web3j web3j(Web3j web3j){
	    web3j = Web3j.build(new HttpService(web3jAddress));
	    return web3j;
    }*/
}
