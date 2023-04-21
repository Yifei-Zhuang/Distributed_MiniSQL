package master.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import master.pojo.Region;
import master.pojo.Table;
import master.utils.JSONUtil;
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

/**
 * @author zhuangyifei
 */
@Service
@Data
@AllArgsConstructor
public class ZKService {
    public ConcurrentHashMap<String, List<Table>> regionTables;
    public ConcurrentHashMap<String, List<Region>> tableRegions;
    @Autowired
    NetUtils netUtils;
    @Autowired
    CuratorFramework curatorFramework;

    @Autowired
    JSONUtil jsonUtil;
    @Autowired
    List<Region> regions;

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
            Region region = ((JSONObject) JSON.parse(new String(data))).toJavaObject(Region.class);
            region.setTables((ArrayList<Table>) jsonUtil.JSONArrayToTableArray((JSONArray) ((Object) region.getTables())));
            regions.add(region);
            regionTables.put(region.getRegionName(), new ArrayList<>(region.getTables()));
            for (Table table : region.getTables()) {
                if (!tableRegions.containsKey(table.getName())) {
                    tableRegions.put(table.getName(), new ArrayList<>());
                }
                tableRegions.get(table.getName()).add(region);
            }
        }
    }

    public JSONObject sendPost(Region region, String path, HashMap<String, String> params) {
        String url = "http://127.0.0.1:" + region.getPort() + "/" + path;
        return netUtils.sendPost(url, params);
    }

    public void registerCallbacks() {
        CuratorCache curatorCache = CuratorCache.build(curatorFramework, "/lss");
        curatorCache.listenable().addListener((e, oldData, newData) -> {
            //TODO 分布式锁
            if (e.equals(CuratorCacheListener.Type.NODE_DELETED)) {
                String[] strs = oldData.getPath().split("/");
                String regionName = strs[strs.length - 1];
                List<Table> tableOfRegions = regionTables.get(regionName);
                regions.remove(getRegion(regionName));
                regionTables.remove(regionName);
                for (Table table : tableOfRegions) {
                    tableRegions.get(table.getName()).remove(getRegion(regionName));
                }
                for (Table table : tableOfRegions) {
                    String anotherRegionName = getAnotherRegionHasTheSameTable(regionName, table.getName());
                    if (anotherRegionName == null) {
                        throw new RuntimeException("该表只有一个副本集，异常");
                    }
                    //TODO 判断是不是master表，如果是，那么进行主从切换，然后更新zk的主从信息

                    // dump
                    String currentDir = System.getProperty("user.dir");
                    Region region = getRegion(anotherRegionName);
                    HashMap<String, String> params = new HashMap<>();
                    params.put("tableName", table.getName());
                    params.put("path", currentDir + "/" + table.getName() + ".sql");
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
                        for (Table curtable : regionTables.get(region1.getRegionName())) {
                            if (curtable.getName().equals(table.getName())) {
                                // 已经有两个表，break
                                tag = true;
                                break;
                            }
                        }
                        if (tag) {
                            break;
                        } else {
                            HashMap<String, String> param = new HashMap<>();
                            param.put("tableName", table.getName());
                            param.put("path", currentDir + "/" + table.getName() + ".sql");
                            sendPost(region1, "import", param);
                            //TODO 更新ZK
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
                List<Table> cur = regionTables.get(key);
                for (Table table : cur) {
                    if (table.getName().equals(tableName)) {
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

    public Region getRegionInfoByPath(String path) throws Exception {
        byte[] data = curatorFramework.getData().forPath(path);
        return ((JSONObject) JSON.parse(new String(data))).toJavaObject(Region.class);
    }

    public void writeRegion(Region region) throws Exception {
        curatorFramework.setData().forPath("/lss/" + region.getRegionName(), JSON.toJSONString(region).getBytes());
    }


}