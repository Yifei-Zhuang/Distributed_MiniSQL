package com.zhangyin.region.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Region {
    public String regionName;
    public int port;
    public ArrayList<String> tables;
}
