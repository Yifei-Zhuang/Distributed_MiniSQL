package com.zhangyin.region;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;

public class testPost {
    public static void main(String[] args) {
        String url = "http://127.0.0.1:8088/exec";
        HashMap<String, String> params = new HashMap<>();
        params.put("sql", "insert into t8 values(1,2);");
        JSONObject paramMap = new JSONObject();
        paramMap.putAll(params);
        System.out.println(paramMap);
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        RestTemplate client = restTemplateBuilder.setReadTimeout(Duration.ofMillis(1000)).setConnectTimeout(Duration.ofMillis(1000)).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JSONObject> httpEntity = new HttpEntity<>(paramMap, headers);
        var result = client.postForEntity(url, httpEntity, JSONObject.class);
        System.out.println(result);
        System.out.println(result.getBody());
    }
}
