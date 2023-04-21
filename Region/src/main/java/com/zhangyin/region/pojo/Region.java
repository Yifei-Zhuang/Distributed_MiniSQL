package com.zhangyin.region.pojo;

import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Region {
    public String host = "127.0.0.1";
    public String regionName;
    public int port;
    public ArrayList<Table> tables;

    public void copyFrom(JSONObject javaObject) {
        this.host = (String) javaObject.get("host");
        this.regionName = (String) javaObject.get("regionName");
        this.port = (Integer) javaObject.get("port");
        this.tables = (ArrayList<Table>) javaObject.getJSONArray("tables").toJavaList(Table.class);
    }
}
