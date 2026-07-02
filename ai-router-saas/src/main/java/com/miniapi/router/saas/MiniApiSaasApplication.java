package com.miniapi.router.saas;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.miniapi.router.saas", "com.miniapi.router.core"})
@MapperScan("com.miniapi.router.saas.mapper")
@EnableAsync
@EnableScheduling
public class MiniApiSaasApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiniApiSaasApplication.class, args);
    }
}
