package com.zhangyin.region.service;

import com.zhangyin.region.pojo.Region;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ZKService {
    @Autowired
    CuratorFramework curatorFramework;
    //    @Autowired
//    SqlService sqlService;
    @Value("${zookeeper.client.name}")
    private String regionName;
    @Value("${server.port}")
    private int port;
    private static boolean initRegionDone = false;
    @Autowired
    Region region;

    public void init() throws Exception {
        System.out.println("ZK init!");
        // init region info
        region.setPort(port);
        region.setRegionName(regionName);
        region.setTables(new ArrayList<>());
        if (curatorFramework.checkExists().forPath("/lss/" + regionName) != null) {
            curatorFramework.delete().forPath("/lss/" + regionName);
        }
        curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/lss/" + regionName, region.toZKNodeValue().getBytes());
        initRegionDone = true;
        System.out.println("ZK init done!");
    }

    public List<Region> getAllRegions() {
        try {
            List<String> regionNames = curatorFramework.getChildren().forPath("/lss");
            List<Region> regionList = new ArrayList<>();
            for (String regionName : regionNames) {
                byte[] data = curatorFramework.getData().forPath("/lss/" + regionName);
                Region region = Region.deserializeFromString(new String(data));
                regionList.add(region);
            }
            return regionList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void updateNode(Region region) {
        try {
            curatorFramework.setData().forPath("/lss/" + region.getRegionName(), region.toZKNodeValue().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void createTable(String original, String newTable) {
        try {
            Region temp = Region.deserializeFromString(original);
            assert temp != null;

            temp.tableCount++;
            temp.tables.add(newTable);

            curatorFramework.setData().forPath("/lss/" + regionName, temp.toZKNodeValue().getBytes());
            region.copyFrom(temp.toZKNodeValue());
            System.out.println("createTable: " + region);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dropTable(String original, String victim) {
        try {
            Region temp = Region.deserializeFromString(original);
            assert temp != null;

            temp.tableCount--;
            temp.tables = new ArrayList<>(temp.getTables().stream().filter(table -> {
                return !table.equals(victim) && !table.equals(victim + "_slave");
            }).toList());

            updateNode(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
