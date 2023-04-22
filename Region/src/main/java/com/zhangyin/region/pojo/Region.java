package com.zhangyin.region.pojo;

import com.zhangyin.region.utils.NetUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

@Data
@AllArgsConstructor
public class Region {
    public String host;
    public String regionName;
    public int port;
    public int tableCount;
    public ArrayList<String> tables;

    public String toZKNodeValue() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(host);
        stringBuilder.append(",");
        stringBuilder.append(regionName);
        stringBuilder.append(",");
        stringBuilder.append(port);
        stringBuilder.append(",");
        stringBuilder.append(tableCount);
        stringBuilder.append(",");
        for (String table : tables) {
            stringBuilder.append(table);
            stringBuilder.append(",");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    public void copyFrom(String regionInfo) {
        try {
            String[] temp = regionInfo.split(",");
            host = temp[0];
            regionName = temp[1];
            port = Integer.parseInt(temp[2]);
            tableCount = Integer.parseInt(temp[3]);
            tables = new ArrayList<>();
            tables.addAll(Arrays.asList(temp).subList(4, temp.length));
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.out.println("[Region::copyFrom]: init region from info failed");
        }
    }

    public static Region deserializeFromString(String regionInfo) {
        try {
            Region region = new Region();
            String[] temp = regionInfo.split(",");
            region.host = temp[0];
            region.regionName = temp[1];
            region.port = Integer.parseInt(temp[2]);
            region.tableCount = Integer.parseInt(temp[3]);
            region.tables = new ArrayList<>();
            region.tables.addAll(Arrays.asList(temp).subList(4, temp.length));
            return region;
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.out.println("[Region::copyFrom]: init region from info failed");
            return null;
        }
    }

    public Region() {
        try {
            this.host = NetUtils.getIP();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean containsTable(String tableName) {
        return tables.contains(tableName);
    }
}
