package com.database.visualization.utils;

import com.database.visualization.controller.DatabaseService;
import com.database.visualization.model.ConnectionConfig;

import java.util.List;
import java.util.Map;

/**
 * 主键查询测试类
 * 仅用于测试，可以在生产环境中删除
 */
public class PrimaryKeyTest {
    
    public static void main(String[] args) {
        // 创建测试用的连接配置
        ConnectionConfig config = new ConnectionConfig();
        config.setName("Test MySQL");
        config.setDatabaseType("mysql");
        config.setHost("localhost");
        config.setPort(3306);
        config.setDatabase("test_db");
        config.setUsername("root");
        config.setPassword("password");
        
        // 表名 - 替换成实际存在的表名
        String tableName = "test_db.test_table";
        
        // 测试主键查询
        testPrimaryKeyQuery(config, tableName);
    }
    
    private static void testPrimaryKeyQuery(ConnectionConfig config, String tableName) {
        System.out.println("===== 开始测试主键查询 =====");
        System.out.println("表名: " + tableName);
        
        try {
            // 测试直接查询
            System.out.println("\n----- 使用information_schema查询 -----");
            String schema = null;
            String table = tableName;
            
            if (tableName.contains(".")) {
                String[] parts = tableName.split("\\.");
                schema = parts[0];
                table = parts[1];
            }
            
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
            
            Map<String, Object> result = DatabaseService.executeQuery(config, sql);
            printQueryResult(result);
            
            // 测试SHOW KEYS查询
            System.out.println("\n----- 使用SHOW KEYS查询 -----");
            sql = "SHOW KEYS FROM " + tableName + " WHERE Key_name = 'PRIMARY'";
            result = DatabaseService.executeQuery(config, sql);
            printQueryResult(result);
            
            // 测试getPrimaryKeys方法
            System.out.println("\n----- 使用getPrimaryKeys方法 -----");
            List<String> primaryKeys = DatabaseService.getPrimaryKeys(config, tableName);
            System.out.println("主键列表: " + primaryKeys);
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n===== 测试结束 =====");
    }
    
    private static void printQueryResult(Map<String, Object> result) {
        boolean success = (boolean) result.get("success");
        System.out.println("查询成功: " + success);
        
        if (success) {
            List<String> columns = (List<String>) result.get("columns");
            List<List<Object>> data = (List<List<Object>>) result.get("data");
            
            System.out.println("列名: " + columns);
            System.out.println("数据行数: " + data.size());
            
            for (int i = 0; i < data.size(); i++) {
                System.out.println("行 " + (i + 1) + ": " + data.get(i));
            }
        } else {
            System.out.println("错误: " + result.get("error"));
        }
    }
} 