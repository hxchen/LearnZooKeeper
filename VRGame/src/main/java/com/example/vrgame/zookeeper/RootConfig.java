package com.example.vrgame.zookeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ZK 上的根目录对象
 */
public class RootConfig extends ZkConfig{
    private GameServersConfig gameServers;
    private List<IZkGameServerConfigListener> gameServerOuterListeners = new ArrayList<>();
    public RootConfig(SimpleCurator zk, String zkPath) {
        super(zk, zkPath);
        this.gameServers = new GameServersConfig(zk, this.getZkPath() + "/GameServers", this.gameServerOuterListeners);
    }

    public Map<Integer, GameServerConfig> getGameServers() {
        return gameServers.getGameServers();
    }

}
