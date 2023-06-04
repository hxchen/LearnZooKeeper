package com.example.vrgame.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
@Slf4j
public class GameServersConfig extends ZkConfig{
    /**
     * 每次在ZK上新建GameServer时，这个是最后一个建立的节点，表示整个GameServer信息完整了
     */
    public static final String GAME_CONFIG_END_FLAG = "GAME_CONFIG_END_FLAG";

    private Map<Integer, GameServerConfig> gameServers = new ConcurrentSkipListMap<>();
    /**
     * 返回只读GameServerConfig Map
     */
    private Map<Integer, GameServerConfig> unmodifiableGameServers = Collections.unmodifiableMap(gameServers);

    private List<IZkGameServerConfigListener> listenerList;

    private int parseServerId(String path) {
        String subPath = path.replaceFirst(this.getZkPath(), "");
        String[] pathData = subPath.split("/");
        String sServerId = pathData[1];
        return Integer.parseInt(sServerId);
    }

    private void onRemoved(int serverId) {

        if (null != listenerList) {
            for (IZkGameServerConfigListener listener : listenerList) {
                listener.OnRemoved(serverId);
            }
        }
    }

    private void onAdded(int serverId) {

        if (null != listenerList) {
            for (IZkGameServerConfigListener listener : listenerList) {
                listener.OnAdded(serverId);
            }
        }
    }
    public Map<Integer, GameServerConfig> getGameServers() {
        return unmodifiableGameServers;
    }
    public GameServersConfig(SimpleCurator zk, String zkPath, List<IZkGameServerConfigListener> listeners) {
        super(zk, zkPath);
        this.listenerList = listeners;

        // 需要等下面配置都读完后，才能继续。
        CountDownLatch waitForConfigLoadFinish = new CountDownLatch(1);

        zk.watchChildren(this.getZkPath(), new SimpleCurator.ChildrenListener() {
            @Override
            public void childAdded(SimpleCurator.ChildrenEvent event) {
                log.info("ZKConfig节点发生变化：watchChildren.childAdded：" + event);
            }

            @Override
            public void childUpdated(SimpleCurator.ChildrenEvent event) {
                log.info("ZKConfig节点发生变化：watchChildren.childUpdated：" + event);
            }

            @Override
            public void childRemoved(SimpleCurator.ChildrenEvent event) {
                log.info("ZKConfig节点发生变化：watchChildren.childRemoved：" + event.getPath());
                // GameServers下面的直接子节点（serverId）被移除，表明此GameServer被彻底从线上删除了
                String path = event.getPath();
                int serverId = GameServersConfig.this.parseServerId(path);
                GameServerConfig gsc = GameServersConfig.this.gameServers.remove(serverId);
                if (null == gsc) {
                    log.error("ZKConfig节点发生变化：watchChildren.childRemoved：" + event.getPath()
                            + "，无法找到ServerID：" + serverId + "对应的GameServerConfig！");
                } else {
                    gsc.close();
                }
                onRemoved(serverId);
            }

            @Override
            public void otherEvent(CuratorFramework client, PathChildrenCacheEvent event, PathChildrenCache cache) {
                log.info("ZKConfig节点发生变化：watchChildren.otherEvent：" + event);
            }
        });

        zk.watchTree(this.getZkPath(), new SimpleCurator.TreeListener() {
            @Override
            public void nodeAdded(SimpleCurator.TreeEvent event) {
                String path = event.getPath();
                log.info("ZKConfig节点发生变化：watchTree.nodeAdded：" + event.getPath());
                if (path.contains(GAME_CONFIG_END_FLAG)) {
                    // 只有此时，该GameServerConfig的所有子节点才是全部建立完毕。此时才能启动解析。
                    int serverId = GameServersConfig.this.parseServerId(path);
                    String gameServerPath = GameServersConfig.this.getZkPath() + "/" + serverId;
                    GameServerConfig gameServer = new GameServerConfig(zk, gameServerPath, serverId, listenerList);
                    GameServersConfig.this.gameServers.put(serverId, gameServer);
                    onAdded(serverId);
                    log.info("ZKConfig节点发生变化：watchTree.nodeAdded：添加Game Server服务器：" + gameServer);
                }
            }

            @Override
            public void nodeUpdated(SimpleCurator.TreeEvent event) {
                log.info("ZKConfig节点发生变化：watchTree.nodeUpdated：" + event.getPath());
            }

            @Override
            public void nodeRemoved(SimpleCurator.TreeEvent event) {
                log.info("ZKConfig节点发生变化：watchTree.nodeRemoved：" + event.getPath());
            }

            @Override
            public void nodeInitialized() {
                log.info("ZKConfig节点发生变化：watchTree.nodeInitialized");
                waitForConfigLoadFinish.countDown();
            }

            @Override
            public void otherEvent(CuratorFramework client, TreeCacheEvent event, TreeCache cache) {
                log.info("ZKConfig节点发生变化：watchTree.otherEvent：" + event);
            }
        });

        try {
            waitForConfigLoadFinish.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("ZKConfig等待GameServers全部读取完毕的过程中发生线程中断！", e);
        }
    }

}
