package master.service;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import master.pojo.Region;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Data
@AllArgsConstructor
public class ZKService {
    public ConcurrentHashMap<String, String[]> regionTables;
    @Autowired
    CuratorFramework curatorFramework;

    @Autowired
    List<Region> regions;

    public ZKService() {
        regionTables = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void init() throws Exception {
        registerCallbacks();
    }

    //TODO 封装
    public JSONObject sendPost(Region region, String path, String... params) {
        assert region != null;
        String url = "http://127.0.0.1:" + region.getPort() + "/" + path;
        JSONObject paramMap = new JSONObject();
        for (int i = 0; i < params.length; i += 2) {
            paramMap.put(params[i], params[i + 1]);
        }
        RestTemplate client = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JSONObject> httpEntity = new HttpEntity<JSONObject>(paramMap, headers);
        return client.postForEntity(url, httpEntity, JSONObject.class).getBody();
    }

    public void registerCallbacks() {
        CuratorCache curatorCache = CuratorCache.build(curatorFramework, "/lss");
        curatorCache.listenable().addListener((e, oldData, newData) -> {
            //TODO 分布式锁
            if (e.equals(CuratorCacheListener.Type.NODE_DELETED)) {
                String[] strs = oldData.getPath().split("/");
                String regionName = strs[strs.length - 1];
                String[] tableOfReginons = regionTables.get(regionName);
                for (String table : tableOfReginons) {
                    String anotherRegionName = getAnotherRegionHasTheSameTable(regionName, table);
                    if (anotherRegionName == null) {
                        throw new RuntimeException("该表只有一个副本集，异常");
                    }
                    // dump
                    String currentDir = System.getProperty("user.dir");
                    Region region = getRegion(anotherRegionName);
                    System.out.println(sendPost(region, "dump", "tableName", table, "path", currentDir + "/" + table + ".sql"));
                    // 随机挑选
                    while (true) {
                        Random random = new Random();
                        int index = random.nextInt();
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
                            sendPost(region1, "import", "tableName", table, "path", currentDir + "/" + table + ".sql");
                            break;
                        }
                    }
                }
            } else if (e.equals(CuratorCacheListener.Type.NODE_CREATED)) {
                // 加到map里
                String[] strs = oldData.getPath().split("/");
                String regionName = strs[strs.length - 1];
                regionTables.put(regionName, new String[0]);
            }
        });
    }


    public String getAnotherRegionHasTheSameTable(String exclude, String tableName) {
        for (var key : regionTables.keySet()) {
            if (!key.equals(exclude)) {
                String[] cur = regionTables.get(key);
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