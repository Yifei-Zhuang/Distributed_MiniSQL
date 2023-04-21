package com.zhangyin.region.service;

import com.alibaba.fastjson2.JSON;
import com.zhangyin.region.pojo.Region;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class ZKService {
    @Autowired
    CuratorFramework curatorFramework;
    @Autowired
    SqlService sqlService;
    @Value("${zookeeper.client.name}")
    private String regionName;
    @Value("${server.port}")
    private int port;
    private static boolean initRegionDone = false;
    @Autowired
    Region region;

    public void init() throws Exception {
        System.out.println("ZK init!");
        String list = sqlService.execSelectSql("show tables");
        String[] tableNames = new String[0];
        if (list != null) {
            tableNames = list.trim().split("\n");
        }
        // drop all table
        for (int i = 1; i < tableNames.length; i++) {
            String table = tableNames[i];
            if (!table.isEmpty()) {
                sqlService.execCreateAndDropSql("drop table " + table + ";");
            }
        }
        if (curatorFramework.checkExists().forPath("/lss/" + regionName) != null) {
            curatorFramework.delete().forPath("/lss/" + regionName);
        }
        region.setPort(port);
        region.setRegionName(regionName);
        region.setTables(new ArrayList<>());
        curatorFramework.create().creatingParentsIfNeeded().forPath("/lss/" + regionName, JSON.toJSONString(region).getBytes());
        initRegionDone = true;
        System.out.println("ZK init done!");
    }
}
