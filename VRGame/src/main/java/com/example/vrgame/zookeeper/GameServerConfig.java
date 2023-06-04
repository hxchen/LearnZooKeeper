package com.example.vrgame.zookeeper;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class GameServerConfig extends ZkConfig{
    private int gameServerId;
    private MySQLConfig mySQLConfig;
    private List<IZkGameServerConfigListener> listeners;

    public GameServerConfig(SimpleCurator zk, String zkPath, int gameServerId, List<IZkGameServerConfigListener> listeners) {
        super(zk, zkPath);
        this.gameServerId = gameServerId;
        this.listeners = listeners;
        this.mySQLConfig = new MySQLConfig(zk, this.getZkPath()+"/MySql");
    }

    public void close() {
        log.info("GameServerConfig close, remove all node listeners");
        this.removeAllNodeListeners();
    }
}
