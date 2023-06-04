package com.example.vrgame.zookeeper;

public interface IZkGameServerConfigListener {
    /**
     * 添加一个服务器
     * @param serverId
     */
    default void OnAdded(int serverId){}

    /**
     * 删除一个服务器
     * @param serverId
     */
    default void OnRemoved(int serverId){}

    /**
     * 服务器进程启动注册进来
     * @param serverId
     */
    default void OnStarted(int serverId){}

    /**
     * 服务器进程停止
     * @param serverId
     */
    default void OnStopped(int serverId){}
}
