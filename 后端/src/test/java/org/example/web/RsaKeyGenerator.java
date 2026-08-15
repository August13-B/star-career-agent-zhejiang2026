package org.example.web;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import java.util.Base64;

/**
 * 生成 RSA 公钥+私钥 (仅运行一次)
 */
public class RsaKeyGenerator {
    public static void main(String[] args) {
        RSA rsa = new RSA();
        // 私钥（后端绝密保管）
        String privateKey = Base64.getEncoder().encodeToString(rsa.getPrivateKey().getEncoded());
        // 公钥（公开给前端）
        String publicKey = Base64.getEncoder().encodeToString(rsa.getPublicKey().getEncoded());

        System.out.println("RSA私钥：\n" + privateKey);
        System.out.println("\nRSA公钥：\n" + publicKey);
    }
}



