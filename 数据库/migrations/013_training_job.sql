-- ============================================================
-- 013 训练会话绑定「目标岗位」（模拟面试按用户意向/报告的岗位出题）
--
-- 背景：
--   模拟面试原先写死"Java 后端实习生"，与用户真实意向无关。
--   现要求：用「学生职业意向（career_intentions）→ 全库岗位搜索」得到候选岗位，
--   由用户在训练工作台选择；选定后岗位信息需要**冻结**在会话里，
--   供出题模板占位符（{{job}}/{{skills}}…）与评分对照使用。
--
-- 内容（幂等：已存在则跳过）：
--   training_session_config.job_id        bigint  训练绑定的岗位（job_info.id）
--   training_session_config.job_snapshot  json    岗位快照（冻结，避免岗位数据变更导致会话漂移）
-- ============================================================
USE `youthpath`;

SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'youthpath' AND table_name = 'training_session_config' AND column_name = 'job_id'
);
SET @ddl := IF(@exists = 0,
  'ALTER TABLE `training_session_config` ADD COLUMN `job_id` bigint NULL COMMENT ''训练绑定的岗位（job_info.id，可空）'', ADD COLUMN `job_snapshot` json NULL COMMENT ''岗位快照（冻结；出题占位符与评分对照用）''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
