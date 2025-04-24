package com.database.visualization.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 安全管理器，用于密码加密解密和安全设置管理
 */
public class SecurityManager {
    private static final String SETTINGS_FILE = "settings.json";
    private static final String SECURITY_KEY = "security";
    private static final String ENCRYPT_ENABLED_KEY = "encryptEnabled";
    private static final String ALGORITHM_KEY = "algorithm";
    private static final String KEY_STRENGTH_KEY = "keyStrength";
    private static final String SECRET_KEY_BYTES = "secretKeyBytes";
    
    private static boolean encryptEnabled = true;
    private static String algorithm = "AES";
    private static String keyStrength = "128位";
    private static SecretKey secretKey;
    
    // 静态初始化
    static {
        loadSettings();
        initSecretKey();
    }
    
    /**
     * 加载安全设置
     */
    public static void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            // 如果文件不存在，使用默认设置并保存
            saveSettings();
            return;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(file);
            
            // 读取安全设置节点
            if (rootNode.has(SECURITY_KEY)) {
                JsonNode securityNode = rootNode.get(SECURITY_KEY);
                
                if (securityNode.has(ENCRYPT_ENABLED_KEY)) {
                    encryptEnabled = securityNode.get(ENCRYPT_ENABLED_KEY).asBoolean();
                }
                
                if (securityNode.has(ALGORITHM_KEY)) {
                    algorithm = securityNode.get(ALGORITHM_KEY).asText();
                }
                
                if (securityNode.has(KEY_STRENGTH_KEY)) {
                    keyStrength = securityNode.get(KEY_STRENGTH_KEY).asText();
                }
                
                // 加载密钥
                if (securityNode.has(SECRET_KEY_BYTES)) {
                    String keyBase64 = securityNode.get(SECRET_KEY_BYTES).asText();
                    byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
                    secretKey = new SecretKeySpec(keyBytes, algorithm);
                }
            }
        } catch (IOException e) {
            System.err.println("加载安全设置失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存安全设置
     */
    public static void saveSettings() {
        File file = new File(SETTINGS_FILE);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode;
            
            if (file.exists()) {
                // 如果文件存在，读取现有内容
                rootNode = mapper.readTree(file);
            } else {
                // 否则创建新的
                rootNode = mapper.createObjectNode();
            }
            
            // 创建或更新安全设置节点
            ObjectNode securityNode;
            if (rootNode.has(SECURITY_KEY)) {
                securityNode = (ObjectNode) rootNode.get(SECURITY_KEY);
            } else {
                securityNode = mapper.createObjectNode();
                ((ObjectNode) rootNode).set(SECURITY_KEY, securityNode);
            }
            
            securityNode.put(ENCRYPT_ENABLED_KEY, encryptEnabled);
            securityNode.put(ALGORITHM_KEY, algorithm);
            securityNode.put(KEY_STRENGTH_KEY, keyStrength);
            
            // 保存密钥
            if (secretKey != null) {
                String keyBase64 = Base64.getEncoder().encodeToString(secretKey.getEncoded());
                securityNode.put(SECRET_KEY_BYTES, keyBase64);
            }
            
            // 写入文件
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, rootNode);
        } catch (IOException e) {
            System.err.println("保存安全设置失败: " + e.getMessage());
        }
    }
    
    /**
     * 初始化加密密钥
     */
    private static void initSecretKey() {
        try {
            if (!encryptEnabled) {
                return;
            }
            
            // 如果已有密钥，则不需要重新生成
            if (secretKey != null) {
                return;
            }
            
            int keySize;
            switch (keyStrength) {
                case "128位":
                    keySize = 128;
                    break;
                case "192位":
                    keySize = 192;
                    break;
                case "256位":
                    keySize = 256;
                    break;
                default:
                    keySize = 128;
            }
            
            // AES只支持128, 192, 256位密钥
            // DES只支持56位密钥
            // 根据选择的算法调整密钥大小
            if (algorithm.equals("DES") && keySize > 56) {
                keySize = 56;
            }
            
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            SecureRandom secureRandom = new SecureRandom();
            keyGen.init(keySize, secureRandom);
            secretKey = keyGen.generateKey();
            
            // 生成新密钥后保存设置
            saveSettings();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("初始化加密密钥失败: " + e.getMessage());
        }
    }
    
    /**
     * 加密密码
     * @param password 原始密码
     * @return 加密后的密码
     */
    public static String encryptPassword(String password) {
        if (password == null || password.isEmpty() || !encryptEnabled) {
            return password;
        }
        
        try {
            if (secretKey == null) {
                initSecretKey();
            }
            
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(password.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            System.err.println("密码加密失败: " + e.getMessage());
            return password;
        }
    }
    
    /**
     * 解密密码
     * @param encryptedPassword 加密后的密码
     * @return 解密后的密码
     */
    public static String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty() || !encryptEnabled) {
            return encryptedPassword;
        }
        
        // 尝试判断是否为加密字符串
        if (!isBase64(encryptedPassword)) {
            // 不是Base64编码，肯定不是加密字符串，返回原始值
            return encryptedPassword;
        }
        
        try {
            if (secretKey == null) {
                initSecretKey();
                // 如果此时仍然没有密钥，无法解密
                if (secretKey == null) {
                    return encryptedPassword;
                }
            }
            
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedPassword);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            // 解密失败，可能是其他类型的Base64字符串，或者不是用当前密钥加密的
            System.err.println("密码解密失败: " + e.getMessage());
            return encryptedPassword;
        }
    }
    
    /**
     * 更新安全设置
     * @param encryptEnabled 是否启用加密
     * @param algorithm 加密算法
     * @param keyStrength 密钥强度
     */
    public static void updateSecuritySettings(boolean encryptEnabled, String algorithm, String keyStrength) {
        boolean needNewKey = !SecurityManager.algorithm.equals(algorithm) || 
                             !SecurityManager.keyStrength.equals(keyStrength);
        
        SecurityManager.encryptEnabled = encryptEnabled;
        SecurityManager.algorithm = algorithm;
        SecurityManager.keyStrength = keyStrength;
        
        // 如果算法或密钥强度改变，需要重新生成密钥
        if (needNewKey) {
            secretKey = null;
            initSecretKey();
        } else {
            saveSettings();
        }
    }
    
    /**
     * 获取是否启用密码加密
     */
    public static boolean isEncryptEnabled() {
        return encryptEnabled;
    }
    
    /**
     * 获取加密算法
     */
    public static String getAlgorithm() {
        return algorithm;
    }
    
    /**
     * 获取密钥强度
     */
    public static String getKeyStrength() {
        return keyStrength;
    }
    
    /**
     * 检查是否为旧版加密的密码，如果是则使用新方法重新加密
     * @param encryptedPassword 可能是旧版加密的密码
     * @return 使用新方法加密的密码或原始密码
     */
    public static String migratePasswordIfNeeded(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return encryptedPassword;
        }
        
        // 首先检查是否可能是Base64编码的字符串
        if (!isBase64(encryptedPassword)) {
            // 不是Base64，可能是明文，直接加密
            return encryptPassword(encryptedPassword);
        }
        
        try {
            // 尝试判断是否为旧格式加密密码
            if (SecurityUtil.isEncrypted(encryptedPassword)) {
                try {
                    // 尝试使用旧方法解密
                    String decryptedPassword = SecurityUtil.decrypt(encryptedPassword);
                    if (decryptedPassword != null) {
                        // 如果解密成功，说明是旧版加密，使用新方法重新加密
                        return encryptPassword(decryptedPassword);
                    }
                } catch (Exception e) {
                    // 忽略旧版解密异常，按原密码处理
                    System.err.println("迁移旧密码失败: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // 忽略一切异常，当作不是旧版密码处理
            System.err.println("检测旧密码格式失败: " + e.getMessage());
        }
        
        // 如果到这里，可能是以下情况之一：
        // 1. 不是旧版加密的密码
        // 2. 是旧版加密但无法解密（密钥或格式有问题）
        // 直接返回原密码
        return encryptedPassword;
    }
    
    /**
     * 检查字符串是否为有效的Base64编码
     */
    private static boolean isBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        try {
            // 尝试解码
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            // 解码失败，不是有效的Base64
            return false;
        }
    }
} 