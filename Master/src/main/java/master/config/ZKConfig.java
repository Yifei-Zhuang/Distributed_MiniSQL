package master.config;


import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZKConfig {

    CuratorFramework curatorFramework;
    @Value("${zookeeper.server-url}")
    private String serverUrl;
    @Value("${zookeeper.namespace}")
    private String namespace;
    @Value("${zookeeper.sessionTimeoutMs}")
    private int sessionTimeoutMs;
    @Value("${zookeeper.connectionTimeoutMs}")
    private int connectionTimeoutMs;
    @Value("${zookeeper.baseSleepTimeMs}")
    private int baseSleepTimeMs;
    @Value("${zookeeper.maxRetries}")
    private int maxRetries;

    @Bean
    public CuratorFramework curatorFramework() throws Exception {
        String connectString = serverUrl;
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries);
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .sessionTimeoutMs(sessionTimeoutMs)
                .connectionTimeoutMs(connectionTimeoutMs)
                .namespace(namespace)
                .retryPolicy(retryPolicy)
                .build();
        curatorFramework.create().creatingParentsIfNeeded().forPath("/lss");
        curatorFramework.start();
        this.curatorFramework = curatorFramework;
        return curatorFramework;
    }
}
