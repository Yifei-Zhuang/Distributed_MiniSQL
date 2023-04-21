package master.config;

import lombok.Data;
import master.pojo.RegionWithTables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Data
public class RegionMetaConfig {
    public List<RegionWithTables> list;

    @Bean
    public List<RegionWithTables> initList() {
        list = new ArrayList<>();
        return list;
    }
}
