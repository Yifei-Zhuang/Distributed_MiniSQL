package master.utils;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

@Component
public class NetUtils {
    public JSONObject sendPost(String url, HashMap<String, String> params) {
        JSONObject paramMap = new JSONObject();
        paramMap.putAll(params);
        RestTemplate client = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JSONObject> httpEntity = new HttpEntity<>(paramMap, headers);
        return client.postForEntity(url, httpEntity, JSONObject.class).getBody();
    }
}
