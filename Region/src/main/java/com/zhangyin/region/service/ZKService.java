package com.zhangyin.region.service;

import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ZKService {
    @Autowired
    CuratorFramework curatorFramework;
    @Autowired
    SqlService sqlService;
    @Value("${zookeeper.client.name}")
    private String regionName;

    public void init() throws Exception {
        System.out.println("ZK init!");
        String list = sqlService.execSelectSql("show tables");
        String[] tableNames = list.trim().split("\n");
        // drop all table
        for (String table : tableNames) {
            if (!table.isEmpty()) {
                sqlService.execCreateAndDropSql("drop table " + table + ";");
            }
        }
        try {
            curatorFramework.delete().forPath("/lss/" + regionName);
        } catch (Exception e) {

        }
        curatorFramework.create().creatingParentsIfNeeded().forPath("/lss/" + regionName);
        System.out.println("ZK init done!");
    }
}
