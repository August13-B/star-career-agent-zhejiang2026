-- ============================================================
-- 015 画像更新策略（B 口径）+ 变更留痕
--
-- 背景：
--   1) 首次评估（当前画像尚未被任何评估更新过）允许相对基线 60 大幅调整：
--      ±35 → 落在 25~95；
--   2) 之后再次评估：单维单次变化 ≤10，且**降低必须带理由**（无理由则保持原值）；
--   3) 简历 PDF 解析可补全硬实力四项，单项加分 ≤+15（只加不减）；
--   4) 「画像在执行完任何操作后自动更新并提示」需要记录最近一次变更的
--      来源 / 理由 / 明细（各维度 before→after、是否首次）。
--
-- 内容（幂等）：
--   student_ability_score.change_source   varchar(32)  最近一次变更来源
--     baseline / resume / assessment / interview_training / report
--   student_ability_score.change_detail   text         最近一次变更明细（JSON）
--     {"firstTime":true,"source":"resume","reason":"简历解析补充实习经历","deltas":{"internship":{"before":60,"after":72}}}
--
-- 说明：历史表 student_ability_score_history 已有 change_reason（记录"能力补充测评 / 职场训练 …"），
--       本次不再改历史表结构。
-- ============================================================
USE `youthpath`;

-- ① 最近一次变更来源
SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'youthpath' AND table_name = 'student_ability_score' AND column_name = 'change_source'
);
SET @ddl := IF(@exists = 0,
  'ALTER TABLE `student_ability_score` ADD COLUMN `change_source` varchar(32) NULL DEFAULT NULL COMMENT ''最近一次变更来源：baseline/resume/assessment/interview_training/report''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ② 最近一次变更明细（JSON：firstTime/source/reason/deltas）
SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'youthpath' AND table_name = 'student_ability_score' AND column_name = 'change_detail'
);
SET @ddl := IF(@exists = 0,
  'ALTER TABLE `student_ability_score` ADD COLUMN `change_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT ''最近一次变更明细（JSON：firstTime/source/reason/deltas）''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
