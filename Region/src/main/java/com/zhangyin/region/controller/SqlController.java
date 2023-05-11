package com.zhangyin.region.controller;

import com.zhangyin.region.pojo.MyResponse;
import com.zhangyin.region.pojo.Region;
import com.zhangyin.region.service.SqlService;
import com.zhangyin.region.service.ZKService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;


@CrossOrigin
@RestController
public class SqlController {
    @Autowired
    Environment environment;

    @Autowired
    SqlService sqlService;

    @Autowired
    ZKService zkService;

    @Autowired
    Region region;

    @Value("${local.path}")
    String localPath;

    @PostMapping("/exec")
    public MyResponse execSQL(@RequestBody Map<String, String> map, HttpServletResponse httpServletResponse) {
        String sql = map.get("sql");
        System.out.println("[execSQL]: " + sql);
        if (sql == null) {
            return MyResponse.fail("missing sql", "exception");
        }
        try {
            String type = sql.split(" ")[0];
            if ("insert".equalsIgnoreCase(type) || "delete".equalsIgnoreCase(type) || "update".equalsIgnoreCase(type)) {
                sqlService.execUpdateOrInsertOrDeleteSqls(sql);
                return MyResponse.success(type);
            } else if ("create".equalsIgnoreCase(type) || "drop".equalsIgnoreCase(type)) {
                sqlService.execCreateAndDropSql(sql);
                return MyResponse.success(type);
            } else {
                return MyResponse.success(sqlService.execSelectSql(sql), type);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return MyResponse.fail(e.toString());
        }
    }

    @PostMapping("/dump")
    public void dump(@RequestBody Map<String, String> map) throws IOException {
        /*
         * map = {
         *   "region": region String
         *   "tableName": tableName
         * }
         * */
        String tableName = map.get("tableName");
        String regionString = map.get("region");
        Region masterRegion = Region.deserializeFromString(regionString);
        assert masterRegion != null;
        String url = "mysqldump -uminisql -h" + masterRegion.getHost() + " -pmysql " + masterRegion.getRegionName() + " " + tableName + " > " + tableName + ".sql";
        System.out.println("[dump] mysqldump url: " + url);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("cmd.exe", "/c", url);
            var s = processBuilder.start();
            s.waitFor();
            // 执行import命令
            File file = new File(localPath + tableName + ".sql");
            if (file.exists()) {
                String importUrl = "mysql -uminisql -pmysql " + region.getRegionName() + " < " + localPath + tableName + ".sql";
                System.out.println("[dump] mysql import url: " + importUrl);
                ProcessBuilder processBuilder1 = new ProcessBuilder();
                processBuilder1.command("cmd.exe", "/c", importUrl);
                Process p1 = processBuilder1.start();
                p1.waitFor();
                // 更新zk节点信息
                zkService.createTable(region.toZKNodeValue(), tableName + "_slave");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/import")
    public void importTable(@RequestBody Map<String, String> map) {
        try {
            String tableName = map.get("tableName");
            String path = map.get("path");
            sqlService.execCreateAndDropSql("drop table " + tableName + " if exists;");
            String connUrl = environment.getProperty("spring.datasource.url");
            String[] temp = connUrl.split("/");
            String databaseName = temp[temp.length - 1];
            String cmd = "mysql -u minisql -pmysql " + databaseName + " < " + localPath + path;
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(cmd);
            zkService.createTable(region.toZKNodeValue(), tableName + "_slave");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
