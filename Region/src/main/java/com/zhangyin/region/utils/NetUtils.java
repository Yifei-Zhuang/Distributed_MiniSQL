package com.zhangyin.region.utils;

import com.alibaba.fastjson2.JSONObject;
import com.zhangyin.region.pojo.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.UnknownHostException;
import java.util.HashMap;

@Component
public class NetUtils {
    @Autowired
    Region region;

    public JSONObject sendPost(String url, HashMap<String, String> params) {
        try {
            if (!url.startsWith("http")) {
                url = "http://" + url;
            }
            JSONObject paramMap = new JSONObject();
            paramMap.putAll(params);
            RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
            RestTemplate client = restTemplateBuilder.build();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<JSONObject> httpEntity = new HttpEntity<>(paramMap, headers);
            var result = client.postForEntity(url, httpEntity, JSONObject.class);
            return result.getBody();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getIP() throws UnknownHostException {
        String localIP = null;
        try {
            localIP = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return localIP;
    }


}
