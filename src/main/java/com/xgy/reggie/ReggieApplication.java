package com.xgy.reggie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Slf4j     //开启日志信息，方便调试
@ServletComponentScan //开启Servlet组件扫描，不然创建的过滤器WebFilter不会被扫描到生效
@SpringBootApplication  //boot启动类
@EnableTransactionManagement  //开启事务
public class ReggieApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReggieApplication.class,args);
        log.info("项目启动成功！");
    }
}
