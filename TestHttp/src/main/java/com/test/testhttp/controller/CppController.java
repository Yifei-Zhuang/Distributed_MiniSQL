package com.test.testhttp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidParameterException;
import java.util.Map;

@Controller
@RequestMapping("/")
@RestController
public class CppController {
    @GetMapping("/")
    public String index(){
        return "hello";
    }
    @PostMapping("/")
    public String postIndex(@RequestBody Map<String,String> map) throws Exception {
        String codeStr = map.get("code");
        String msgStr = map.get("msg");
        if(codeStr == null || msgStr == null || codeStr.isEmpty() || msgStr.isEmpty()){
            throw new Exception();
        }
        System.out.println("code:" + Integer.parseInt(codeStr) + " msgStr:" + msgStr);
        return "hellopost";
    }

}
