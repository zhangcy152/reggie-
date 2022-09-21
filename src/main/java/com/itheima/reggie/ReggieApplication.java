package com.itheima.reggie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@SpringBootApplication
@ServletComponentScan //扫描filter包下的  @WebFilter修饰的类
@EnableTransactionManagement //开始事务
//@EnableCaching //打开缓存功能
public class ReggieApplication {
    public static void main(String[] args){

        SpringApplication.run(ReggieApplication.class,args);
        log.info("项目启动...");
    }
}
