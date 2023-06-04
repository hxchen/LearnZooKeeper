package com.example.vrgame.zookeeper;

import com.example.vrgame.zookeeper.SimpleCurator;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.zookeeper.CreateMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
@Slf4j
public class ZkConfig {


    private SimpleCurator zk;
    private String zkPath;

    public SimpleCurator getZk() {
        return this.zk;
    }

    public String getZkPath() {
        return this.zkPath;
    }

    public ZkConfig(SimpleCurator zk, String zkPath) {
        this.zk = zk;
        this.zkPath = zkPath;
    }

    @Override
    public String toString() {
        return "ZkConfig [zkPath=" + zkPath + "]";
    }

    //
    // 辅助工具
    //

    public void setDataOrCreateNode(CreateMode nodeType, String path, String data) {
        if (zk.exists(path)) {
            zk.setData(path, data);
        } else {
            zk.createPath(nodeType, path, data);
        }
    }

    //
    // 普通节点监听机制
    //

    public interface ZkConfigDataListener {

        public void dataChanged(String path, String data);

    }

    private ConcurrentMap<String, CuratorCache> ncs = new ConcurrentHashMap<>();

    public void getDataAndWatchNode(String path, ZkConfigDataListener ndl) {
        if (this.ncs.containsKey(path)) {
            throw new RuntimeException("Config不允许对同一个路径重复添加监听器：" + path);
        }

        CuratorCache nc = zk.watchNode(path, event -> {
            String data = event.getCache().get(path).get().getData() == null ? null : event.stringData();
            log.info("Config节点发生变化：watchNode：" + event.getPath() + " : " + data);
            ndl.dataChanged(event.getPath(), data);
        });
        this.ncs.put(path, nc);
    }

    public void removeNodeListenersByPath(String path) {
        CuratorCache nc = this.ncs.remove(path);
        if (null != nc) {
            nc.stream().iterator().remove();
        }
    }

    public void removeAllNodeListeners() {
        for (String path : this.ncs.keySet()) {
            this.removeNodeListenersByPath(path);
        }
    }

    public String getData(String path) {
        return zk.getData(path);
    }
}
