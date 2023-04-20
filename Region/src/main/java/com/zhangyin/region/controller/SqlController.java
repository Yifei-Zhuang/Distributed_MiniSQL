package com.zhangyin.region.controller;

import com.zhangyin.region.service.SqlService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

enum StatusCode {
    SUCCESS,
    FAIL
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class MyResponse {
    public StatusCode statusCode;
    public String msg;

    public static MyResponse success() {
        return new MyResponse(StatusCode.SUCCESS, "");
    }

    public static MyResponse success(String msg) {
        return new MyResponse(StatusCode.SUCCESS, msg);
    }

    public static MyResponse fail() {
        return new MyResponse(StatusCode.FAIL, "");
    }

    public static MyResponse fail(String msg) {
        return new MyResponse(StatusCode.FAIL, msg);
    }
}

@RestController
public class SqlController {
    @Autowired
    Environment environment;

    @Autowired
    SqlService sqlService;

    @PostMapping("/exec")
    public MyResponse execSQL(@RequestBody Map<String, String> map, HttpServletResponse httpServletResponse) {
        String sql = map.get("sql");
        if (sql == null) {
            return MyResponse.fail("missing sql");
        }
        try {
            String type = sql.split(" ")[0];
            if ("insert".equalsIgnoreCase(type) || "delete".equalsIgnoreCase(type) || "update".equalsIgnoreCase(type)) {
                sqlService.execUpdateOrInsertOrDeleteSqls(sql);
                return MyResponse.success();
            } else if ("create".equalsIgnoreCase(type) || "drop".equalsIgnoreCase(type)) {
                sqlService.execCreateAndDropSql(sql);
                return MyResponse.success();
            } else {
                return MyResponse.success(sqlService.execSelectSql(sql));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return MyResponse.fail(e.toString());
        }
    }

    @PostMapping("/dump")
    public void dump(@RequestBody Map<String, String> map) {
        String tableName = map.get("tableName");
        String path = map.get("path");
        String connUrl = environment.getProperty("spring.datasource.url");
        String[] temp = connUrl.split("/");
        String databaseName = temp[temp.length - 1];
        String cmd = "mysqldump -uroot -pmysql " + databaseName + " " + tableName + " > " + path;
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(cmd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/import")
    public void importTable(@RequestBody Map<String, String> map) {
        try {
            String tableName = map.get("tableName");
            String path = map.get("path");
            sqlService.execCreateAndDropSql("drop table " + tableName + ";");
            String connUrl = environment.getProperty("spring.datasource.url");
            String[] temp = connUrl.split("/");
            String databaseName = temp[temp.length - 1];
            String cmd = "mysql -uroot -pmysql " + databaseName + " < " + path;
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(cmd);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
