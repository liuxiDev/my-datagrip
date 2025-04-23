package com.database.visualization.utils;

import com.database.visualization.model.ConnectionConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 连接管理工具类
 */
public class ConnectionManager {
    private static final String CONFIG_FILE = "connections.json";
    private static final String SETTINGS_FILE = "settings.json";
    private static List<ConnectionConfig> connections = new ArrayList<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Map<String, Object> settings = new HashMap<>();
    
    /**
     * 加载所有连接配置
     */
    public static List<ConnectionConfig> loadConnections() {
        File file = new File(CONFIG_FILE);
        // 并且文件内容不能为空
        if (file.exists() && file.length() > 0) {
            try {
                connections = objectMapper.readValue(file, new TypeReference<List<ConnectionConfig>>() {});
            } catch (IOException e) {
                e.printStackTrace();
                connections = new ArrayList<>();
            }
        }
        return connections;
    }
    
    /**
     * 保存所有连接配置
     */
    public static void saveConnections() {
        try {
            objectMapper.writeValue(new File(CONFIG_FILE), connections);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 添加连接配置，确保不重复
     */
    public static boolean addConnection(ConnectionConfig config) {
        // 检查是否存在重复连接
        if (isDuplicateConnection(config)) {
            return false;
        }
        
        connections.add(config);
        saveConnections();
        return true;
    }
    
    /**
     * 检查是否存在重复连接
     */
    private static boolean isDuplicateConnection(ConnectionConfig config) {
        for (ConnectionConfig existing : connections) {
            // 如果ID相同，认为是同一个连接的更新
            if (existing.getId().equals(config.getId())) {
                continue;
            }
            
            // 检查连接是否重复（主机+端口+数据库+类型）
            if (existing.getDatabaseType().equalsIgnoreCase(config.getDatabaseType()) &&
                existing.getHost().equalsIgnoreCase(config.getHost()) &&
                existing.getPort() == config.getPort() &&
                existing.getDatabase().equalsIgnoreCase(config.getDatabase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 删除连接配置
     */
    public static void deleteConnection(ConnectionConfig config) {
        connections.removeIf(c -> c.getId().equals(config.getId()));
        saveConnections();
    }
    
    /**
     * 更新连接配置
     */
    public static void updateConnection(ConnectionConfig config) {
        for (int i = 0; i < connections.size(); i++) {
            if (connections.get(i).getId().equals(config.getId())) {
                connections.set(i, config);
                break;
            }
        }
        saveConnections();
    }
    
    /**
     * 通过ID获取连接配置
     */
    public static ConnectionConfig getConnectionById(String id) {
        for (ConnectionConfig config : connections) {
            if (config.getId().equals(id)) {
                return config;
            }
        }
        return null;
    }
    
    /**
     * 获取所有连接配置
     */
    public static List<ConnectionConfig> getConnections() {
        if (connections.isEmpty()) {
            loadConnections();
        }
        return connections;
    }
    
    /**
     * 加载设置
     */
    public static void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try {
                settings = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                e.printStackTrace();
                settings = new HashMap<>();
            }
        }
    }
    
    /**
     * 保存设置
     */
    public static void saveSettings() {
        try {
            objectMapper.writeValue(new File(SETTINGS_FILE), settings);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 保存特定设置
     * @param key 设置键
     * @param value 设置值
     */
    public static void saveSetting(String key, Object value) {
        if (settings.isEmpty()) {
            loadSettings();
        }
        settings.put(key, value);
        saveSettings();
    }
    
    /**
     * 获取特定设置
     * @param key 设置键
     * @param defaultValue 默认值
     * @return 设置值，如果不存在则返回默认值
     */
    public static Object getSetting(String key, Object defaultValue) {
        if (settings.isEmpty()) {
            loadSettings();
        }
        return settings.getOrDefault(key, defaultValue);
    }
} 