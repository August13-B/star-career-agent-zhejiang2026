package org.example.web.service.training;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Uses the existing storage key, with a fresh GCM nonce per training write. */
@Component
public class TrainingContentCipher {
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public TrainingContentCipher(@Value("${aes-storage.key}") String key) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    public String encrypt(String text) {
        try {
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return "g1:" + Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array());
        } catch (Exception e) {
            throw new TrainingException(503, "STORAGE_ENCRYPTION", "训练数据加密失败，请检查后端存储密钥配置");
        }
    }

    public String decrypt(String text) {
        if (text == null) return "";
        try {
            if (!text.startsWith("g1:")) throw new IllegalArgumentException();
            ByteBuffer bytes = ByteBuffer.wrap(Base64.getDecoder().decode(text.substring(3)));
            byte[] nonce = new byte[12];
            bytes.get(nonce);
            byte[] encrypted = new byte[bytes.remaining()];
            bytes.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new TrainingException(503, "STORAGE_DECRYPTION", "训练数据解密失败，请核对原存储密钥");
        }
    }
}
