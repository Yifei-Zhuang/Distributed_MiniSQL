package master.pojo;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public
class RegionWithTables extends Region {
    String[] tables;

    public RegionWithTables(String regionName, String databaseName, int port, String[] tables) {
        super(regionName, databaseName, port);
        this.tables = tables;
    }

    public String[] getTables() {
        return tables;
    }

    public void setTables(String[] tables) {
        this.tables = tables;
    }

}