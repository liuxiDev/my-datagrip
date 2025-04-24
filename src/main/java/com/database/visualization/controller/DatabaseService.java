package com.database.visualization.controller;

import com.database.visualization.model.ConnectionConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
import java.lang.reflect.Method;

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
        String dbKey = id + ":" + config.getDatabase(); // 使用ID和数据库名称组合作为key
        
        // 如果该数据库的连接池不存在，创建新的连接池
        if (!dataSources.containsKey(dbKey)) {
            // 如果存在旧的连接池（不同数据库），先关闭它
            if (dataSources.containsKey(id)) {
                HikariDataSource oldDataSource = dataSources.get(id);
                dataSources.remove(id);
                try {
                    oldDataSource.close();
                } catch (Exception e) {
                    // 忽略关闭错误
                }
            }
            
            HikariConfig hikariConfig = new HikariConfig();
            
            // 根据当前数据库名称动态构建 JDBC URL
            String originalUrl = config.getUrl();
            String databaseName = config.getDatabase();
            
            // 根据数据库类型构建正确的 URL
            if ("mysql".equalsIgnoreCase(config.getDatabaseType())) {
                // 处理MySQL URL: jdbc:mysql://host:port/database?params
                int dbStart = originalUrl.indexOf("/", 13); // 跳过jdbc:mysql://
                if (dbStart > 0) {
                    int paramStart = originalUrl.indexOf("?", dbStart);
                    if (paramStart > 0) {
                        // URL 带参数: jdbc:mysql://host:port/olddb?params
                        String baseUrl = originalUrl.substring(0, dbStart + 1) + databaseName + originalUrl.substring(paramStart);
                        hikariConfig.setJdbcUrl(baseUrl);
                    } else {
                        // URL 不带参数: jdbc:mysql://host:port/olddb
                        String baseUrl = originalUrl.substring(0, dbStart + 1) + databaseName;
                        hikariConfig.setJdbcUrl(baseUrl);
                    }
                } else {
                    // 如果URL格式不标准，使用原始URL
                    hikariConfig.setJdbcUrl(originalUrl);
                }
            } else {
                // 其他数据库类型使用原始URL
                hikariConfig.setJdbcUrl(originalUrl);
            }
            
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
            
            HikariDataSource dataSource = new HikariDataSource(hikariConfig);
            dataSources.put(dbKey, dataSource); // 使用新的组合key存储
        }
        
        return dataSources.get(dbKey).getConnection();
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
        // 关闭所有与该连接ID相关的连接池
        List<String> keysToRemove = new ArrayList<>();
        
        for (String key : dataSources.keySet()) {
            if (key.startsWith(connectionId + ":") || key.equals(connectionId)) {
                HikariDataSource dataSource = dataSources.get(key);
                try {
                    dataSource.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                keysToRemove.add(key);
            }
        }
        
        // 移除已关闭的连接池
        for (String key : keysToRemove) {
            dataSources.remove(key);
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
        // 关闭所有数据源连接池
        for (HikariDataSource dataSource : dataSources.values()) {
            try {
                dataSource.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        dataSources.clear();
        
        // 关闭所有Redis连接
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
                    
                case "keys":
                    if (parts.length > 1) {
                        String pattern = parts[1];
                        Set<String> keys = (Set<String>) jedisClass.getMethod("keys", String.class)
                                .invoke(jedis, pattern);
                        
                        columnNames.add("key");
                        
                        for (String key : keys) {
                            List<Object> row = new ArrayList<>();
                            row.add(key);
                            data.add(row);
                        }
                    }
                    break;
                    
                case "type":
                    if (parts.length > 1) {
                        String key = parts[1];
                        String type = (String) jedisClass.getMethod("type", String.class)
                                .invoke(jedis, key);
                        
                        columnNames.add("key");
                        columnNames.add("type");
                        
                        List<Object> row = new ArrayList<>();
                        row.add(key);
                        row.add(type);
                        data.add(row);
                    }
                    break;
                    
                case "ttl":
                    if (parts.length > 1) {
                        String key = parts[1];
                        Long ttl = (Long) jedisClass.getMethod("ttl", String.class)
                                .invoke(jedis, key);
                        
                        columnNames.add("key");
                        columnNames.add("ttl");
                        
                        List<Object> row = new ArrayList<>();
                        row.add(key);
                        row.add(ttl);
                        data.add(row);
                    }
                    break;
                    
                case "hgetall":
                    if (parts.length > 1) {
                        String key = parts[1];
                        Map<String, String> hash = (Map<String, String>) jedisClass.getMethod("hgetAll", String.class)
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
                        
                        List<String> list = (List<String>) jedisClass.getMethod("lrange", String.class, long.class, long.class)
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
                        Set<String> members = (Set<String>) jedisClass.getMethod("smembers", String.class)
                                .invoke(jedis, key);
                        
                        columnNames.add("member");
                        
                        for (String member : members) {
                            List<Object> row = new ArrayList<>();
                            row.add(member);
                            data.add(row);
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
                            zrange = (Set<String>) jedisClass.getMethod("zrangeWithScores", String.class, long.class, long.class)
                                    .invoke(jedis, key, start, end);
                            
                            columnNames.add("key");
                            columnNames.add("value");
                            columnNames.add("score");
                            
                            // 这里处理有点复杂，因为返回的是一个Set<Tuple>，需要特殊处理
                            // 简化处理，直接获取所有元素再获取分数
                            Set<String> members = (Set<String>) jedisClass.getMethod("zrange", String.class, long.class, long.class)
                                    .invoke(jedis, key, start, end);
                            
                            for (String member : members) {
                                Double score = (Double) jedisClass.getMethod("zscore", String.class, String.class)
                                        .invoke(jedis, key, member);
                                
                                List<Object> row = new ArrayList<>();
                                row.add(member);
                                row.add(score);
                                data.add(row);
                            }
                        } else {
                            zrange = (Set<String>) jedisClass.getMethod("zrange", String.class, long.class, long.class)
                                    .invoke(jedis, key, start, end);
                            
                            columnNames.add("member");
                            
                            for (String member : zrange) {
                                List<Object> row = new ArrayList<>();
                                row.add(member);
                                data.add(row);
                            }
                        }
                    }
                    break;
                    
                case "info":
                    String section = parts.length > 1 ? parts[1] : null;
                    String info;
                    
                    if (section != null) {
                        info = (String) jedisClass.getMethod("info", String.class)
                                .invoke(jedis, section);
                    } else {
                        info = (String) jedisClass.getMethod("info")
                                .invoke(jedis);
                    }
                    
                    columnNames.add("property");
                    columnNames.add("value");
                    
                    for (String line : info.split("\n")) {
                        if (line.startsWith("#") || line.trim().isEmpty()) {
                            continue;
                        }
                        
                        String[] parts2 = line.split(":");
                        if (parts2.length == 2) {
                            List<Object> row = new ArrayList<>();
                            row.add(parts2[0].trim());
                            row.add(parts2[1].trim());
                            data.add(row);
                        }
                    }
                    break;
                    
                default:
                    columnNames.add("警告");
                    List<Object> row = new ArrayList<>();
                    row.add("不支持的查询命令: " + cmd + "。请尝试Redis支持的命令，如GET、KEYS、TYPE、TTL、HGETALL、LRANGE、SMEMBERS、ZRANGE、INFO等。");
                    data.add(row);
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
                    
                case "expire":
                    if (parts.length > 2) {
                        String key = parts[1];
                        int seconds = Integer.parseInt(parts[2]);
                        
                        Long response = (Long) jedisClass
                                .getMethod("expire", String.class, int.class)
                                .invoke(jedis, key, seconds);
                        
                        result.put("success", response > 0);
                        result.put("affectedRows", response);
                    } else {
                        throw new Exception("EXPIRE命令格式：EXPIRE key seconds");
                    }
                    break;
                    
                case "select":
                    if (parts.length > 1) {
                        int dbIndex = Integer.parseInt(parts[1]);
                        
                        String response = (String) jedisClass
                                .getMethod("select", int.class)
                                .invoke(jedis, dbIndex);
                        
                        result.put("success", "OK".equalsIgnoreCase(response));
                        result.put("affectedRows", 0);
                        result.put("message", "已切换到数据库" + dbIndex);
                    } else {
                        throw new Exception("SELECT命令格式：SELECT index");
                    }
                    break;
                    
                default:
                    // 尝试通过反射调用其他命令
                    try {
                        // 将命令和参数分开
                        String[] params = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                        
                        // 尝试找到匹配的方法
                        Method method = findMatchingMethod(jedisClass, cmd, params.length);
                        
                        if (method != null) {
                            // 准备参数
                            Object[] methodParams = new Object[params.length];
                            for (int i = 0; i < params.length; i++) {
                                methodParams[i] = params[i];
                            }
                            
                            // 执行命令
                            Object response = method.invoke(jedis, methodParams);
                            
                            result.put("success", true);
                            result.put("affectedRows", response instanceof Number ? ((Number)response).intValue() : 0);
                            result.put("message", "命令执行成功: " + response);
                        } else {
                            throw new Exception("不支持的Redis命令: " + cmd);
                        }
                    } catch (NoSuchMethodException e) {
                        throw new Exception("不支持的Redis命令: " + cmd);
                    }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 查找匹配的方法
     */
    private static Method findMatchingMethod(Class<?> jedisClass, String methodName, int paramCount) throws NoSuchMethodException {
        for (Method method : jedisClass.getMethods()) {
            if (method.getName().equalsIgnoreCase(methodName) && 
                method.getParameterCount() == paramCount) {
                return method;
            }
        }
        throw new NoSuchMethodException("找不到匹配的方法: " + methodName + " 参数数量: " + paramCount);
    }
    
    /**
     * 获取Redis数据库列表
     */
    public static List<String> getRedisDatabases(ConnectionConfig config) {
        List<String> databases = new ArrayList<>();
        
        try {
            Object jedis = getRedisConnection(config);
            Class<?> jedisClass = jedis.getClass();
            
            // Redis默认有16个数据库(0-15)
            for (int i = 0; i < 16; i++) {
                // 尝试选择数据库
                try {
                    jedisClass.getMethod("select", int.class).invoke(jedis, i);
                    // 获取数据库中的键数量
                    Long keyCount = (Long) jedisClass.getMethod("dbSize").invoke(jedis);
                    
                    // 添加数据库到列表
                    databases.add("db" + i + " (" + keyCount + " keys)");
                } catch (Exception e) {
                    // 如果选择数据库失败，可能是权限问题或该数据库不可用
                    // 继续检查下一个数据库
                }
            }
            
            // 恢复到原来的数据库
            int dbIndex = 0;
            if (config.getDatabase() != null && !config.getDatabase().isEmpty()) {
                try {
                    dbIndex = Integer.parseInt(config.getDatabase());
                } catch (NumberFormatException e) {
                    // 使用默认数据库0
                }
            }
            jedisClass.getMethod("select", int.class).invoke(jedis, dbIndex);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return databases;
    }

    /**
     * 切换Redis数据库
     */
    public static boolean selectRedisDatabase(ConnectionConfig config, int dbIndex) {
        try {
            Object jedis = getRedisConnection(config);
            Class<?> jedisClass = jedis.getClass();
            
            // 选择数据库
            String response = (String) jedisClass.getMethod("select", int.class).invoke(jedis, dbIndex);
            return "OK".equalsIgnoreCase(response);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取数据库列表
     * @param config 连接配置
     * @return 数据库列表
     */
    public static List<String> getDatabases(ConnectionConfig config) {
        List<String> databases = new ArrayList<>();
        
        try {
            if ("mysql".equalsIgnoreCase(config.getDatabaseType())) {
                Map<String, Object> result = executeQuery(config, "SHOW DATABASES");
                if ((boolean) result.get("success")) {
                    List<List<Object>> data = (List<List<Object>>) result.get("data");
                    for (List<Object> row : data) {
                        if (row != null && !row.isEmpty() && row.get(0) != null) {
                            databases.add(row.get(0).toString());
                        }
                    }
                }
            } else if ("postgresql".equalsIgnoreCase(config.getDatabaseType())) {
                Map<String, Object> result = executeQuery(config, "SELECT datname FROM pg_database WHERE datistemplate = false");
                if ((boolean) result.get("success")) {
                    List<List<Object>> data = (List<List<Object>>) result.get("data");
                    for (List<Object> row : data) {
                        if (row != null && !row.isEmpty() && row.get(0) != null) {
                            databases.add(row.get(0).toString());
                        }
                    }
                }
            } else if ("sqlserver".equalsIgnoreCase(config.getDatabaseType())) {
                Map<String, Object> result = executeQuery(config, "SELECT name FROM sys.databases");
                if ((boolean) result.get("success")) {
                    List<List<Object>> data = (List<List<Object>>) result.get("data");
                    for (List<Object> row : data) {
                        if (row != null && !row.isEmpty() && row.get(0) != null) {
                            databases.add(row.get(0).toString());
                        }
                    }
                }
            } else if ("oracle".equalsIgnoreCase(config.getDatabaseType())) {
                Map<String, Object> result = executeQuery(config, "SELECT username FROM all_users ORDER BY username");
                if ((boolean) result.get("success")) {
                    List<List<Object>> data = (List<List<Object>>) result.get("data");
                    for (List<Object> row : data) {
                        if (row != null && !row.isEmpty() && row.get(0) != null) {
                            databases.add(row.get(0).toString());
                        }
                    }
                }
            } else if ("sqlite".equalsIgnoreCase(config.getDatabaseType())) {
                databases.add("main"); // SQLite只有一个主数据库
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return databases;
    }

    /**
     * 获取数据库统计信息
     * @param config 连接配置
     * @return 统计信息键值对
     */
    public static Map<String, String> getDatabaseStats(ConnectionConfig config) {
        Map<String, String> stats = new HashMap<>();
        
        try {
            stats.put("数据库类型", config.getDatabaseType());
            stats.put("主机", config.getHost());
            stats.put("端口", String.valueOf(config.getPort()));
            stats.put("数据库名", config.getDatabase());
            stats.put("状态", "已连接");
            
            // 获取表数量
            if ("mysql".equalsIgnoreCase(config.getDatabaseType())) {
                Map<String, Object> result = executeQuery(config, 
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + config.getDatabase() + "'");
                if ((boolean) result.get("success")) {
                    List<List<Object>> data = (List<List<Object>>) result.get("data");
                    if (!data.isEmpty() && !data.get(0).isEmpty()) {
                        stats.put("表数量", data.get(0).get(0).toString());
                    }
                }
                
                // 获取版本信息
                result = executeQuery(config, "SELECT VERSION()");
                if ((boolean) result.get("success")) {
                    List<List<Object>> data = (List<List<Object>>) result.get("data");
                    if (!data.isEmpty() && !data.get(0).isEmpty()) {
                        stats.put("服务器版本", data.get(0).get(0).toString());
                    }
                }
            } else if ("postgresql".equalsIgnoreCase(config.getDatabaseType())) {
                Map<String, Object> result = executeQuery(config, 
                        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema NOT IN ('pg_catalog', 'information_schema')");
                if ((boolean) result.get("success")) {
                    List<List<Object>> data = (List<List<Object>>) result.get("data");
                    if (!data.isEmpty() && !data.get(0).isEmpty()) {
                        stats.put("表数量", data.get(0).get(0).toString());
                    }
                }
                
                // 获取版本信息
                result = executeQuery(config, "SELECT version()");
                if ((boolean) result.get("success")) {
                    List<List<Object>> data = (List<List<Object>>) result.get("data");
                    if (!data.isEmpty() && !data.get(0).isEmpty()) {
                        stats.put("服务器版本", data.get(0).get(0).toString());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            stats.put("错误", e.getMessage());
        }
        
        return stats;
    }

    /**
     * 获取表的主键列
     * @param config 连接配置
     * @param tableName 表名
     * @return 主键列名列表
     */
    public static List<String> getPrimaryKeys(ConnectionConfig config, String tableName) {
        List<String> primaryKeys = new ArrayList<>();
        
        try {
            if ("mysql".equalsIgnoreCase(config.getDatabaseType())) {
                // 从tableName中提取schema和表名
                String schema = null;
                String table = tableName;
                
                if (tableName.contains(".")) {
                    String[] parts = tableName.split("\\.");
                    schema = parts[0];
                    table = parts[1];
                }
                
                // 对于MySQL, 使用一个更可靠的查询方式
                String sql;
                if (schema != null) {
                    sql = "SELECT k.COLUMN_NAME FROM information_schema.table_constraints t "
                        + "JOIN information_schema.key_column_usage k "
                        + "ON k.constraint_name = t.constraint_name "
                        + "WHERE k.table_schema = '" + schema + "' "
                        + "AND k.table_name = '" + table + "' "
                        + "AND t.constraint_type = 'PRIMARY KEY' "
                        + "ORDER BY k.ordinal_position";
                } else {
                    sql = "SELECT k.COLUMN_NAME FROM information_schema.table_constraints t "
                        + "JOIN information_schema.key_column_usage k "
                        + "ON k.constraint_name = t.constraint_name "
                        + "WHERE k.table_name = '" + table + "' "
                        + "AND t.constraint_type = 'PRIMARY KEY' "
                        + "ORDER BY k.ordinal_position";
                }
                
                Map<String, Object> result = executeQuery(config, sql);
                
                if ((boolean) result.get("success")) {
                    List<List<Object>> data = (List<List<Object>>) result.get("data");
                    
                    // 如果使用information_schema查询没有结果，尝试使用SHOW KEYS
                    if (data.isEmpty()) {
                        sql = "SHOW KEYS FROM " + tableName + " WHERE Key_name = 'PRIMARY'";
                        result = executeQuery(config, sql);
                        
                        if ((boolean) result.get("success")) {
                            data = (List<List<Object>>) result.get("data");
                            List<String> columns = (List<String>) result.get("columns");
                            
                            // 尝试找到Column_name列的索引
                            int columnNameIndex = -1;
                            for (int i = 0; i < columns.size(); i++) {
                                if ("Column_name".equalsIgnoreCase(columns.get(i)) ||
                                    "COLUMN_NAME".equalsIgnoreCase(columns.get(i))) {
                                    columnNameIndex = i;
                                    break;
                                }
                            }
                            
                            // 如果找不到Column_name列，MySQL的SHOW KEYS命令中该列通常在索引4
                            if (columnNameIndex < 0 && !data.isEmpty() && data.get(0).size() > 4) {
                                columnNameIndex = 4;  // MySQL中Column_name通常是第5列(索引4)
                            }
                            
                            if (columnNameIndex >= 0) {
                                for (List<Object> row : data) {
                                    if (row.size() > columnNameIndex && row.get(columnNameIndex) != null) {
                                        primaryKeys.add(row.get(columnNameIndex).toString());
                                    }
                                }
                            }
                        }
                    } else {
                        // 使用information_schema查询有结果
                        for (List<Object> row : data) {
                            if (!row.isEmpty() && row.get(0) != null) {
                                primaryKeys.add(row.get(0).toString());
                            }
                        }
                    }
                }
            } else if ("postgresql".equalsIgnoreCase(config.getDatabaseType())) {
                // 从tableName中提取schema和表名
                String schema = "public";
                String table = tableName;
                
                if (tableName.contains(".")) {
                    String[] parts = tableName.split("\\.");
                    schema = parts[0];
                    table = parts[1];
                }
                
                String sql = "SELECT a.attname " +
                        "FROM pg_index i " +
                        "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) " +
                        "WHERE i.indrelid = '" + table + "'::regclass AND i.indisprimary";
                
                Map<String, Object> result = executeQuery(config, sql);
                
                if ((boolean) result.get("success")) {
                    List<List<Object>> data = (List<List<Object>>) result.get("data");
                    for (List<Object> row : data) {
                        if (!row.isEmpty() && row.get(0) != null) {
                            primaryKeys.add(row.get(0).toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return primaryKeys;
    }

    /**
     * 连接数据库
     * @param config 连接配置
     * @return 是否连接成功
     */
    public static boolean connect(ConnectionConfig config) {
        return testConnection(config);
    }
} 