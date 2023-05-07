package com.zhangyin.region.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
    private static final String USER = "minisql";
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
//        String list = execSelectSql("show tables");
//        String[] tableNames = new String[0];
//        if (list != null) {
//            tableNames = list.trim().split("\n");
//        }
//        // drop all table
//        for (int i = 1; i < tableNames.length; i++) {
//            String table = tableNames[i];
//            if (!table.isEmpty()) {
//                execCreateAndDropSql("drop table " + table + ";");
//            }
//        }
//        region.setTableCount(0);
//        zkService.updateNode(region);

    }

    public void init() throws SQLException {
        try {
            Connection c = getConnection();
            Statement s = c.createStatement();
            s.execute("drop database if exists " + databaseName);
            s.execute("create database if not exists " + databaseName);
            s.close();
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public Connection getConnectionWithoutDatabase() {
        System.out.println("databaseName:" + databaseName);
        Connection conn = null;
        System.out.println("JDBC_URL: " + JDBC_URL);
        try {
            conn = DriverManager.getConnection(JDBC_URL.substring(0, JDBC_URL.length() - 1), USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
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
            if ("insert".equalsIgnoreCase(temp[0]) || "delete".equalsIgnoreCase(temp[0])) {
                tableName = temp[2];
            } else {
                tableName = temp[1];
            }
            boolean master = false;
            // 如果当前为master，进行主从同步
            for (String table : region.getTables()) {
                if (table.equalsIgnoreCase(tableName)) {
                    master = true;
                    break;
                }
            }
            if (!master) {
                return rowCount;
            }
            List<Region> regions = zkService.getAllRegions();
            for (Region pointer : regions) {
                // 遍历所有的slave region，发送sql请求
                if (pointer.getRegionName().equalsIgnoreCase(region.regionName) || !pointer.containsTable(tableName + "_slave")) {
                    continue;
                }
                // 对slave发送请求
                String finalTableName = tableName;
                String slaveUrl = pointer.getHost() + ":" + pointer.getPort() + "/exec";
                HashMap<String, String> map = new HashMap<>();
                map.put("sql", sql);
                map.put("tableName", finalTableName);
                System.out.println(netUtils.sendPost(slaveUrl, map));

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
        // TODO 修改返回格式
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
//            StringBuilder stringBuilder = new StringBuilder();
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            List<String> columnNames = new ArrayList<>();
            for (int i = 0; i < count; i++) {
//                stringBuilder.append(resultSetMetaData.getColumnName(i + 1));
//                stringBuilder.append(" ");
                columnNames.add(resultSetMetaData.getColumnName(i + 1));
                jsonArray.add(resultSetMetaData.getColumnName(i + 1));
            }
            jsonObject.put("fields", jsonArray);
//            stringBuilder.append('\n');
            jsonArray = new JSONArray();
            while (rs.next()) {
                JSONObject temp = new JSONObject();
                for (int i = 1; i <= count; i++) {
                    String columnName = resultSetMetaData.getColumnName(i);
                    if (resultSetMetaData.getColumnType(i) == java.sql.Types.ARRAY) {
                        temp.put(columnName, rs.getArray(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.BIGINT) {
                        temp.put(columnName, rs.getInt(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.BOOLEAN) {
                        temp.put(columnName, rs.getBoolean(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.BLOB) {
                        temp.put(columnName, rs.getBlob(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.DOUBLE) {
                        temp.put(columnName, rs.getDouble(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.FLOAT) {
                        temp.put(columnName, rs.getFloat(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.INTEGER) {
                        temp.put(columnName, rs.getInt(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.NVARCHAR) {
                        temp.put(columnName, rs.getNString(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.VARCHAR) {
                        temp.put(columnName, rs.getString(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.TINYINT) {
                        temp.put(columnName, rs.getInt(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.SMALLINT) {
                        temp.put(columnName, rs.getInt(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.DATE) {
                        temp.put(columnName, rs.getDate(columnName));
                    } else if (resultSetMetaData.getColumnType(i) == java.sql.Types.TIMESTAMP) {
                        temp.put(columnName, rs.getTimestamp(columnName));
                    } else {
                        temp.put(columnName, rs.getObject(columnName));
                    }
                }
                jsonArray.add(temp);
            }
            jsonObject.put("data", jsonArray);
            return jsonObject.toJSONString();
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
            if ("if".equalsIgnoreCase(temp[2])) {
                // drop table if exist a;
                if ("not".equalsIgnoreCase(temp[3])) {
                    tableName = temp[5];
                } else {
                    tableName = temp[4];
                }
            }
            if (tableName.endsWith(";")) {
                tableName = tableName.substring(0, tableName.length() - 1);
            }
            if (tableName.endsWith("`")) {
                tableName = tableName.substring(1, tableName.length() - 1);
            }
            if (tableName.endsWith(")")) {
                tableName = tableName.substring(1, tableName.length() - 1);
            }
            if (tableName.endsWith("`")) {
                tableName = tableName.substring(1, tableName.length() - 1);
            }
            if (tableName.endsWith(")")) {
                tableName = tableName.substring(1, tableName.length() - 1);
            }
            // 查询表是否已经存在
            List<Region> regions = zkService.getAllRegions();
            if ("create".equalsIgnoreCase(type)) {
                for (Region pointer : regions) {
                    if (pointer.containsTable(tableName) || pointer.containsTable(tableName + "_slave")) {
                        throw new SQLException("表已经存在");
                    }
                }
            }
            boolean result = ps.execute(sql);
            if ("create".equalsIgnoreCase(type)) {
                zkService.createTable(region.toZKNodeValue(), tableName);
            } else if ("drop".equalsIgnoreCase(type)) {
                zkService.dropTable(region.toZKNodeValue(), tableName);
            }
            return result;
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}


