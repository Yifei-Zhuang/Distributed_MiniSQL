package master;

import jakarta.annotation.PostConstruct;
import master.timerEvent.CheckRegionThread;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class MasterApplication {
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    @Autowired
    CheckRegionThread refreshRegion;

    public static void main(String[] args) {
        SpringApplication.run(MasterApplication.class, args);
    }

    @PostConstruct
    public void init() {
        initTimer();
    }

    public void initTimer() {
        scheduledExecutorService.scheduleAtFixedRate(
                refreshRegion,
                500,
                5000,
                TimeUnit.MILLISECONDS);
    }

}
