package master.config;

import master.pojo.Region;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RegionMetaConfig {
    List<Region> list;

    @Bean
    public List<Region> initList() {
        // 写死，不想改了
        list = new ArrayList<>();
        list.add(new Region("a", "a", 8088));
        list.add(new Region("b", "b", 8089));
        list.add(new Region("c", "c", 8090));
        list.add(new Region("d", "d", 8091));
        list.add(new Region("e", "e", 8092));
        return list;
    }
}
