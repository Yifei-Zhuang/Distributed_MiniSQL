package master.config;

import lombok.Data;
import master.pojo.Region;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Data
public class RegionMetaConfig {
    public List<Region> list;

    @Bean
    public List<Region> initList() {
        list = new ArrayList<>();
        return list;
    }
}
