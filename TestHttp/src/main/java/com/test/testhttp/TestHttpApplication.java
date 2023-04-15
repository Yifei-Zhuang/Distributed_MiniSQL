package com.test.testhttp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.util.Arrays;

@SpringBootApplication
public class TestHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestHttpApplication.class, args);
    }

}
