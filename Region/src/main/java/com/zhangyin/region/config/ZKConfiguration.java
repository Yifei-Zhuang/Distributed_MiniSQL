package com.zhangyin.region.config;


import com.zhangyin.region.pojo.Region;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class ZKConfiguration {

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
                .namespace(namespace)
                .connectionTimeoutMs(connectionTimeoutMs)
                .retryPolicy(retryPolicy)
                .build();
        curatorFramework.start();
        if (curatorFramework.checkExists().forPath("/lss") == null) {
            curatorFramework.create().creatingParentsIfNeeded().forPath("/lss");
        }
        this.curatorFramework = curatorFramework;
        return curatorFramework;
    }

    @Bean
    public Region getRegion() {
        return new Region();
    }

    @Bean
    public ConcurrentHashMap<String, List<Region>> getTableRegions() {
        return new ConcurrentHashMap<>();
    }
}
