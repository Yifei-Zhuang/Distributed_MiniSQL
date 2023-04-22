package com.zhangyin.region.timerEvent;

import com.zhangyin.region.pojo.Region;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Deprecated
@Component
public class RefreshRegion implements Runnable {
    @Autowired
    CuratorFramework curatorFramework;
    @Value("${zookeeper.client.name}")
    private String regionName;
    @Autowired
    Region region;


    @Override
    public void run() {
        byte[] data = new byte[0];
        try {
            data = curatorFramework.getData().forPath("/lss/" + regionName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        region.copyFrom(new String(data));
        System.out.println(region);
    }
}
