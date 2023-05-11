package master.service;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import master.pojo.Master;
import master.pojo.Region;
import master.utils.NetUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.zookeeper.common.Time;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author zhuangyifei
 */
@Service
@Data
@AllArgsConstructor
public class ZKService {
    //    public ConcurrentHashMap<String, List<Table>> regionTables;
//    public ConcurrentHashMap<String, List<Region>> tableRegions;
    @Autowired
    NetUtils netUtils;
    @Autowired
    CuratorFramework curatorFramework;
    @Autowired
    List<Region> regions;

    @Value("${server.port}")
    int serverPort;

    public ZKService() {
//        regionTables = new ConcurrentHashMap<>();
//        tableRegions = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() throws Exception {
        registerCallbacks();
        List<String> children = curatorFramework.getChildren().forPath("/lss");
        for (String child : children) {
            byte[] data = curatorFramework.getData().forPath("/lss/" + child);
            Region region = Region.deserializeFromString(new String(data));
            regions.add(region);
        }
        // 注册自己的host和ip
        Master master = new Master();
        master.setHost(java.net.InetAddress.getLocalHost().getHostAddress());
        master.setPort(serverPort);
        curatorFramework.setData().forPath("/master", (master.getHost() + "," + master.getPort()).getBytes());
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


    public JSONObject sendPost(Region region, String path, HashMap<String, String> params) {
        String url = region.getHost() + ":" + region.getPort() + "/" + path;
        return netUtils.sendPost(url, params);
    }

    public void refreshRegion() {
        regions.clear();
        regions.addAll(getAllRegions());
    }

    public List<Region> getRegionsNotHaveTable(String regionName, String tableName) {
        List<Region> regionsNotHaveTable = new ArrayList<>();
        regions.clear();
        regions.addAll(getAllRegions());
        for (Region region : regions) {
            if (!region.getTables().contains(tableName) && !region.getRegionName().equals(regionName)) {
                regionsNotHaveTable.add(region);
            }
        }
        return regionsNotHaveTable;
    }

    public void registerCallbacks() {
        ScheduledExecutorService threadPool = Executors.newScheduledThreadPool(10);

        CuratorCache curatorCache = CuratorCache.build(curatorFramework, "/lss");
        curatorCache.listenable().addListener(CuratorCacheListener.builder()
                .forInitialized(() -> {
                    System.out.println("cache initialize");
                }).forCreates(childData -> {
                    if (childData != null) {
                        regions.clear();
                        regions.addAll(getAllRegions());
                        // 新节点进来，那么添加到regions里面
                        if (childData.getData() == null) {
                            return;
                        }
                        System.out.println("[cache]: create " + new String(childData.getData()));
                        try {
                            Region region = Region.deserializeFromString(new String(childData.getData()));
                            for (Region region1 : regions) {
                                if (region != null && region.equals(region1)) {
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).forDeletes(childData -> {
                    if (childData != null) {
                        System.out.println("[cache]: delete " + new String(childData.getData()));
                        // 节点删除，需要做主从迁移
                        Region region = Region.deserializeFromString(new String(childData.getData()));
                        assert region != null;
                        regions.clear();
                        regions.addAll(getAllRegions());
                        List<String> tables = region.getTables();
                        System.out.println(region);
                        for (var table : tables) {
                            if (table.endsWith("_slave")) {
                                // 选择新的region作为从 region
                                regions.clear();
                                regions.addAll(getAllRegions());
                                Collections.shuffle(regions);
                                for (Region r : regions) {
                                    if (r.getTables().contains(table) || r.getTables().contains(table.substring(0, table.length() - 6))) {
                                        continue;
                                    }
                                    // slave add
                                    HashMap<String, String> map = new HashMap<>();
                                    map.put("tableName", table.substring(0, table.length() - 6));
                                    map.put("region", region.toZKNodeValue());
                                    String url = "http://" + r.getHost() + ":" + r.getPort() + "/dump";
                                    System.out.println(url);
                                    netUtils.sendPost(url, map);
                                    break;
                                }
                            } else {
                                // 这个表是主表，那么做主从切换
                                // 具体来说，就是从region里选一个有这张表的节点，把这个region的这个表设置为主表
                                // 由于之前做了数据同步，这里只需要修改zk里面的数据就可以了
                                for (var r : regions) {
                                    if (r.getTables().contains(table + "_slave")) {
                                        // 找到了一个有这张表的region
                                        r.getTables().remove(table + "_slave");
                                        r.getTables().add(table);
                                        try {
                                            writeRegion(r);
                                            regions.clear();
                                            regions.addAll(getAllRegions());
                                            Collections.shuffle(regions);
                                            for (var r1 : regions) {
                                                if (!r1.getTables().contains(table) && !r1.getTables().contains(table + "_slave")) {
                                                    // slave add
                                                    HashMap<String, String> map = new HashMap<>();
                                                    map.put("tableName", table);
                                                    map.put("region", r.toZKNodeValue());
                                                    String url = "http://" + r1.getHost() + ":" + r1.getPort() + "/dump";
                                                    System.out.println(url);
                                                    netUtils.sendPost(url, map);
                                                    break;
                                                }
                                            }
                                            System.out.println("[registerCallbacks::forDeletes] handle fail, node migration fail in ");
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }).forChanges((oldData, newData) -> {
                    System.out.println("[cache]: change " + new String(oldData.getData()) + " -> " + new String(newData.getData()));
                    Region r1 = Region.deserializeFromString(new String(oldData.getData()));
                    Region r2 = Region.deserializeFromString(new String(newData.getData()));
                    assert r1 != null;
                    assert r2 != null;
                    if (r1.getTables().size() == r2.getTables().size()) {
                        // 主从迁移，不需要修改
                        System.out.println("no need to do");
                        return;
                    } else if (r1.getTables().size() < r2.getTables().size()) {
                        // 有region增加了表
                        String TableName = r2.getTables().get(r2.getTableCount() - 1);
                        if (TableName.endsWith("_slave")) {
                            return;
                        }
                        List<Region> regionsNotHaveTable = getRegionsNotHaveTable(r2.getRegionName(), r2.getTables().get(r2.getTableCount() - 1));
                        List<Integer> randomChoose = new ArrayList<>();
                        Random random = new Random(Time.currentElapsedTime());
                        while (randomChoose.size() != 2) {
                            int index = random.nextInt(regionsNotHaveTable.size());
                            if (!randomChoose.contains(index)) {
                                randomChoose.add(index);
                            }
                        }
                        for (int i = 0; i < randomChoose.size(); i++) {
                            int integer = randomChoose.get(i);
                            Region slaveRegion = regionsNotHaveTable.get(integer);
                            HashMap<String, String> map = new HashMap<>();
                            map.put("tableName", r2.getTables().get(r2.getTableCount() - 1));
                            map.put("region", r2.toZKNodeValue());
                            String url = "http://" + slaveRegion.getHost() + ":" + slaveRegion.getPort() + "/dump";
                            System.out.println(url);
                            netUtils.sendPost(url, map);
                        }
                    } else {
                        // 有regions删除了表
                        // 通知所有相关的region删除这个表
                        System.out.println("Table Deletes");
//                        String TableName = r1.getTables().get(r1.getTableCount() - 1);
                        String TableName = null;
                        for (int i = 0; i < r1.getTables().size(); i++) {
                            if (!r2.getTables().contains(r1.getTables().get(i))) {
                                TableName = r1.getTables().get(i);
                                break;
                            }
                        }
                        System.out.println("TableName" + TableName);
                        if (TableName.endsWith("_slave")) {
                            return;
                        }
                        refreshRegion();
                        List<Region> regionList = new ArrayList<>(regions);
                        for (Region region : regionList) {
                            System.out.println("region: " + region + " TableName: " + TableName);
                            if (region.getTables().contains(TableName + "_slave")) {
                                System.out.println("enter");
                                region.getTables().remove(TableName + "_slave");
                                try {
                                    writeRegion(region);
                                    // 发送drop信息
                                    HashMap<String, String> map = new HashMap<>();
                                    map.put("sql", "drop table " + TableName + ";");
                                    String url = "http://" + region.getHost() + ":" + region.getPort() + "/exec";
                                    System.out.println("[delete first]: " + url + " sql: " + map.get("url"));
                                    netUtils.sendPost(url, map);
                                    String finalTableName = TableName;
                                    threadPool.schedule(() -> {
                                        try {
                                            HashMap<String, String> map1 = new HashMap<>();
                                            map1.put("sql", "drop table " + finalTableName + "_slave;");
                                            String url1 = url;
                                            System.out.println("redelete: " + url1 + " sql: " + map1.get("sql"));
                                            netUtils.sendPost(url1, map1);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }, 3, TimeUnit.SECONDS);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                })
                .build());
        curatorCache.start();
    }

    public Region getRegion(String name) {
        for (var region : regions) {
            if (region.getRegionName().equals(name)) {
                return region;
            }
        }
        return null;
    }

    public Region getRegionInfoByPath(String path) throws Exception {
        byte[] data = curatorFramework.getData().forPath(path);
        return Region.deserializeFromString(new String(data));
    }

    public void writeRegion(Region region) throws Exception {
        curatorFramework.setData().forPath("/lss/" + region.getRegionName(), region.toZKNodeValue().getBytes());
    }


}