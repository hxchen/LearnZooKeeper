package com.example.vrgame;

import com.example.vrgame.common.ConfigLoader;
import com.example.vrgame.zookeeper.ZookeeperConfig;
import lombok.Getter;

public class ServerConfigManager {

    private static final ServerConfigManager instance = new ServerConfigManager();
    public static ServerConfigManager getInstance(){
        return instance;
    }

    @Getter
    private ZookeeperConfig zookeeperConfig;

    public void loadConfig(){
        this.zookeeperConfig = ConfigLoader.loadConfig(ZookeeperConfig.class, GameConstants.ZOOKEEPER_CONFIG_NAME);
    }
}
