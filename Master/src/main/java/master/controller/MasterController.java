package master.controller;

import master.pojo.Region;
import master.pojo.RegionWithTables;
import master.pojo.Response;
import master.pojo.SQL;
import master.service.ZKService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class MasterController {
    @Autowired
    ZKService zkService;
    @Autowired
    List<Region> regions;

    @GetMapping("/regionInfo")
    public List<RegionWithTables> getRegionInfo() {
        ArrayList<RegionWithTables> arrayList = new ArrayList<>();
        for (Region region : regions) {
            arrayList.add(new RegionWithTables(region.regionName, region.databaseName, region.port,
                    zkService.regionTables.get(region.getRegionName())));
        }
        return arrayList;
    }

    @PostMapping("/exec")
    public Response execSQL(@RequestBody SQL sql) {
        ArrayList<String> relatedRegions = new ArrayList<>();
        zkService.regionTables.forEach((key, value) -> {
            for (String table : value) {
                if (table.equals(sql.tableName)) {
                    relatedRegions.add(key);
                }
            }
        });
    }
}
