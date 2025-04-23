package com.database.visualization.utils;

/**
 * SQL格式化工具类
 */
public class SQLFormatter {
    
    /**
     * 格式化SQL语句
     */
    public static String format(String sql) {
        sql = sql.trim();
        
        // 替换多个空格为单个空格
        sql = sql.replaceAll("\\s+", " ");
        
        // 在关键字后添加换行
        String[] keywords = {
            "SELECT", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY",
            "JOIN", "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "OUTER JOIN",
            "UNION", "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM", 
            "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "CREATE INDEX", "DROP INDEX"
        };
        
        for (String keyword : keywords) {
            String pattern = "(?i)\\b" + keyword + "\\b";
            sql = sql.replaceAll(pattern, "\n" + keyword.toUpperCase());
        }
        
        // 在逗号后添加换行
        sql = sql.replaceAll(",", ",\n  ");
        
        return sql;
    }
} 