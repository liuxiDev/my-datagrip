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
            // 首先尝试Base64解码
            byte[] decodedBytes = Base64.getDecoder().decode(str);
            
            // 如果能成功解码，尝试用我们的密钥解密
            // 注意这里不要抛出异常，因为我们只是在判断，不是真的要解密
            SecretKeySpec secretKey = new SecretKeySpec(KEY_BYTES, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            try {
                // 尝试解密，看是否能成功
                cipher.doFinal(decodedBytes);
                // 如果没有抛出异常，说明可能是由我们加密的
                return true;
            } catch (Exception e) {
                // 解密失败，可能不是我们加密的
                return false;
            }
        } catch (Exception e) {
            // 如果解码失败，说明不是Base64编码，肯定不是加密的
            return false;
        }
    }
} 