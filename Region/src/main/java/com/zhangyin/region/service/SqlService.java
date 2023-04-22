package com.zhangyin.region.service;

import com.zhangyin.region.pojo.Region;
import com.zhangyin.region.utils.NetUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SqlService {

    private static final String JDBC_URL = "jdbc:mysql://127.0.0.1:3306/";
    private static final String USER = "root";
    private static final String PASSWORD = "mysql";
    @Value("${zookeeper.client.name}")
    private String databaseName;

    @Autowired
    NetUtils netUtils;

    @Autowired
    ZKService zkService;

    @Autowired
    Region region;

    @PostConstruct
    public void initMethod() throws SQLException {
        String list = execSelectSql("show tables");
        String[] tableNames = new String[0];
        if (list != null) {
            tableNames = list.trim().split("\n");
        }
        // drop all table
        for (int i = 1; i < tableNames.length; i++) {
            String table = tableNames[i];
            if (!table.isEmpty()) {
                execCreateAndDropSql("drop table " + table + ";");
            }
        }
        region.setTableCount(0);
        zkService.updateNode(region);

    }

    public Connection getConnection() {
        System.out.println("databaseName:" + databaseName);
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(JDBC_URL + databaseName, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public Connection getConnectionByInfos(String url, String user, String password) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public int execUpdateOrInsertOrDeleteSqls(String sql) throws SQLException {
        Connection conn = getConnection();
        ExecutorService es = Executors.newCachedThreadPool();
        if (conn == null) {
            System.err.println("获取数据库连接失败，请重试");
            return 0;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql);) {
            System.out.println("sql:" + ps.toString());
            int rowCount = ps.executeUpdate(sql);
            String tableName = null;
            String[] temp = sql.trim().split(" ");
            if ("insert".equals(temp[0])) {
                tableName = temp[2];
            } else {
                tableName = temp[1];
            }
            // 当前为master，进行主从同步
            List<Region> regions = zkService.getAllRegions();
            for (Region pointer : regions) {
                // 遍历所有的slave region，发送sql请求
                if (pointer.getRegionName().equals(region.regionName) || !pointer.containsTable(tableName)) {
                    continue;
                }
                // 对slave发送请求
                String finalTableName = tableName;
                es.submit(() -> {
                    String slaveUrl = pointer.getHost() + ":" + pointer.getHost() + "/exec";
                    HashMap<String, String> map = new HashMap<>();
                    map.put("sql", sql);
                    map.put("tableName", finalTableName);
                    System.out.println(netUtils.sendPost(slaveUrl, map));
                    return 0;
                });
            }
            return rowCount;
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public String execSelectSql(String sql) throws SQLException {
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("获取数据库连接失败，请重试");
            return null;
        }
        List<Map<String, Object>> resultList = new ArrayList<>();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery(sql);
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            int count = resultSetMetaData.getColumnCount();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < count; i++) {
                stringBuilder.append(resultSetMetaData.getColumnName(i + 1));
                stringBuilder.append(" ");
            }
            stringBuilder.append('\n');
            while (rs.next()) {
                stringBuilder.append(rs.getString(1));
                for (int i = 2; i <= count; i++) {
                    stringBuilder.append(" ");
                    stringBuilder.append(rs.getString(i));
                }
                stringBuilder.append("\n");
            }
            return stringBuilder.toString();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean execCreateAndDropSql(String sql) throws SQLException {
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("获取数据库连接失败，请重试");
            return false;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql);) {
            String[] temp = sql.trim().split(" ");
            String type = temp[0];
            String tableName = temp[2];
            // 查询表是否已经存在
            List<Region> regions = zkService.getAllRegions();
            for (Region pointer : regions) {
                if (pointer.containsTable(tableName) || pointer.containsTable(tableName + "_slave")) {
                    throw new SQLException("表已经存在");
                }
            }
            boolean result = ps.execute(sql);
            if ("create".equals(type)) {
                zkService.createTable(region.toZKNodeValue(), tableName);
            } else {
                zkService.dropTable(region.toZKNodeValue(), tableName);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}


