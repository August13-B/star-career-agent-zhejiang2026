package org.example.web.tool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 账号生成工具类
 * 生成规则：前缀 + 时间戳 + 随机数
 * 前缀可以是：STU（学生）、TCH（教师）、ADM（管理员）等
 * 格式：STU20240331123456789 + 3位随机数 = STU20240331123456789123
 */
public class AccountGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Random RANDOM = new Random();

    /**
     * 生成学生账号
     * @return 唯一的学生账号
     */
    public static String generateStudentAccount() {
        return generateAccount("STU");
    }

    /**
     * 生成教师账号
     * @return 唯一的教师账号
     */
    public static String generateTeacherAccount() {
        return generateAccount("TCH");
    }

    /**
     * 生成管理员账号
     * @return 唯一的管理员账号
     */
    public static String generateAdminAccount() {
        return generateAccount("ADM");
    }

    /**
     * 生成企业账号
     * @return 唯一的企业账号
     */
    public static String generateEnterpriseAccount() {
        return generateAccount("ENT");
    }

    /**
     * 根据前缀生成账号
     * @param prefix 账号前缀，如STU、TCH等
     * @return 唯一的账号
     */
    public static String generateAccount(String prefix) {
        // 获取当前时间，格式为yyyyMMddHHmmssSSS（17位）
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        // 生成3位随机数（100-999）
        int randomNum = 100 + RANDOM.nextInt(900);
        // 组合：前缀 + 时间戳 + 随机数
        return prefix + timestamp + randomNum;
    }

    /**
     * 生成默认账号（学生账号）
     * @return 默认的学生账号
     */
    public static String generateDefaultAccount() {
        return generateStudentAccount();
    }
}
