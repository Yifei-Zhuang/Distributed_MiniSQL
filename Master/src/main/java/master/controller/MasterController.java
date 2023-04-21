package master.controller;

import master.pojo.*;
import master.service.ZKService;
import master.utils.JSONUtil;
import master.utils.NetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class MasterController {
    @Autowired
    ZKService zkService;
    @Autowired
    List<Region> regions;
    @Autowired
    NetUtils netUtils;
    @Autowired
    JSONUtil jsonUtil;
    private final String REGION_HOST = "http://127.0.0.1";

    @GetMapping("/regionInfo")
    public List<Region> getRegionInfo() {
        return regions;
    }

    @PostMapping("/exec")
    public Response execSQL(@RequestBody SQL sql) throws Exception {
        String[] arr = sql.sql.trim().split(" ");
        String type = arr[0];
        if ("create".equals(type)) {
            if ("database".equals(arr[1])) {
                // create database
                // not implement, for we just create database on region startup
                return Response.fail("cannot create database");
            } else if ("table".equals(arr[1])) {
                // create table
                // check whether this table exist or not
                if (zkService.tableRegions.get(sql.tableName) != null) {
                    return Response.fail("table already exists!");
                }
                ArrayList<Integer> list = new ArrayList<>();
                // one Master and two Slave for each table
                ThreadLocalRandom.current().ints(0, regions.size()).distinct().limit(3).forEach(list::add);
                ArrayList<Region> relatedRegions = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    int index = list.get(i);
                    // send
                    // TODO 将主从的信息同步到zk里面去
                    String url = REGION_HOST + ":" + regions.get(index).getPort() + "/exec";
                    HashMap<String, String> map = new HashMap<>();
                    map.put("sql", sql.sql);
                    Response res = netUtils.sendPost(url, map).toJavaObject(Response.class);
                    if (res.getStatusCode() == StatusCode.FAIL) {
                        return Response.fail(res.msg);
                    }
                    // sync with zk
                    Region region = zkService.getRegion(regions.get(index).getRegionName());
                    List<Table> tableList = region.getTables();
                    System.out.println(new Table(sql.tableName, i == 0));
                    tableList.add(new Table(sql.tableName, i == 0));
                    try {
                        zkService.writeRegion(region);
                        relatedRegions.add(regions.get(index));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                zkService.tableRegions.put(sql.tableName, relatedRegions);
                return Response.success();
            } else {
                return Response.fail("invalid sql");
            }
        } else if ("drop".equals(type)) {
            if ("database".equals(arr[1])) {
                // drop database
                // not implement
                return Response.fail("cannot drop database");
            } else if ("table".equals(arr[1])) {
                // drop table
                if (zkService.tableRegions.get(sql.tableName) == null) {
                    return Response.fail("table not exists!");
                }
                List<Region> relatedRegions = zkService.tableRegions.get(sql.tableName);
                for (int i = 0; i < relatedRegions.size(); i++) {
                    // send
                    String url = REGION_HOST + ":" + relatedRegions.get(i).getPort() + "/exec";
                    HashMap<String, String> map = new HashMap<>();
                    map.put("sql", sql.sql);
                    Response res = netUtils.sendPost(url, map).toJavaObject(Response.class);
                    if (res.getStatusCode() == StatusCode.FAIL) {
                        return Response.fail(res.msg);
                    }
                    // sync with zk
                    Region region = relatedRegions.get(i);
                    List<Table> tableList = region.getTables();
                    tableList.removeIf(table -> table.getName().equals(sql.tableName));
                    zkService.regionTables.put(region.getRegionName(), tableList);
                    try {
                        zkService.writeRegion(region);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                zkService.tableRegions.remove(sql.tableName);
            } else {
                return Response.fail("invalid sql");
            }
        } else {
            List<Region> relatedRegions = zkService.tableRegions.get(sql.tableName);
            if (relatedRegions == null || relatedRegions.isEmpty()) {
                return Response.fail("不存在该表");
            }
            String result = null;
            for (Region region : relatedRegions) {
                // 只对master region发送请求
                List<Table> tableList = region.getTables();
                boolean isMaster = false;
                for (Table table : tableList) {
                    if (table.getName().equals(sql.tableName) && table.isMasterTable()) {
                        isMaster = true;
                        break;
                    }
                }
                if (!isMaster) {
                    continue;
                }
                String url = REGION_HOST + ":" + region.getPort() + "/exec";
                HashMap<String, String> body = new HashMap<>();
                body.put("sql", sql.sql);
                var jsonResult = netUtils.sendPost(url, body);
                System.out.println(jsonResult);
                if (result == null) {
                    result = jsonResult.toJSONString();
                }
            }
            return Response.success(result);
        }
        return Response.fail("Cannot exec sql");
    }
}
