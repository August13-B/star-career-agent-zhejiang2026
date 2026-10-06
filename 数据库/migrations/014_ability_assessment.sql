-- ============================================================
-- 014 能力补充测评（AI 出题 → 答题 → 追问 → 六维评分）
--
-- 背景：
--   1) 「基本情况」（硬实力四项）原来只在题库测评的提交里一次性计算，原始选项没落库，
--      导致拆出「个人中心只填硬实力」后无法复算/回显 → 增加 basic_options 存原始选项键。
--   2) 六维软素质测评从个人中心移到 /ai-score「能力补充测评」，由 AI 出题+追问+评分，
--      需要会话/题目/评分三张表（服务端限时、草稿断点续答、评分留痕）。
--
-- 内容（幂等）：
--   student_ability.basic_options         json    硬实力表单原始选项（可复算）
--   assessment_session / assessment_turn / assessment_evaluation
-- ============================================================
USE `youthpath`;

-- ① 硬实力表单原始选项（education / internshipMonths / skillLevel / certCount）
SET @exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = 'youthpath' AND table_name = 'student_ability' AND column_name = 'basic_options'
);
SET @ddl := IF(@exists = 0,
  'ALTER TABLE `student_ability` ADD COLUMN `basic_options` json NULL COMMENT ''硬实力表单原始选项（education/internshipMonths/skillLevel/certCount），用于规则表复算''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ② 测评会话
CREATE TABLE IF NOT EXISTS assessment_session (
  id bigint NOT NULL PRIMARY KEY,
  user_id bigint NOT NULL,
  status varchar(24) NOT NULL DEFAULT 'active' COMMENT 'active/processing/completed/review_required/canceled',
  version int NOT NULL DEFAULT 0 COMMENT '乐观并发版本（答题提交时校验）',
  objective_target int NOT NULL DEFAULT 0 COMMENT '计划客观题数',
  subjective_target int NOT NULL DEFAULT 0 COMMENT '计划主观题数',
  answered_count int NOT NULL DEFAULT 0 COMMENT '已作答轮次数（含追问轮）',
  plan json NULL COMMENT '内容计划（每轮：kind/dimension/限定秒数）',
  hard_snapshot json NULL COMMENT '开始时的硬实力四项（冻结）',
  context_snapshot json NULL COMMENT '开始时的画像/意向快照（冻结，供出题与评分）',
  baseline_score_id bigint NULL COMMENT '开始时的能力基线行（乐观并发保护）',
  baseline_profile_version int NULL,
  current_turn_id bigint NULL COMMENT '当前待作答的轮次',
  draft mediumtext NOT NULL COMMENT 'AES-GCM 密文（当前题草稿，断点续答）',
  draft_version int NOT NULL DEFAULT 0,
  client_request_id varchar(80) NOT NULL,
  create_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_assessment_request(user_id, client_request_id),
  KEY idx_assessment_user_time(user_id, create_time),
  CONSTRAINT fk_assessment_session_user FOREIGN KEY(user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ③ 测评轮次（一道题一轮；追问也是独立一轮）
CREATE TABLE IF NOT EXISTS assessment_turn (
  id bigint NOT NULL PRIMARY KEY,
  session_id bigint NOT NULL,
  ordinal int NOT NULL COMMENT '轮次序号（从 1 开始）',
  question_no int NOT NULL COMMENT '题号（追问与母题同号）',
  kind varchar(16) NOT NULL COMMENT 'objective/subjective',
  dimension varchar(32) NOT NULL COMMENT '所属维度（六维键名）',
  follow_up tinyint NOT NULL DEFAULT 0 COMMENT '是否为对上一答的追问',
  question mediumtext NOT NULL COMMENT 'AES-GCM 密文（题干）',
  options mediumtext NULL COMMENT 'AES-GCM 密文（客观题选项 JSON：text+score；仅后端用）',
  answer mediumtext NULL COMMENT 'AES-GCM 密文（用户作答文本）',
  chosen int NULL COMMENT '客观题所选下标',
  status varchar(16) NOT NULL DEFAULT 'asking' COMMENT 'asking/answered/timeout/skipped',
  limit_seconds int NOT NULL DEFAULT 0,
  started_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '本题下发时刻（服务端限时判定）',
  answered_at datetime(3) NULL,
  create_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_assessment_turn_ordinal(session_id, ordinal),
  KEY idx_assessment_turn_session(session_id, ordinal),
  CONSTRAINT fk_assessment_turn_session FOREIGN KEY(session_id) REFERENCES assessment_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ④ 测评评分（与训练同样的「证据制」）
CREATE TABLE IF NOT EXISTS assessment_evaluation (
  id bigint NOT NULL PRIMARY KEY,
  session_id bigint NOT NULL,
  status varchar(24) NOT NULL COMMENT 'valid/review_required',
  result_json mediumtext NOT NULL COMMENT 'AES-GCM 密文（六维分 + 证据 + 评语 + 建议）',
  objective_json mediumtext NULL COMMENT 'AES-GCM 密文（后端按选项分值复算的客观题维度分，用于审计）',
  message varchar(255) NULL,
  create_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_assessment_evaluation(session_id),
  CONSTRAINT fk_assessment_evaluation_session FOREIGN KEY(session_id) REFERENCES assessment_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
