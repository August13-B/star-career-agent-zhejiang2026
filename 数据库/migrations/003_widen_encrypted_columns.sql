-- ============================================================
-- 加宽「加密存储」字段的列长度
--
-- 背景：后端对学生画像等字段做 RSA 加密后入库，密文约 172 字符，
--       而部分列仅 varchar(32)/varchar(50)，导致
--       Data truncation: Data too long for column 'expected_salary'
--
-- 处理：把所有会被加密的短列统一放宽到 varchar(1000)
-- ============================================================
USE `youthpath`;

ALTER TABLE `student_profile`
  MODIFY COLUMN `expected_salary`     varchar(1000) NULL DEFAULT NULL COMMENT '期望薪资范围（加密）',
  MODIFY COLUMN `education`           varchar(1000) NULL DEFAULT NULL COMMENT '学历（加密）',
  MODIFY COLUMN `target_city`         varchar(1000) NULL DEFAULT NULL COMMENT '目标城市（加密）',
  MODIFY COLUMN `industry_preference` varchar(1000) NULL DEFAULT NULL COMMENT '行业偏好（加密）',
  MODIFY COLUMN `grade`               varchar(1000) NULL DEFAULT NULL,
  MODIFY COLUMN `work_type_preference` varchar(1000) NULL DEFAULT NULL COMMENT '工作类型偏好';
