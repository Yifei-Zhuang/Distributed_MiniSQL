//package master.utils;
//
//import com.alibaba.fastjson2.JSONArray;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import master.pojo.Table;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Data
//@AllArgsConstructor
//@Component
//public class JSONUtil {
//    public List<Table> JSONArrayToTableArray(JSONArray jsonArray) {
//        List<Table> result = new ArrayList<>();
//        for (Object table_ : jsonArray) {
//            Table table = ((com.alibaba.fastjson2.JSONObject) table_).toJavaObject(Table.class);
//            result.add(table);
//        }
//        return result;
//    }
//}
