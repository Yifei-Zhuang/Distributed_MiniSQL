package master.timerEvent;

import master.pojo.Region;
import master.service.ZKService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

// 监听所有节点
@Component
public class CheckRegionThread implements Runnable {
    @Autowired
    List<Region> regions;
    @Autowired
    ZKService zkService;

    @Override
    public void run() {
        regions.clear();
        regions.addAll(zkService.getAllRegions());
    }
}
