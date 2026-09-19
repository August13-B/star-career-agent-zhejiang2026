-- ============================================================
-- 006 启用「成长规划/任务」表（报告结构化 1/3/5 年目标落库）
--
-- 背景：
--   职业报告第 6 段（report_composition）会输出结构化的 1/3/5 年目标
--   （<<<GOALS_JSON>>>…<<<END_GOALS_JSON>>>），后端解析后写入
--   grow_plan（每 horizon 一行）+ grow_task（每 keyAction 一条），用于后续
--   「计划跟踪 / 动态调整 / 完成情况」。
--
-- 但现有表结构与"报告直出目标"不匹配：
--   1) grow_plan.match_id 是 NOT NULL 且外键到 match_record，而报告可以不经匹配直接生成；
--   2) plan_type 原注释是「1-短期(3个月)/2-中期(6个月)/3-长期(1年)」，与 1/3/5 年不一致。
--
-- 本脚本（幂等）：
--   - grow_plan.match_id         → 允许 NULL
--   - grow_plan.plan_type 注释   → 1=1年 / 2=3年 / 3=5年
-- ============================================================
USE `youthpath`;

ALTER TABLE `grow_plan`
  MODIFY COLUMN `match_id` bigint NULL DEFAULT NULL COMMENT '来自匹配结果（可空：报告可不经匹配直接生成）';

ALTER TABLE `grow_plan`
  MODIFY COLUMN `plan_type` tinyint NOT NULL DEFAULT 1 COMMENT '计划类型：1-1年，2-3年，3-5年';
