-- ============================================================
-- 005 画像表结构收敛（幂等，可重复执行）
--
-- 背景：`数据库结构.sql` 曾经存在「结构漂移」——
--   建表脚本里 expected_salary 仍是 varchar(50)、education 仍是 varchar(32)，
--   而 RSA 密文约 172 字符 → 保存画像报
--   `Data too long for column 'expected_salary'`；
--   student_profile.is_deleted 是 varchar(500) NULL DEFAULT NULL，
--   而查询条件是 `is_deleted = 0` → 插入后永远查不到（保存成功却查不出来）。
--
-- 本脚本把「加宽加密列」与「is_deleted 收敛为 NOT NULL DEFAULT 0」合并为
-- 一次性、幂等的修复，`manage.py` 灌库后会自动执行，也可手动执行。
--
-- 执行：mysql -u root -p --default-character-set=utf8mb4 < 数据库/migrations/005_fix_profile_schema.sql
-- ============================================================
USE `youthpath`;

-- 1) 加宽会被 RSA 加密的列（RSA-1024 密文 ≈ 172 字符，原 varchar(32/50) 必爆）
ALTER TABLE `student_profile`
  MODIFY COLUMN `expected_salary`     varchar(1000) NULL DEFAULT NULL COMMENT '期望薪资范围（加密）',
  MODIFY COLUMN `education`           varchar(1000) NULL DEFAULT NULL COMMENT '学历（加密）',
  MODIFY COLUMN `target_city`         varchar(1000) NULL DEFAULT NULL COMMENT '目标城市（加密）',
  MODIFY COLUMN `industry_preference` varchar(1000) NULL DEFAULT NULL COMMENT '行业偏好（加密）',
  MODIFY COLUMN `grade`               varchar(1000) NULL DEFAULT NULL COMMENT '年级（加密）',
  MODIFY COLUMN `work_type_preference` varchar(1000) NULL DEFAULT NULL COMMENT '工作类型偏好（加密）';

-- 2) 补齐 is_deleted 为 NULL 的旧行（varchar 或 tinyint 都适用）
UPDATE `student_profile`               SET is_deleted = '0' WHERE is_deleted IS NULL;
UPDATE `student_ability`               SET is_deleted = '0' WHERE is_deleted IS NULL;
UPDATE `student_ability_score`         SET is_deleted = 0   WHERE is_deleted IS NULL;
UPDATE `student_image`                 SET is_deleted = '0' WHERE is_deleted IS NULL;
UPDATE `user_resource`                 SET is_deleted = '0' WHERE is_deleted IS NULL;
UPDATE `career_report`                 SET is_deleted = 0   WHERE is_deleted IS NULL;
UPDATE `match_record`                  SET is_deleted = 0   WHERE is_deleted IS NULL;

-- 3) 把 student_profile.is_deleted 收敛为 tinyint NOT NULL DEFAULT 0（与其它表一致）
ALTER TABLE `student_profile`
  MODIFY COLUMN `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除';
