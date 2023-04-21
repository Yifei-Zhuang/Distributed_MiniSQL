package master.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import master.pojo.Region;
import master.pojo.RegionWithTables;
import master.utils.NetUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Data
@AllArgsConstructor
public class ZKService {
    public ConcurrentHashMap<String, ArrayList<String>> regionTables;
    public ConcurrentHashMap<String, ArrayList<RegionWithTables>> tableRegions;
    @Autowired
    NetUtils netUtils;
    @Autowired
    CuratorFramework curatorFramework;

    @Autowired
    List<RegionWithTables> regions;

    public ZKService() {
        regionTables = new ConcurrentHashMap<>();
        tableRegions = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() throws Exception {
        registerCallbacks();
        List<String> children = curatorFramework.getChildren().forPath("/lss");
        for (String child : children) {
            byte[] data = curatorFramework.getData().forPath("/lss/" + child);
            RegionWithTables region = ((JSONObject) JSON.parse(new String(data))).toJavaObject(RegionWithTables.class);
            regions.add(region);
            regionTables.put(region.getRegionName(), new ArrayList<>(region.getTables()));
            for (String table : region.getTables()) {
                if (tableRegions.containsKey(table)) {
                    tableRegions.put(table, new ArrayList<>());
                }
                tableRegions.get(table).add(region);
            }
        }
    }

    public JSONObject sendPost(Region region, String path, HashMap<String, String> params) {
        String url = "http://127.0.0.1:" + region.getPort() + "/" + path;
        return netUtils.sendPost(url, params);
    }

    //TODO 处理节点添加的情况
    public void registerCallbacks() {
        CuratorCache curatorCache = CuratorCache.build(curatorFramework, "/lss");
        curatorCache.listenable().addListener((e, oldData, newData) -> {
            //TODO 分布式锁
            if (e.equals(CuratorCacheListener.Type.NODE_DELETED)) {
                String[] strs = oldData.getPath().split("/");
                String regionName = strs[strs.length - 1];
                ArrayList<String> tableOfReginons = regionTables.get(regionName);
                for (String table : tableOfReginons) {
                    String anotherRegionName = getAnotherRegionHasTheSameTable(regionName, table);
                    if (anotherRegionName == null) {
                        throw new RuntimeException("该表只有一个副本集，异常");
                    }
                    // dump
                    String currentDir = System.getProperty("user.dir");
                    Region region = getRegion(anotherRegionName);
                    HashMap<String, String> params = new HashMap<>();
                    params.put("tableName", table);
                    params.put("path", currentDir + "/" + table + ".sql");
                    System.out.println(sendPost(region, "dump", params));
                    // 随机挑选
                    while (true) {
                        Random random = new Random();
                        int index = random.nextInt(regions.size());
                        Region region1 = regions.get(index);
                        if (region1.getRegionName().equals(anotherRegionName) || region1.getRegionName().equals(regionName)) {
                            continue;
                        }
                        boolean tag = false;
                        for (String curtable : regionTables.get(region1.getRegionName())) {
                            if (curtable.equals(table)) {
                                // 已经有两个表，break
                                tag = true;
                                break;
                            }
                        }
                        if (tag) {
                            break;
                        } else {
                            HashMap<String, String> param = new HashMap<>();
                            param.put("tableName", table);
                            param.put("path", currentDir + "/" + table + ".sql");
                            sendPost(region1, "import", param);
                            break;
                        }
                    }
                }
            } else if (e.equals(CuratorCacheListener.Type.NODE_CREATED)) {
                // 加到map里
                String[] strs = oldData.getPath().split("/");
                String regionName = strs[strs.length - 1];
                regionTables.put(regionName, new ArrayList<>());
                System.out.println(newData);
                // TODO 向regions中添加对应的region信息
            }
        });
    }


    public String getAnotherRegionHasTheSameTable(String exclude, String tableName) {
        for (var key : regionTables.keySet()) {
            if (!key.equals(exclude)) {
                ArrayList<String> cur = regionTables.get(key);
                for (String a : cur) {
                    if (a.equals(tableName)) {
                        return key;
                    }
                }
            }
        }
        return null;
    }

    public Region getRegion(String name) {
        for (var region : regions) {
            if (region.getRegionName().equals(name)) {
                return region;
            }
        }
        return null;
    }
}