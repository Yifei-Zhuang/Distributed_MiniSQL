package com.zhangyin.region.utils;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.zhangyin.region.pojo.Region;
import com.zhangyin.region.pojo.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
@Component
public class ListenDB implements Runnable {

    /**
     * 要监听的数据库的主机地址
     */
    @Value("${canal-monitor-mysql.hostname}")
    private String canalMonitorHost;

    /**
     * canal端口号，这个是固定的用：11111
     */
    @Value("${canal-monitor-mysql.port}")
    private Integer canalMonitorPort;

    /**
     * canal的example，这个值是固定的用：example
     */
    @Value("${canal-monitor-mysql.example}")
    private String canalExample;

    /**
     * 要监听的数据库名和表名
     */
    @Value("${canal-monitor-mysql.tableName}")
    private String canalMonitorTableName;

    @Autowired
    Region region;

    @Autowired
    ConcurrentHashMap<String, List<Region>> tableRegions;

    @Autowired
    NetUtils netUtils;

    @Override
    public void run() {
//        // sync table here
//        CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress(canalMonitorHost, canalMonitorPort), canalExample, "", "");
//        int batchSize = 1000;
//        try {
//            connector.connect();
//            System.out.println("数据库检测连接成功：" + canalMonitorTableName);
//            connector.subscribe(canalMonitorTableName);
//            connector.rollback();
//            try {
//                while (true) {
//                    //尝试从master那边拉去数据batchSize条记录，有多少取多少
//                    Message message = connector.getWithoutAck(batchSize);
//                    long batchId = message.getId();
//                    int size = message.getEntries().size();
//                    if (batchId == -1 || size == 0) {
//                        //每隔一秒监听一次
//                        Thread.sleep(1000);
//                    } else {
//                        dataHandle(message.getEntries());
//                    }
//                    connector.ack(batchId);
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (InvalidProtocolBufferException e) {
//                e.printStackTrace();
//            }
//        } finally {
//            connector.disconnect();
//        }
    }

    private void dataHandle(List<CanalEntry.Entry> entrys) throws InvalidProtocolBufferException {
        for (CanalEntry.Entry entry : entrys) {
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            if (CanalEntry.EntryType.ROWDATA == entry.getEntryType()) {
                CanalEntry.EventType eventType = rowChange.getEventType();
                if (eventType == CanalEntry.EventType.DELETE) {
                    //删除，返回删除的sql语句，然后在mapper中，直接执行这句sql
                    String sql = saveDeleteSql(entry);
                    if (sql != null) {
                        resendSQL(sql, "delete", entry.getHeader().getTableName());
                    }
                } else if (eventType == CanalEntry.EventType.UPDATE) {
                    //更新，返回更新的sql语句，然后在mapper中，直接执行这句sql
                    String sql = saveUpdateSql(entry);
                    if (sql != null) {
                        resendSQL(sql, "update", entry.getHeader().getTableName());
                    }
                } else if (eventType == CanalEntry.EventType.INSERT) {
                    //新增，返回新增的sql语句，然后在mapper中，直接执行这句sql
                    String sql = saveInsertSql(entry);
                    if (sql != null) {
                        resendSQL(sql, "insert", entry.getHeader().getTableName());
                    }
                }
            }
        }
    }

    private String saveUpdateSql(CanalEntry.Entry entry) {
        try {
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            List<CanalEntry.RowData> rowDataList = rowChange.getRowDatasList();
            return rowChange.getSql();
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    /**
     * 保存删除语句
     */
    private String saveDeleteSql(CanalEntry.Entry entry) {
        try {
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            List<CanalEntry.RowData> rowDataList = rowChange.getRowDatasList();
            return rowChange.getSql();
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    /**
     * 保存插入语句
     */
    private String saveInsertSql(CanalEntry.Entry entry) {
        try {
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            List<CanalEntry.RowData> rowDataList = rowChange.getRowDatasList();
            System.out.println(rowChange.getSql());
            return rowChange.getSql();
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    private void resendSQL(String sql, String type, String tableName) {
        // 检查当前region是否是master
        // TODO 转发sql
        try {
            List<Table> tables = new ArrayList<>(region.getTables());
            //check whether is master
            for (Table table : tables) {
                if (table.getName().equals(tableName)) {
                    if (!table.isMasterTable()) {
                        return;
                    }
                }
            }
            // 转发
            List<Region> slaveRegions = new ArrayList<>(tableRegions.get(tableName));
            for (Region move : slaveRegions) {
                // TODO 转发sql
                if (move.getRegionName().equals(region.getRegionName())) {
                    continue;
                }
                String url = move.getHost() + ":" + move.getPort() + "/exec";
                HashMap<String, String> body = new HashMap<>();
                body.put("sql", sql);
                var jsonResult = netUtils.sendPost(url, body);
                System.out.println("转发sql到：" + url + ":" + sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
