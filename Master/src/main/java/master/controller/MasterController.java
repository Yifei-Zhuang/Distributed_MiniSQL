package master.controller;

import com.alibaba.fastjson2.JSONObject;
import master.pojo.*;
import master.service.ZKService;
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
    List<RegionWithTables> regions;
    @Autowired
    NetUtils netUtils;
    private final String REGION_HOST = "http://127.0.0.1";

    @GetMapping("/regionInfo")
    public List<RegionWithTables> getRegionInfo() {
        ArrayList<RegionWithTables> arrayList = new ArrayList<>();
        for (Region region : regions) {
            arrayList.add(new RegionWithTables(region.regionName, region.databaseName, region.port,
                    zkService.regionTables.get(region.getRegionName())));
        }
        return arrayList;
    }

    @PostMapping("/exec")
    public Response execSQL(@RequestBody SQL sql) {
        String[] arr = sql.sql.trim().split(" ");
        String type = arr[0];
        if ("create".equals(type)) {
            if ("database".equals(arr[1])) {
                // create database
                // not implement, for we just create database on region startup
                return Response.fail("cannot create database");
            } else if ("table".equals(arr[1])) {
                // create table
                // load balancing
                // get region with random
                // check whether this table exist or not

                if (zkService.tableRegions.get(sql.tableName) != null) {
                    return Response.fail("table already exists!");
                }
                ArrayList<Integer> list = new ArrayList<>();
                ThreadLocalRandom.current().ints(0, regions.size()).distinct().limit(2).forEach(list::add);
                ArrayList<RegionWithTables> relatedRegions = new ArrayList<>();
                for (int i : list) {
                    // send
                    String url = REGION_HOST + ":" + regions.get(i).getPort() + "/exec";
                    HashMap<String, String> map = new HashMap<>();
                    map.put("sql", sql.sql);
                    Response res = netUtils.sendPost(url, map).toJavaObject(Response.class);
                    if (res.getStatusCode() == StatusCode.FAIL) {
                        return Response.fail(res.msg);
                    }
                    relatedRegions.add(regions.get(i));
                }
                zkService.tableRegions.put(sql.tableName, relatedRegions);
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
                ArrayList<RegionWithTables> relatedRegions = zkService.tableRegions.get(sql.tableName);
                relatedRegions.forEach(region -> {
                    // send
                    String url = REGION_HOST + ":" + region.getPort() + "/exec";
                    HashMap<String, String> map = new HashMap<>();
                    map.put("sql", sql.sql);
                    JSONObject res = netUtils.sendPost(url, map);
                    System.out.println(res);
                });
                zkService.tableRegions.remove(sql.tableName);
            } else {
                return Response.fail("invalid sql");
            }
        } else {
            ArrayList<RegionWithTables> relatedRegions = zkService.tableRegions.get(sql.tableName);
            if (relatedRegions.isEmpty()) {
                return Response.fail("不存在该表");
            }
            String result = null;
            for (Region region : relatedRegions) {
                // 对region发送请求
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
