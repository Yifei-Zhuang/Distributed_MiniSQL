package com.zhangyin.region.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SqlService {

    private static final String JDBC_URL = "jdbc:mysql://127.0.0.1:3306/";
    private static final String USER = "root";
    private static final String PASSWORD = "mysql";
    @Value("${zookeeper.client.name}")
    private String databaseName;

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

    public int execUpdateOrInsertOrDeleteSqls(String sql) throws SQLException {
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("获取数据库连接失败，请重试");
            return 0;
        }
        try (PreparedStatement ps = conn.prepareStatement(sql);) {
            return ps.executeUpdate(sql);
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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
            while (rs.next()) {
                stringBuilder.append(rs.getString(1));
                for (int i = 2; i <= count; i++) {
                    stringBuilder.append("\t");
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
            return ps.execute(sql);
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}


