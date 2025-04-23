package com.database.visualization.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * 安全工具类，提供密码加密和解密功能
 */
public class SecurityUtil {
    // 密钥 - 实际应用中应从配置或环境变量获取
    private static final String SECRET_KEY = "DatabaseVisualizerSecretKey";
    private static final String SALT = "DbVizSalt";
    private static final byte[] KEY_BYTES;
    
    static {
        try {
            // 生成密钥
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 128);
            SecretKey tmp = factory.generateSecret(spec);
            KEY_BYTES = tmp.getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("初始化加密工具失败", e);
        }
    }
    
    /**
     * 加密字符串
     */
    public static String encrypt(String strToEncrypt) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY_BYTES, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 解密字符串
     */
    public static String decrypt(String strToDecrypt) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY_BYTES, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 判断字符串是否已加密
     */
    public static boolean isEncrypted(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 