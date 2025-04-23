package com.database.visualization.controller;

import com.database.visualization.model.ConnectionConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;

/**
 * 数据库服务，用于管理连接和执行查询
 */
public class DatabaseService {
    private static final Map<String, HikariDataSource> dataSources = new HashMap<>();
    private static final Map<String, Object> redisConnections = new HashMap<>();
    
    /**
     * 测试数据库连接
     */
    public static boolean testConnection(ConnectionConfig config) {
        // 对Redis连接进行特殊处理
        if ("redis".equalsIgnoreCase(config.getDatabaseType())) {
            return testRedisConnection(config);
        }
        
        try (Connection conn = DriverManager.getConnection(
                config.getUrl(), config.getUsername(), config.getPassword())) {
            return conn.isValid(3);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 测试Redis连接
     */
    private static boolean testRedisConnection(ConnectionConfig config) {
        try {
            // 通过反射动态加载Redis客户端类，避免强依赖
            Class<?> jedisClass = Class.forName("redis.clients.jedis.Jedis");
            Object jedis = jedisClass.getConstructor(String.class, int.class)
                    .newInstance(config.getHost(), config.getPort());
            
            // 如果有密码，尝试认证
            if (config.getPassword() != null && !config.getPassword().isEmpty()) {
                jedisClass.getMethod("auth", String.class)
                        .invoke(jedis, config.getPassword());
            }
            
            // 选择数据库
            int dbIndex = 0;
            if (config.getDatabase() != null && !config.getDatabase().isEmpty()) {
                try {
                    dbIndex = Integer.parseInt(config.getDatabase());
                } catch (NumberFormatException e) {
                    // 忽略错误，使用默认数据库0
                }
            }
            jedisClass.getMethod("select", int.class).invoke(jedis, dbIndex);
            
            // 测试连接 - 尝试ping
            String result = (String) jedisClass.getMethod("ping").invoke(jedis);
            
            // 关闭连接
            jedisClass.getMethod("close").invoke(jedis);
            
            return "PONG".equalsIgnoreCase(result);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取数据库连接
     */
    public static Connection getConnection(ConnectionConfig config) throws SQLException {
        // Redis不支持JDBC连接
        if ("redis".equalsIgnoreCase(config.getDatabaseType())) {
            throw new SQLException("Redis不支持JDBC连接");
        }
        
        String id = config.getId();
        if (!dataSources.containsKey(id)) {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
            
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            dataSources.put(id, dataSource);
        }
        
        return dataSources.get(id).getConnection();
    }
    
    /**
     * 获取Redis连接
     */
    public static Object getRedisConnection(ConnectionConfig config) throws Exception {
        String id = config.getId();
        if (!redisConnections.containsKey(id)) {
            // 通过反射创建Jedis实例
            Class<?> jedisClass = Class.forName("redis.clients.jedis.Jedis");
            Object jedis = jedisClass.getConstructor(String.class, int.class)
                    .newInstance(config.getHost(), config.getPort());
            
            // 如果有密码，进行认证
            if (config.getPassword() != null && !config.getPassword().isEmpty()) {
                jedisClass.getMethod("auth", String.class)
                        .invoke(jedis, config.getPassword());
            }
            
            // 选择数据库
            int dbIndex = 0;
            if (config.getDatabase() != null && !config.getDatabase().isEmpty()) {
                try {
                    dbIndex = Integer.parseInt(config.getDatabase());
                } catch (NumberFormatException e) {
                    // 忽略错误，使用默认数据库0
                }
            }
            jedisClass.getMethod("select", int.class).invoke(jedis, dbIndex);
            
            redisConnections.put(id, jedis);
        }
        
        return redisConnections.get(id);
    }
    
    /**
     * 关闭数据库连接
     */
    public static void closeConnection(String connectionId) {
        if (dataSources.containsKey(connectionId)) {
            HikariDataSource dataSource = dataSources.get(connectionId);
            dataSource.close();
            dataSources.remove(connectionId);
        }
        
        if (redisConnections.containsKey(connectionId)) {
            try {
                Object jedis = redisConnections.get(connectionId);
                jedis.getClass().getMethod("close").invoke(jedis);
                redisConnections.remove(connectionId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 关闭所有连接
     */
    public static void closeAllConnections() {
        for (HikariDataSource dataSource : dataSources.values()) {
            dataSource.close();
        }
        dataSources.clear();
        
        for (Object jedis : redisConnections.values()) {
            try {
                jedis.getClass().getMethod("close").invoke(jedis);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        redisConnections.clear();
    }
    
    /**
     * 获取数据库所有的schema
     */
    public static List<String> getSchemas(ConnectionConfig config) {
        List<String> schemas = new ArrayList<>();
        
        if ("redis".equalsIgnoreCase(config.getDatabaseType())) {
            // Redis没有schema概念，返回空列表
            return schemas;
        }
        
        try (Connection conn = getConnection(config)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getSchemas();
            while (rs.next()) {
                schemas.add(rs.getString("TABLE_SCHEM"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return schemas;
    }
    
    /**
     * 获取指定schema下的所有表
     */
    public static List<String> getTables(ConnectionConfig config, String schema) {
        List<String> tables = new ArrayList<>();
        
        if ("redis".equalsIgnoreCase(config.getDatabaseType())) {
            try {
                // 对于Redis，将所有键作为"表"返回
                Object jedis = getRedisConnection(config);
                
                // 获取所有键
                Class<?> jedisClass = jedis.getClass();
                Set<String> keys = (Set<String>) jedisClass.getMethod("keys", String.class)
                        .invoke(jedis, "*");
                
                // 对键进行分组
                Map<String, String> keyTypes = new HashMap<>();
                for (String key : keys) {
                    String type = (String) jedisClass.getMethod("type", String.class)
                            .invoke(jedis, key);
                    keyTypes.put(key, type);
                }
                
                // 按类型分组
                tables.add("string");
                tables.add("hash");
                tables.add("list");
                tables.add("set");
                tables.add("zset");
                
                return tables;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return tables;
        }
        
        try (Connection conn = getConnection(config)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(
                    conn.getCatalog(), schema, "%", new String[]{"TABLE"});
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tables;
    }
    
    /**
     * 获取表的所有列信息
     */
    public static List<Map<String, String>> getColumns(ConnectionConfig config, String schema, String table) {
        List<Map<String, String>> columns = new ArrayList<>();
        
        if ("redis".equalsIgnoreCase(config.getDatabaseType())) {
            try {
                Object jedis = getRedisConnection(config);
                
                // 获取Redis键，按类型分组
                if ("string".equals(table)) {
                    Map<String, String> column = new HashMap<>();
                    column.put("name", "key");
                    column.put("type", "STRING");
                    column.put("size", "");
                    column.put("nullable", "NO");
                    columns.add(column);
                    
                    column = new HashMap<>();
                    column.put("name", "value");
                    column.put("type", "STRING");
                    column.put("size", "");
                    column.put("nullable", "NO");
                    columns.add(column);
                } else if ("hash".equals(table)) {
                    Map<String, String> column = new HashMap<>();
                    column.put("name", "key");
                    column.put("type", "STRING");
                    column.put("size", "");
                    column.put("nullable", "NO");
                    columns.add(column);
                    
                    column = new HashMap<>();
                    column.put("name", "field");
                    column.put("type", "STRING");
                    column.put("size", "");
                    column.put("nullable", "NO");
                    columns.add(column);
                    
                    column = new HashMap<>();
                    column.put("name", "value");
                    column.put("type", "STRING");
                    column.put("size", "");
                    column.put("nullable", "NO");
                    columns.add(column);
                } else if ("list".equals(table) || "set".equals(table) || "zset".equals(table)) {
                    Map<String, String> column = new HashMap<>();
                    column.put("name", "key");
                    column.put("type", "STRING");
                    column.put("size", "");
                    column.put("nullable", "NO");
                    columns.add(column);
                    
                    column = new HashMap<>();
                    column.put("name", "index");
                    column.put("type", "NUMBER");
                    column.put("size", "");
                    column.put("nullable", "NO");
                    columns.add(column);
                    
                    column = new HashMap<>();
                    column.put("name", "value");
                    column.put("type", "STRING");
                    column.put("size", "");
                    column.put("nullable", "NO");
                    columns.add(column);
                    
                    if ("zset".equals(table)) {
                        column = new HashMap<>();
                        column.put("name", "score");
                        column.put("type", "DOUBLE");
                        column.put("size", "");
                        column.put("nullable", "NO");
                        columns.add(column);
                    }
                }
                
                return columns;
            } catch (Exception e) {
                e.printStackTrace();
                return columns;
            }
        }
        
        try (Connection conn = getConnection(config)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getColumns(conn.getCatalog(), schema, table, "%");
            while (rs.next()) {
                Map<String, String> column = new HashMap<>();
                column.put("name", rs.getString("COLUMN_NAME"));
                column.put("type", rs.getString("TYPE_NAME"));
                column.put("size", rs.getString("COLUMN_SIZE"));
                column.put("nullable", rs.getString("IS_NULLABLE"));
                columns.add(column);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return columns;
    }
    
    /**
     * 执行SQL查询
     */
    public static Map<String, Object> executeQuery(ConnectionConfig config, String sql) {
        if ("redis".equalsIgnoreCase(config.getDatabaseType())) {
            return executeRedisQuery(config, sql);
        }
        
        Map<String, Object> result = new HashMap<>();
        List<String> columnNames = new ArrayList<>();
        List<List<Object>> data = new ArrayList<>();
        
        try (Connection conn = getConnection(config);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 获取列名
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
            
            // 获取数据
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                data.add(row);
            }
            
            result.put("success", true);
            result.put("columns", columnNames);
            result.put("data", data);
            
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 执行Redis查询
     */
    private static Map<String, Object> executeRedisQuery(ConnectionConfig config, String command) {
        Map<String, Object> result = new HashMap<>();
        List<String> columnNames = new ArrayList<>();
        List<List<Object>> data = new ArrayList<>();
        
        try {
            Object jedis = getRedisConnection(config);
            Class<?> jedisClass = jedis.getClass();
            
            // 解析命令
            String[] parts = command.trim().split("\\s+");
            String cmd = parts.length > 0 ? parts[0].toLowerCase() : "";
            
            switch (cmd) {
                case "get":
                    if (parts.length > 1) {
                        String key = parts[1];
                        Object value = jedisClass.getMethod("get", String.class)
                                .invoke(jedis, key);
                        
                        columnNames.add("key");
                        columnNames.add("value");
                        
                        List<Object> row = new ArrayList<>();
                        row.add(key);
                        row.add(value);
                        data.add(row);
                    }
                    break;
                    
                case "hgetall":
                    if (parts.length > 1) {
                        String key = parts[1];
                        Map<String, String> hash = (Map<String, String>) jedisClass
                                .getMethod("hgetAll", String.class)
                                .invoke(jedis, key);
                        
                        columnNames.add("key");
                        columnNames.add("field");
                        columnNames.add("value");
                        
                        for (Map.Entry<String, String> entry : hash.entrySet()) {
                            List<Object> row = new ArrayList<>();
                            row.add(key);
                            row.add(entry.getKey());
                            row.add(entry.getValue());
                            data.add(row);
                        }
                    }
                    break;
                    
                case "lrange":
                    if (parts.length > 3) {
                        String key = parts[1];
                        long start = Long.parseLong(parts[2]);
                        long end = Long.parseLong(parts[3]);
                        
                        List<String> list = (List<String>) jedisClass
                                .getMethod("lrange", String.class, long.class, long.class)
                                .invoke(jedis, key, start, end);
                        
                        columnNames.add("key");
                        columnNames.add("index");
                        columnNames.add("value");
                        
                        for (int i = 0; i < list.size(); i++) {
                            List<Object> row = new ArrayList<>();
                            row.add(key);
                            row.add(start + i);
                            row.add(list.get(i));
                            data.add(row);
                        }
                    }
                    break;
                    
                case "smembers":
                    if (parts.length > 1) {
                        String key = parts[1];
                        Set<String> members = (Set<String>) jedisClass
                                .getMethod("smembers", String.class)
                                .invoke(jedis, key);
                        
                        columnNames.add("key");
                        columnNames.add("value");
                        
                        int index = 0;
                        for (String member : members) {
                            List<Object> row = new ArrayList<>();
                            row.add(key);
                            row.add(member);
                            data.add(row);
                            index++;
                        }
                    }
                    break;
                    
                case "zrange":
                    if (parts.length > 3) {
                        String key = parts[1];
                        long start = Long.parseLong(parts[2]);
                        long end = Long.parseLong(parts[3]);
                        boolean withScores = parts.length > 4 && "withscores".equalsIgnoreCase(parts[4]);
                        
                        Set<String> zrange;
                        if (withScores) {
                            zrange = (Set<String>) jedisClass
                                    .getMethod("zrangeWithScores", String.class, long.class, long.class)
                                    .invoke(jedis, key, start, end);
                            
                            columnNames.add("key");
                            columnNames.add("value");
                            columnNames.add("score");
                            
                            // 解析带分数的结果
                            // 这里实现比较复杂，简化处理
                            List<Object> row = new ArrayList<>();
                            row.add(key);
                            row.add("请使用Jedis客户端获取完整的zrangeWithScores结果");
                            row.add(0.0);
                            data.add(row);
                        } else {
                            zrange = (Set<String>) jedisClass
                                    .getMethod("zrange", String.class, long.class, long.class)
                                    .invoke(jedis, key, start, end);
                            
                            columnNames.add("key");
                            columnNames.add("value");
                            
                            int index = 0;
                            for (String member : zrange) {
                                List<Object> row = new ArrayList<>();
                                row.add(key);
                                row.add(member);
                                data.add(row);
                                index++;
                            }
                        }
                    }
                    break;
                    
                case "keys":
                    if (parts.length > 1) {
                        String pattern = parts[1];
                        Set<String> keys = (Set<String>) jedisClass
                                .getMethod("keys", String.class)
                                .invoke(jedis, pattern);
                        
                        columnNames.add("key");
                        columnNames.add("type");
                        
                        for (String key : keys) {
                            String type = (String) jedisClass
                                    .getMethod("type", String.class)
                                    .invoke(jedis, key);
                            
                            List<Object> row = new ArrayList<>();
                            row.add(key);
                            row.add(type);
                            data.add(row);
                        }
                    }
                    break;
                    
                default:
                    throw new Exception("不支持的Redis命令: " + cmd);
            }
            
            result.put("success", true);
            result.put("columns", columnNames);
            result.put("data", data);
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 执行非查询SQL语句
     */
    public static Map<String, Object> executeUpdate(ConnectionConfig config, String sql) {
        if ("redis".equalsIgnoreCase(config.getDatabaseType())) {
            return executeRedisCommand(config, sql);
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try (Connection conn = getConnection(config);
             Statement stmt = conn.createStatement()) {
            
            int affectedRows = stmt.executeUpdate(sql);
            
            result.put("success", true);
            result.put("affectedRows", affectedRows);
            
        } catch (SQLException e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 执行Redis命令
     */
    private static Map<String, Object> executeRedisCommand(ConnectionConfig config, String command) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Object jedis = getRedisConnection(config);
            Class<?> jedisClass = jedis.getClass();
            
            // 解析命令
            String[] parts = command.trim().split("\\s+");
            String cmd = parts.length > 0 ? parts[0].toLowerCase() : "";
            
            switch (cmd) {
                case "set":
                    if (parts.length > 2) {
                        String key = parts[1];
                        String value = parts[2];
                        String response = (String) jedisClass
                                .getMethod("set", String.class, String.class)
                                .invoke(jedis, key, value);
                        
                        result.put("success", "OK".equalsIgnoreCase(response));
                        result.put("affectedRows", 1);
                    } else {
                        throw new Exception("SET命令格式：SET key value");
                    }
                    break;
                    
                case "hset":
                    if (parts.length > 3) {
                        String key = parts[1];
                        String field = parts[2];
                        String value = parts[3];
                        
                        Long response = (Long) jedisClass
                                .getMethod("hset", String.class, String.class, String.class)
                                .invoke(jedis, key, field, value);
                        
                        result.put("success", true);
                        result.put("affectedRows", response);
                    } else {
                        throw new Exception("HSET命令格式：HSET key field value");
                    }
                    break;
                    
                case "lpush":
                case "rpush":
                    if (parts.length > 2) {
                        String key = parts[1];
                        String[] values = Arrays.copyOfRange(parts, 2, parts.length);
                        
                        Long response;
                        if ("lpush".equals(cmd)) {
                            response = (Long) jedisClass
                                    .getMethod("lpush", String.class, String[].class)
                                    .invoke(jedis, key, (Object) values);
                        } else {
                            response = (Long) jedisClass
                                    .getMethod("rpush", String.class, String[].class)
                                    .invoke(jedis, key, (Object) values);
                        }
                        
                        result.put("success", true);
                        result.put("affectedRows", response);
                    } else {
                        throw new Exception(cmd.toUpperCase() + "命令格式：" + cmd.toUpperCase() + " key value [value ...]");
                    }
                    break;
                    
                case "sadd":
                    if (parts.length > 2) {
                        String key = parts[1];
                        String[] members = Arrays.copyOfRange(parts, 2, parts.length);
                        
                        Long response = (Long) jedisClass
                                .getMethod("sadd", String.class, String[].class)
                                .invoke(jedis, key, (Object) members);
                        
                        result.put("success", true);
                        result.put("affectedRows", response);
                    } else {
                        throw new Exception("SADD命令格式：SADD key member [member ...]");
                    }
                    break;
                    
                case "zadd":
                    if (parts.length > 3 && parts.length % 2 == 0) {
                        String key = parts[1];
                        
                        // 将参数转换为分数-成员对
                        Map<String, Double> scoreMembers = new HashMap<>();
                        for (int i = 2; i < parts.length; i += 2) {
                            double score = Double.parseDouble(parts[i]);
                            String member = parts[i + 1];
                            scoreMembers.put(member, score);
                        }
                        
                        Long response = (Long) jedisClass
                                .getMethod("zadd", String.class, Map.class)
                                .invoke(jedis, key, scoreMembers);
                        
                        result.put("success", true);
                        result.put("affectedRows", response);
                    } else {
                        throw new Exception("ZADD命令格式：ZADD key score member [score member ...]");
                    }
                    break;
                    
                case "del":
                    if (parts.length > 1) {
                        String[] keys = Arrays.copyOfRange(parts, 1, parts.length);
                        
                        Long response = (Long) jedisClass
                                .getMethod("del", String[].class)
                                .invoke(jedis, (Object) keys);
                        
                        result.put("success", true);
                        result.put("affectedRows", response);
                    } else {
                        throw new Exception("DEL命令格式：DEL key [key ...]");
                    }
                    break;
                    
                default:
                    throw new Exception("不支持的Redis命令: " + cmd);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
} 