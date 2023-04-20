package master.pojo;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@AllArgsConstructor
@NoArgsConstructor
public
class RegionWithTables extends Region {
    ArrayList<String> tables;

    public RegionWithTables(String regionName, String databaseName, int port, ArrayList<String> tables) {
        super(regionName, databaseName, port);
        this.tables = tables;
    }

    public ArrayList<String> getTables() {
        return tables;
    }

    public void setTables(ArrayList<String> tables) {
        this.tables = tables;
    }

}