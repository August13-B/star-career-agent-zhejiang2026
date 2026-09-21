-- ============================================================
-- 009 用户昵称唯一索引（支持「昵称登录」）
--
-- 背景：登录新增「昵称」方式（账号/邮箱/昵称自动识别）。若昵称可重复，
--       登录时无法确定是哪个账号，故给 nickname 加唯一索引；
--       注册接口同步校验昵称是否已被使用。
--
-- 幂等：先用 information_schema 判断索引是否存在，再决定是否 ALTER。
-- 注意：若历史数据存在重复昵称，本迁移会失败（当前库无重复）——
--       可先手工把重复昵称改为唯一后再执行。
-- ============================================================
USE `youthpath`;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'user'
    AND INDEX_NAME = 'uk_user_nickname'
);
SET @ddl := IF(@exists = 0,
  'ALTER TABLE `user` ADD UNIQUE INDEX `uk_user_nickname`(`nickname` ASC) USING BTREE COMMENT ''昵称唯一索引''',
  'SELECT ''uk_user_nickname already exists''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
