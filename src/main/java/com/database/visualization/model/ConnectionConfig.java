package com.database.visualization.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * 数据库连接配置
 */
public class ConnectionConfig implements Serializable {
    private String id;
    private String name;
    private String databaseType; // mysql, postgresql, oracle, sqlserver, sqlite
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String url;
    
    public ConnectionConfig() {
        this.id = UUID.randomUUID().toString();
    }
    
    public ConnectionConfig(String name, String databaseType, String host, int port, 
                            String database, String username, String password) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.databaseType = databaseType;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        generateUrl();
    }
    
    /**
     * 根据数据库类型生成连接URL
     */
    public void generateUrl() {
        switch (databaseType.toLowerCase()) {
            case "mysql":
                url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", 
                        host, port, database);
                break;
            case "postgresql":
                url = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
                break;
            case "oracle":
                url = String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database);
                break;
            case "sqlserver":
                url = String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, database);
                break;
            case "sqlite":
                url = String.format("jdbc:sqlite:%s", database);
                break;
            case "redis":
                // Redis不使用JDBC URL，但为显示和一致性创建一个类似的格式
                url = String.format("redis://%s:%d/%s", host, port,(database==null|| database.isEmpty()) ? "0" : database);
                break;
            default:
                url = "";
                break;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
        generateUrl();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
        generateUrl();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        generateUrl();
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
        generateUrl();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        // 如果密码已加密，则解密
        if (password != null && com.database.visualization.utils.SecurityUtil.isEncrypted(password)) {
            return com.database.visualization.utils.SecurityUtil.decrypt(password);
        }
        return password;
    }

    public void setPassword(String password) {
        // 如果密码非空且未加密，则加密
        if (password != null && !password.isEmpty() && 
                !com.database.visualization.utils.SecurityUtil.isEncrypted(password)) {
            this.password = com.database.visualization.utils.SecurityUtil.encrypt(password);
        } else {
            this.password = password;
        }
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return name;
    }
} 