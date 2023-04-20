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
        // 写死，不想改了
        // TODO 修改成启动的时候检查所有lss下的region节点
        list = new ArrayList<>();
//        list.add(new Region("a", "a", 8088));
//        list.add(new Region("b", "b", 8089));
//        list.add(new Region("c", "c", 8090));
//        list.add(new Region("d", "d", 8091));
//        list.add(new Region("e", "e", 8092));
        return list;
    }
}
