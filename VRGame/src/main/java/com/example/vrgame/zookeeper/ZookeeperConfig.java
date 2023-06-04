package com.example.vrgame.zookeeper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Zk 参数配置 对应zookeeper.json
 */
@Slf4j
@Data
public class ZookeeperConfig {
    private String connectString;
    private String configPath;


}
