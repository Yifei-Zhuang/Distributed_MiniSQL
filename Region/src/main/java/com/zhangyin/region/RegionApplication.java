package com.zhangyin.region;

import com.zhangyin.region.service.ZKService;
import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zhangyin.region.mapper")
public class RegionApplication {

    @Autowired
    ZKService zkService;

    public static void main(String[] args) {
        SpringApplication.run(RegionApplication.class, args);
    }

    @PostConstruct
    public void initZK() {
        try {
            zkService.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
