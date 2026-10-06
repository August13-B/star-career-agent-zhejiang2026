-- 三场景训练：模板快照、作品修订、画像应用及成长来源。只新增表，不清空已有记录。
USE `youthpath`;
CREATE TABLE IF NOT EXISTS training_session_config (
  session_id bigint NOT NULL PRIMARY KEY,
  template_snapshot json NOT NULL,
  difficulty varchar(20) NOT NULL DEFAULT 'standard',
  use_for_profile tinyint NOT NULL DEFAULT 0,
  baseline_score_id bigint NULL,
  baseline_profile_version int NULL,
  artifact_draft mediumtext NOT NULL COMMENT 'AES-GCM 密文',
  artifact_draft_version int NOT NULL DEFAULT 0,
  selected_artifact_id bigint NULL,
  CONSTRAINT fk_training_config_session FOREIGN KEY(session_id) REFERENCES training_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS training_artifact (
  id bigint NOT NULL PRIMARY KEY,
  session_id bigint NOT NULL,
  revision int NOT NULL,
  client_request_id varchar(80) NOT NULL,
  input_hash varchar(64) NOT NULL,
  content_json mediumtext NOT NULL COMMENT 'AES-GCM 密文；只追加修订',
  create_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_training_artifact_revision(session_id,revision),
  UNIQUE KEY uk_training_artifact_request(session_id,client_request_id),
  CONSTRAINT fk_training_artifact_session FOREIGN KEY(session_id) REFERENCES training_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS training_profile_application (
  session_id bigint NOT NULL PRIMARY KEY,
  status varchar(48) NOT NULL,
  policy_version varchar(40) NOT NULL DEFAULT 'latest_observation_v1',
  attempt int NOT NULL DEFAULT 0,
  before_scores mediumtext NULL COMMENT 'AES-GCM 密文',
  after_scores mediumtext NULL COMMENT 'AES-GCM 密文',
  profile_version int NULL,
  score_history_id bigint NULL,
  message varchar(255) NOT NULL,
  update_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_training_application_session FOREIGN KEY(session_id) REFERENCES training_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS training_growth_link (
  session_id bigint NOT NULL PRIMARY KEY,
  task_id bigint NOT NULL,
  plan_id bigint NOT NULL,
  suggestion_index int NOT NULL,
  create_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  CONSTRAINT fk_training_growth_session FOREIGN KEY(session_id) REFERENCES training_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
