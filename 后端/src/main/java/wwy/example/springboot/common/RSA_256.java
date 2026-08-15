package wwy.example.springboot.common;


import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import wwy.example.springboot.config.RsaConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler.PrintlnLogErrorHandler.log;

/**
 * 加密工具类
 * 功能：提供 RSA + AES 混合加解密
 * 用途：前端加密传输 → 后端解密接收
 *       后端加密返回 → 前端解密展示
 *       新增：数据库密文存储/解密
 */
@Component("jobRSA256")
public class RSA_256 {
    // AES 加密算法格式（固定写法：AES/CBC/PKCS5Padding）
    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";

    // ===================== 新增：数据库固定AES密钥（从配置读取） =====================
    @Value("${aes-storage.key}")
    private String DB_AES_KEY;
    @Value("${aes-storage.iv}")
    private String DB_AES_IV;

    // Hutool 提供的 RSA 加密工具对象
    private final RSA rsa;

    /**
     * 构造方法：Spring 自动注入 RSA 公私钥配置
     * 从 RsaConfig 中读取私钥、公钥并初始化 RSA
     */
    public RSA_256(RsaConfig rsaConfig) {
        this.rsa = new RSA(rsaConfig.getPrivateKey(), rsaConfig.getPublicKey());
    }

    /**
     * RSA 公钥加密
     * @param data 要加密的字符串（一般是 AES 密钥）
     * @return 加密后的 Base64 字符串
     * 作用：前端使用公钥加密 AES 密钥
     */
    public String rsaEncrypt(String data) {
        return rsa.encryptBase64(data, KeyType.PublicKey);
    }

    /**
     * RSA 私钥解密
     * @param data 被 RSA 加密后的字符串
     * @return 解密后的原始字符串（AES 密钥）
     * 作用：后端用私钥解开 AES 密钥
     */
    public String rsaDecrypt(String data) {
        return rsa.decryptStr(data, KeyType.PrivateKey);
    }

    /**
     * AES 加密（CBC 模式）
     * @param data 要加密的真实数据（如 hello world）
     * @param aesKey AES 密钥
     * @param aesIv 偏移量（增加安全性）
     * @return Base64 格式的密文
     * 作用：加密业务数据
     */
    public String aesEncrypt(String data, String aesKey, String aesIv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(aesKey.getBytes(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(aesIv.getBytes());
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * AES 解密（CBC 模式）
     * @param data 加密后的 Base64 字符串
     * @param aesKey AES 密钥
     * @param aesIv 偏移量
     * @return 解密后的原始明文
     * 作用：后端解开前端传来的业务数据
     */
    public String aesDecrypt(String data, String aesKey, String aesIv) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(aesKey.getBytes(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(aesIv.getBytes());
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(data));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    // ===================== 【新增】数据库专用加解密（原有功能无任何影响） =====================

    /**
     * 数据库加密：明文 → 密文（存入数据库）
     */
    public String encryptForDB(String plainText) throws Exception {
        return aesEncrypt(plainText, DB_AES_KEY, DB_AES_IV);
    }

    /**
     * 数据库解密：密文 → 明文（从数据库读取）
     * 兼容明文存储：如果传入的字符串不是有效的 Base64 密文，则直接返回原字符串
     * @param cipherText 可能是密文（Base64）或明文
     * @return 解密后的明文，或原字符串
     */
    public String decryptFromDB(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }

        // 1. 尝试 Base64 解码，检查是否为合法 Base64
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(cipherText);
        } catch (IllegalArgumentException e) {
            // 不是 Base64 格式，视为明文，直接返回
            log.debug("数据不是 Base64 格式，视为明文直接返回: {}");
            return cipherText;
        }

        // 2. 尝试 AES 解密
        try {
            SecretKeySpec keySpec = new SecretKeySpec(DB_AES_KEY.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(DB_AES_IV.getBytes());
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密失败（可能是密钥错误或数据损坏），也视为明文返回原值，并记录错误
            log.error("AES 解密失败，返回原数据（可能是明文或损坏数据）: {}");
            return cipherText;
        }
    }
}