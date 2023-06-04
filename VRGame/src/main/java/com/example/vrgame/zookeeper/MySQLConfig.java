package com.example.vrgame.zookeeper;

public class MySQLConfig extends ZkConfig{

    private String url;
    private String username;

    private String password;

    public MySQLConfig(SimpleCurator zk, String zkPath) {
        super(zk, zkPath);
        this.url = zk.getData(this.getZkPath() + "/url");
        this.username =zk.getData(this.getZkPath() + "/username");
        this.password = zk.getData(this.getZkPath() + "/password");
    }

    @Override
    public String toString() {
        return "MySQLConfig{" +
                "url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
