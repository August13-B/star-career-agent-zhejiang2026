package org.example.web.tool;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA_256 {

    /**
     * 对明文进行SHA-256哈希加密
     *
     * @param input 明文字符串
     * @return SHA-256哈希值的十六进制字符串
     */
    public static String sha256(String input) {
        // 如果输入为空，直接返回空
        if (input == null) {
            return null;
        }

        try {
            // 创建MessageDigest实例，指定使用SHA-256算法
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 将输入字符串转换为字节数组并计算哈希值
            byte[] hashBytes = digest.digest(input.getBytes());

            // 将字节数组转换为十六进制字符串
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不存在", e);
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     *
     * @param hash 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            // 将每个字节转换为两位十六进制数，不足两位前面补零
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}