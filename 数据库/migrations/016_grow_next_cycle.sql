-- ============================================================
-- 016 成长计划「下一周期」+ 任务文件提交
--
-- 背景（个人成长新功能）：
--   任务提交文件即算完成 → 右侧选择本次完成难度（太简单/中等/困难）
--   → AI 依据「现有计划 + 完成度 + 难度」生成**下一个周期的计划**（新建一条 grow_plan）。
--
-- 内容（幂等）：
--   grow_plan.parent_plan_id  bigint  来源计划（下一周期指回上一周期，便于"只看最新"与幂等）
--   grow_plan.cycle_round     int     第几轮（首轮为 1）
--   说明：任务提交的文件元数据沿用已有列 grow_task.grow_recourse（JSON 数组）；
--         本次完成难度写入已有列 grow_task.effect_score（太简单=5 / 中等=3 / 困难=1）
--         与 grow_task.adjustment_reason（"本次完成难度：中等"），不新增列。
-- ============================================================
USE `youthpath`;

-- ① 来源计划
SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'youthpath' AND table_name = 'grow_plan' AND column_name = 'parent_plan_id'
);
SET @ddl := IF(@exists = 0,
  'ALTER TABLE `grow_plan` ADD COLUMN `parent_plan_id` bigint NULL DEFAULT NULL COMMENT ''来源计划ID（下一周期计划指回上一周期；首轮为空）''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ② 轮次
SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'youthpath' AND table_name = 'grow_plan' AND column_name = 'cycle_round'
);
SET @ddl := IF(@exists = 0,
  'ALTER TABLE `grow_plan` ADD COLUMN `cycle_round` int NULL DEFAULT 1 COMMENT ''周期轮次（首轮 1，下一周期 +1）''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ③ 便于按来源计划查询（幂等判断"是否已生成下一周期"）
SET @exists := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = 'youthpath' AND table_name = 'grow_plan' AND index_name = 'idx_grow_plan_parent'
);
SET @ddl := IF(@exists = 0,
  'ALTER TABLE `grow_plan` ADD INDEX `idx_grow_plan_parent` (`user_id`, `parent_plan_id`)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ④ 任务提交文件所在列（历史库可能没有；提交文件直接写这列，故一并确保存在）
SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'youthpath' AND table_name = 'grow_task' AND column_name = 'grow_recourse'
);
SET @ddl := IF(@exists = 0,
  'ALTER TABLE `grow_task` ADD COLUMN `grow_recourse` json NULL COMMENT ''学生提交的任务资源（JSON 数组）''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
