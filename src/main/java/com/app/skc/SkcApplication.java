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
}
