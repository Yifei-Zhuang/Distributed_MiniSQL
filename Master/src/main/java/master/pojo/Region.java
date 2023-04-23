package master.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;

@Data
@AllArgsConstructor
@NoArgsConstructor
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

    public boolean containsTable(String tableName) {
        return tables.contains(tableName);
    }
}
