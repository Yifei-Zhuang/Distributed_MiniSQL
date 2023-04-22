package com.zhangyin.region;

import com.zhangyin.region.service.ZKService;
import com.zhangyin.region.timerEvent.RefreshRegion;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class RegionApplication {

    @Autowired
    ZKService zkService;

    //    @Autowired
//    ListenDB listenDB;
    @Autowired
    RefreshRegion refreshRegion;
//    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);


    public static void main(String[] args) {
        SpringApplication.run(RegionApplication.class, args);
    }

    @PostConstruct
    public void initZK() {
        try {
            zkService.init();
            initTimer();
//            initCanal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //    public void initCanal() {
//        executor.submit(listenDB);
//    }
//
    public void initTimer() {
        scheduledExecutorService.scheduleAtFixedRate(
                refreshRegion,
                500,
                5000,
                TimeUnit.MILLISECONDS);
    }

}
