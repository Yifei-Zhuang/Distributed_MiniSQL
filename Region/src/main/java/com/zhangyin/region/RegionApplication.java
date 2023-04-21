package com.zhangyin.region;

import com.zhangyin.region.service.ZKService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RegionApplication {

    @Autowired
    ZKService zkService;

    @Autowired
//    ListenDB listenDB;
//    @Autowired
//    RefreshRegion refreshRegion;
//    private final ExecutorService executor = Executors.newFixedThreadPool(1);

//    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);


    public static void main(String[] args) {
        SpringApplication.run(RegionApplication.class, args);
    }

    @PostConstruct
    public void initZK() {
        try {
            zkService.init();
//            initTimer();
//            initCanal();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    public void initCanal() {
//        executor.submit(listenDB);
//    }
//
//    public void initTimer() {
//        scheduledExecutorService.scheduleAtFixedRate(
//                refreshRegion,
//                500,
//                2000,
//                TimeUnit.MILLISECONDS);
//    }

}
