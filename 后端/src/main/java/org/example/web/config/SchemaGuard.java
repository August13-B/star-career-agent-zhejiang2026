package org.example.web.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 轻量 Schema 守卫：启动时校验并补齐「运行必需」的列。
 *
 * <p>背景：迁移脚本需要人工执行，容易漏跑。一旦漏跑就会出现
 * {@code Unknown column 'platform_report_id'} → 报告落库失败 → 个人中心看不到报告。
 * 这里在启动时自动检测并补列（幂等，列已存在则跳过）。
 */
@Component
public class SchemaGuard {

    private final JdbcTemplate jdbcTemplate;

    public SchemaGuard(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensure() {
        ensureColumn("career_report", "platform_report_id",
                "ALTER TABLE `career_report` ADD COLUMN `platform_report_id` varchar(128) NULL DEFAULT NULL "
                        + "COMMENT '百宝箱平台报告ID（Appwrite $id；用于两处同步删除）' AFTER `report_content`");
    }

    private void ensureColumn(String table, String column, String ddl) {
        try {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, table, column);
            if (n == null || n == 0) {
                jdbcTemplate.execute(ddl);
                System.out.println("[SchemaGuard] 已自动补齐列 " + table + "." + column);
            }
        } catch (Exception e) {
            System.err.println("[SchemaGuard] 校验/补齐列失败 " + table + "." + column + ": " + e);
        }
    }
}
