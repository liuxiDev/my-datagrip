package com.database.visualization.utils;

import com.database.visualization.model.ConnectionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

/**
 * 安全功能测试类
 * 这个类仅用于测试，生产环境可以删除
 */
public class SecurityTest {
    
    public static void main(String[] args) {
        testPasswordEncryption();
        testConnectionConfigSerialization();
    }
    
    private static void testPasswordEncryption() {
        System.out.println("------ 测试密码加密/解密 ------");
        
        // 测试不同的加密设置
        String[] algorithms = {"AES", "DES"};
        String[] strengths = {"128位", "192位", "256位"};
        String[] passwords = {"123456", "test password", "复杂密码!@#$%^&*"};
        
        for (String algorithm : algorithms) {
            for (String strength : strengths) {
                // 跳过无效组合
                if (algorithm.equals("DES") && !strength.equals("128位")) {
                    continue;
                }
                
                System.out.println("算法: " + algorithm + ", 强度: " + strength);
                SecurityManager.updateSecuritySettings(true, algorithm, strength);
                
                for (String password : passwords) {
                    String encrypted = SecurityManager.encryptPassword(password);
                    String decrypted = SecurityManager.decryptPassword(encrypted);
                    
                    System.out.println("原始密码: " + password);
                    System.out.println("加密密码: " + encrypted);
                    System.out.println("解密密码: " + decrypted);
                    System.out.println("解密是否成功: " + password.equals(decrypted));
                    System.out.println();
                }
            }
        }
    }
    
    private static void testConnectionConfigSerialization() {
        System.out.println("------ 测试连接配置序列化 ------");
        
        try {
            // 创建连接配置
            ConnectionConfig config = new ConnectionConfig();
            config.setName("测试连接");
            config.setDatabaseType("mysql");
            config.setHost("localhost");
            config.setPort(3306);
            config.setDatabase("testdb");
            config.setUsername("root");
            config.setPassword("test123");
            
            // 序列化
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(config);
            System.out.println("序列化后的JSON:");
            System.out.println(json);
            
            // 确认密码已被加密
            System.out.println("密码是否已加密: " + !json.contains("\"password\":\"test123\""));
            
            // 反序列化
            ConnectionConfig deserializedConfig = mapper.readValue(json, ConnectionConfig.class);
            System.out.println("反序列化后的对象:");
            System.out.println("名称: " + deserializedConfig.getName());
            System.out.println("用户名: " + deserializedConfig.getUsername());
            
            // 确认密码可以被正确解密
            String decryptedPassword = deserializedConfig.getPassword();
            System.out.println("解密后的密码: " + decryptedPassword);
            System.out.println("密码解密是否成功: " + "test123".equals(decryptedPassword));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 