package com.example.vrgame.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.RetryForever;
import org.apache.zookeeper.CreateMode;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 封装的ZK客户端连接类
 */
@Slf4j
public class SimpleCurator implements Closeable {

    private CuratorFramework client;

    private Executor listenerExecutor;

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static String properties2string(Properties prop) {
        StringWriter writer = new StringWriter();
        try {
            prop.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer.getBuffer().toString();
    }

    public static Properties string2properties(String data) {
        StringReader reader = new StringReader(data);
        Properties prop = new Properties();
        try {
            prop.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return prop;
    }

    /**
     * @param connectString zk 地址
     * @param listenerExecutor 线程池(单线程)
     */
    public SimpleCurator(String connectString, Executor listenerExecutor) {
        this.listenerExecutor = listenerExecutor;
        RetryPolicy retry = new RetryForever(1000);
        this.client = CuratorFrameworkFactory.builder().connectString(connectString).retryPolicy(retry).build();
        this.client.start();
        log.info(String.format("ZK：开始连接%s（等待连接成功）", connectString));
        try {
            this.client.blockUntilConnected();
            log.info(String.format("ZK：连接%s成功", connectString));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * 节点是否存在
     * @param path
     */
    public boolean exists(String path) {
        try {
            return null != this.client.checkExists().forPath(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 建立节点
     *
     * @param type
     * @param path
     * @param data
     */
    public void createPath(CreateMode type, String path, String data) {
        String strData = null == data ? "" : data;
        try {
            this.client.create().creatingParentsIfNeeded().withMode(type).forPath(path,
                    strData.getBytes(DEFAULT_CHARSET));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 删除节点
     *
     * @param path
     */
    public void deletePath(String path) {
        try {
            this.client.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取~获取不到返回默认值
     *
     * @param path
     * @param defaultValue
     * @return
     */
    public String getData(String path, String defaultValue) {
        String data = getData(path);
        return data == null ? defaultValue : data;
    }

    /**
     * 读取
     *
     * @param path
     * @return
     */
    public String getData(String path) {
        String rtn = doGetData(path);
        log.debug("ZK：获取" + path + "下的数据为" + rtn);
        return rtn;
    }

    /**
     * 读取
     *
     * @param path
     * @return
     */
    private String doGetData(String path) {
        try {
            // 要考虑路径不存在
            if (null == this.client.checkExists().forPath(path)) {
                return null;
            }
            // 要考虑数据不存在
            byte[] bytes = this.client.getData().forPath(path);
            if (null == bytes || bytes.length == 0) {
                return null;
            }
            String data = new String(bytes, DEFAULT_CHARSET);
            return data;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置
     *
     * @param path
     * @param data
     */
    public void setData(String path, String data) {
        try {
            this.client.setData().forPath(path, data.getBytes(DEFAULT_CHARSET));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 监听节点
     *
     * @param path
     * @param listener
     */
    protected List<CuratorCache> nodeCacheList = new ArrayList<>();

    public CuratorCache watchNode(String path, NodeListener listener) {
        CuratorCache cache = CuratorCache.build(this.client, path);

        CuratorCacheListener l = CuratorCacheListener.builder()
                .forInitialized(() -> {
                    System.out.println("Initialized!");
                })
                .forCreates(childData -> System.out.println("Creates! " + childData))
                .forChanges((childData, childData1) -> {
                    System.out.println("Changes! " + childData + ", " + childData1);
                    listener.nodeChanged(new NodeEvent(SimpleCurator.this.client, path, cache));
                })
                .forCreatesAndChanges((childData, childData1) -> System.out.println("CreatesAndChanges! " + childData + ", " + childData1))
                .forDeletes(childData -> System.out.println("Deletes! " + childData))
                .forAll((type, childData, childData1) -> System.out.println("All! " + type + ", " + childData + ", " + childData1))
                .build();
        if (null != this.listenerExecutor) {
            cache.listenable().addListener(l, this.listenerExecutor);
        } else {
            cache.listenable().addListener(l);
        }
        try {
            cache.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.nodeCacheList.add(cache);
        // 先触发一次，将当前数据设置进去，避免加了监听器后还需要再getData。
        listener.nodeChanged(new NodeEvent(SimpleCurator.this.client, path, cache));
        return cache;
    }

    public static class NodeEvent {
        private CuratorFramework client;
        private String path;
        private CuratorCache cache;

        public NodeEvent(CuratorFramework client, String path, CuratorCache cache) {
            super();
            this.client = client;
            this.path = path;
            this.cache = cache;
        }

        public CuratorFramework getClient() {
            return client;
        }

        public String getPath() {
            return path;
        }

        public CuratorCache getCache() {
            return cache;
        }

        public String stringData() {
            try {
                byte[] bytes = cache.get(path).get().getData();
                if (null == bytes) {
                    return null;
                } else {
                    return new String(cache.get(path).get().getData(), DEFAULT_CHARSET);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "NodeEvent [path=" + path + ", cache=" + cache + "]";
        }

    }

    public interface NodeListener {

        void nodeChanged(NodeEvent event);
    }

    /**
     * 监听子节点
     *
     * @param path
     * @param listener
     */
    protected List<PathChildrenCache> pathChildrenCacheList = new ArrayList<>();

    public PathChildrenCache watchChildren(String path, ChildrenListener listener) {
        PathChildrenCache cache = new PathChildrenCache(this.client, path, true);
        PathChildrenCacheListener l = new PathChildrenCacheListener() {

            @Override
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                try {
                    switch (event.getType()) {
                        case CHILD_ADDED:
                            listener.childAdded(new ChildrenEvent(SimpleCurator.this.client, event.getData().getPath(), cache));
                            break;
                        case CHILD_UPDATED:
                            listener.childUpdated(
                                    new ChildrenEvent(SimpleCurator.this.client, event.getData().getPath(), cache));
                            break;
                        case CHILD_REMOVED:
                            listener.childRemoved(
                                    new ChildrenEvent(SimpleCurator.this.client, event.getData().getPath(), cache));
                            break;
                        default:
                            listener.otherEvent(SimpleCurator.this.client, event, cache);
                    }
                } catch (Exception e) {
                    log.error("ZK：watchChildren发生异常：", e);
                    throw e;
                }
            }
        };
        if (null != this.listenerExecutor) {
            cache.getListenable().addListener(l, this.listenerExecutor);
        } else {
            cache.getListenable().addListener(l);
        }
        try {
            cache.start(PathChildrenCache.StartMode.NORMAL);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.pathChildrenCacheList.add(cache);
        return cache;
    }

    public static class ChildrenEvent {
        private CuratorFramework client;
        private String path;
        private PathChildrenCache cache;

        public ChildrenEvent(CuratorFramework client, String path, PathChildrenCache cache) {
            super();
            this.client = client;
            this.path = path;
            this.cache = cache;
        }

        public CuratorFramework getClient() {
            return client;
        }

        public String getPath() {
            return path;
        }

        public PathChildrenCache getCache() {
            return cache;
        }

        @Override
        public String toString() {
            return "ChildrenEvent [path=" + path + ", cache=" + cache + "]";
        }

    }

    public interface ChildrenListener {

        void childAdded(ChildrenEvent event);

        void childUpdated(ChildrenEvent event);

        void childRemoved(ChildrenEvent event);

        /**
         * 其他事件（仅用来debug，如果需要处理其他事件，应修改此类，添加具体事件处理方法）
         *
         * @param client
         * @param event
         * @param cache
         */
        void otherEvent(CuratorFramework client, PathChildrenCacheEvent event, PathChildrenCache cache);

    }

    /**
     * 监听树
     *
     * @param path
     * @param listener
     */

    protected List<TreeCache> treeCacheList = new ArrayList<>();

    public TreeCache watchTree(String path, TreeListener listener) {
        TreeCache cache = new TreeCache(this.client, path);
        TreeCacheListener l = (client, event) -> {
            try {
                switch (event.getType()) {
                    case NODE_ADDED:
                        listener.nodeAdded(new TreeEvent(SimpleCurator.this.client, event.getData().getPath(), cache));
                        break;
                    case NODE_UPDATED:
                        listener.nodeUpdated(
                                new TreeEvent(SimpleCurator.this.client, event.getData().getPath(), cache));
                        break;
                    case NODE_REMOVED:
                        listener.nodeRemoved(
                                new TreeEvent(SimpleCurator.this.client, event.getData().getPath(), cache));
                        break;
                    case INITIALIZED:
                        listener.nodeInitialized();
                        break;
                    default:
                        listener.otherEvent(SimpleCurator.this.client, event, cache);
                }
            } catch (Exception e) {
                log.error("ZK：watchNode发生异常：", e);
                throw e;
            }
        };
        if (null != this.listenerExecutor) {
            cache.getListenable().addListener(l, this.listenerExecutor);
        } else {
            cache.getListenable().addListener(l);
        }
        try {
            cache.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.treeCacheList.add(cache);
        return cache;
    }

    public static class TreeEvent {
        private CuratorFramework client;
        private String path;
        private TreeCache cache;

        public TreeEvent(CuratorFramework client, String path, TreeCache cache) {
            super();
            this.client = client;
            this.path = path;
            this.cache = cache;
        }

        public CuratorFramework getClient() {
            return client;
        }

        public String getPath() {
            return path;
        }

        public TreeCache getCache() {
            return cache;
        }

        @Override
        public String toString() {
            return "TreeEvent [path=" + path + ", cache=" + cache + "]";
        }

    }

    public interface TreeListener {

        void nodeAdded(TreeEvent event);

        void nodeUpdated(TreeEvent event);

        void nodeRemoved(TreeEvent event);

        /**
         * 首次启动，缓存完毕所有节点后，发出此通知。
         */
        void nodeInitialized();

        /**
         * 其他事件（仅用来debug，如果需要处理其他事件，应修改此类，添加具体事件处理方法）
         *
         * @param client
         * @param event
         * @param cache
         */
        void otherEvent(CuratorFramework client, TreeCacheEvent event, TreeCache cache);

    }

    /**
     * 获得子节点
     *
     * @return
     */
    public List<String> getChildren(String path) {
        try {
            return client.getChildren().forPath(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        for (CuratorCache cache : nodeCacheList) {
            try {
                cache.close();
            } catch (Exception e) {
                log.error("ZK：关闭 NodeCache " + cache + " 出错!", e);
            }
        }
        for (PathChildrenCache cache : pathChildrenCacheList) {
            try {
                cache.close();
            } catch (Exception e) {
                log.error("ZK：关闭 PathChildrenCache " + cache + " 出错!", e);
            }
        }
        for (TreeCache cache : treeCacheList) {
            try {
                cache.close();
            } catch (Exception e) {
                log.error("ZK：关闭 TreeCache " + cache + " 出错!", e);
            }
        }
        this.client.watches().removeAll();
        this.client.close();

        log.info("zookeeper close already!!!!");
    }
}
