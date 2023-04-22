package master.controller;

import master.pojo.Region;
import master.service.ZKService;
import master.utils.NetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
public class MasterController {
    @Autowired
    ZKService zkService;
    @Autowired
    List<Region> regions;
    @Autowired
    NetUtils netUtils;

    private final String REGION_HOST = "http://127.0.0.1";

    @GetMapping("/regionInfo")
    public List<Region> getRegionInfo() {
        zkService.refreshRegion();
        return regions;
    }
}
