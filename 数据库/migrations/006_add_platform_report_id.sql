-- ============================================================
-- 006 报告表增加平台报告ID（用于与百宝箱同步删除）
--
-- 背景：个人中心「报告管理」批量删除时，需要同时删除平台侧的报告；
--       平台报告ID（Appwrite 行 $id）在 done 帧返回，必须落库才能反查删除。
--
-- 幂等：先查 information_schema，列不存在才 ALTER（可重复执行）。
-- 执行：mysql -u root -p --default-character-set=utf8mb4 < 数据库/migrations/006_add_platform_report_id.sql
-- ============================================================
USE `youthpath`;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'youthpath'
    AND TABLE_NAME = 'career_report'
    AND COLUMN_NAME = 'platform_report_id'
);

SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `career_report` ADD COLUMN `platform_report_id` varchar(128) NULL DEFAULT NULL COMMENT ''百宝箱平台报告ID（Appwrite $id；用于两处同步删除）'' AFTER `report_content`',
  'SELECT ''platform_report_id 已存在，跳过'' AS note'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
