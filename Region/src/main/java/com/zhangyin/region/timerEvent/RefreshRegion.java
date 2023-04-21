package com.zhangyin.region.timerEvent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhangyin.region.pojo.Region;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
@Component
public class RefreshRegion implements Runnable {
    @Autowired
    CuratorFramework curatorFramework;
    @Value("${zookeeper.client.name}")
    private String regionName;
    @Autowired
    Region region;

    @Autowired
    ConcurrentHashMap<String, List<Region>> tableRegions;

    @Override
    public void run() {
        byte[] data = new byte[0];
        try {
            data = curatorFramework.getData().forPath("/lss/" + regionName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        region.copyFrom((JSONObject) JSON.parse(new String(data)));
        System.out.println(region);
        try {
            data = curatorFramework.getData().forPath("/lss/__tableMeta__");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        tableRegions.clear();
        try {
            JSONObject temp = ((JSONObject) JSON.parse(new String(data)));
            for (String key : temp.keySet()) {
                tableRegions.put(key, temp.getJSONArray(key).toJavaList(Region.class));
            }
            System.out.println(tableRegions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
