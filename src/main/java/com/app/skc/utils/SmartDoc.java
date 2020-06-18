package com.app.skc.utils;

import com.power.doc.builder.ApiDocBuilder;
import com.power.doc.model.ApiConfig;

public class SmartDoc {

    public static void main(String[] args) {
        create();
    }

    public static void create() {
        ApiConfig config = new ApiConfig();
        //服务地址
        config.setServerUrl("http://localhost:8010");
        //生成到一个文档
        config.setAllInOne(true);
        //采用严格模式
        config.isStrict();
        //文档输出路径
        config.setOutPath("/Users/Dylan/项目/api");
        ApiDocBuilder.buildApiDoc(config);
        //将生成的文档输出到/Users/dujf/Downloads/md目录下，严格模式下api-doc会检测Controller的接口注释
    }
}
