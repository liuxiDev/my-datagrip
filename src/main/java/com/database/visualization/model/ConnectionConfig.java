package com.database.visualization.model;

import java.io.Serializable;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        setPassword(password); // 使用setter确保密码被加密
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

    // 这个方法用于Jackson的序列化，确保存储的是加密密码
    @JsonProperty("password")
    private String getEncryptedPassword() {
        return password;
    }
    
    // 这个方法用于Jackson的反序列化，确保读取时直接保存加密密码
    @JsonProperty("password")
    private void setEncryptedPassword(String encryptedPassword) {
        this.password = encryptedPassword;
    }

    // 这个方法用于应用程序获取解密后的密码
    @JsonIgnore
    public String getPassword() {
        // 检查并迁移旧版加密密码
        if (password != null && !password.isEmpty()) {
            try {
                // 尝试迁移旧版加密密码
                String migratedPassword = com.database.visualization.utils.SecurityManager.migratePasswordIfNeeded(password);
                if (!migratedPassword.equals(password)) {
                    // 如果密码被迁移，更新存储的密码
                    this.password = migratedPassword;
                }
                return com.database.visualization.utils.SecurityManager.decryptPassword(migratedPassword);
            } catch (Exception e) {
                // 如果迁移或解密过程出错，返回原始密码
                System.err.println("密码处理失败，使用原始密码: " + e.getMessage());
                return password;
            }
        }
        return password;
    }

    // 这个方法用于应用程序设置密码，确保加密
    @JsonIgnore
    public void setPassword(String password) {
        // 使用SecurityManager加密密码
        if (password != null && !password.isEmpty()) {
            this.password = com.database.visualization.utils.SecurityManager.encryptPassword(password);
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