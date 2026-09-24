-- 面试训练首个闭环。幂等新增，不修改或清空已有用户数据。
USE `youthpath`;

CREATE TABLE IF NOT EXISTS training_session (
  id bigint NOT NULL PRIMARY KEY,
  user_id bigint NOT NULL,
  template_id varchar(80) NOT NULL,
  status varchar(24) NOT NULL DEFAULT 'active',
  answered_count int NOT NULL DEFAULT 0,
  version int NOT NULL DEFAULT 0,
  draft mediumtext NOT NULL COMMENT 'AES-GCM 密文',
  draft_version int NOT NULL DEFAULT 0,
  client_request_id varchar(80) NOT NULL,
  create_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_training_create(user_id,client_request_id),
  KEY idx_training_user_time(user_id,create_time),
  CONSTRAINT fk_training_user FOREIGN KEY(user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS training_run (
  id bigint NOT NULL PRIMARY KEY,
  session_id bigint NOT NULL,
  operation varchar(16) NOT NULL,
  status varchar(16) NOT NULL DEFAULT 'queued',
  attempt int NOT NULL DEFAULT 1,
  client_request_id varchar(80) NOT NULL,
  input_hash varchar(64) NOT NULL,
  request_json mediumtext NOT NULL COMMENT '冻结请求的 AES-GCM 密文',
  response_message_id bigint NULL,
  error_code varchar(64) NULL,
  error_message varchar(255) NULL,
  raw_result mediumtext NULL COMMENT '平台原始结构化结果的 AES-GCM 密文',
  create_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_training_request(session_id,client_request_id),
  KEY idx_training_run_state(status,create_time),
  CONSTRAINT fk_training_run_session FOREIGN KEY(session_id) REFERENCES training_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS training_turn (
  id bigint NOT NULL PRIMARY KEY,
  session_id bigint NOT NULL,
  run_id bigint NULL,
  role varchar(16) NOT NULL,
  ordinal int NOT NULL,
  content mediumtext NOT NULL COMMENT 'AES-GCM 密文',
  status varchar(16) NOT NULL,
  create_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_training_ordinal(session_id,ordinal),
  CONSTRAINT fk_training_turn_session FOREIGN KEY(session_id) REFERENCES training_session(id),
  CONSTRAINT fk_training_turn_run FOREIGN KEY(run_id) REFERENCES training_run(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS training_evaluation (
  id bigint NOT NULL PRIMARY KEY,
  session_id bigint NOT NULL,
  run_id bigint NOT NULL,
  status varchar(24) NOT NULL,
  result_json mediumtext NOT NULL COMMENT '校验后结果的 AES-GCM 密文',
  message varchar(255) NOT NULL,
  create_time datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_training_evaluation(session_id),
  CONSTRAINT fk_training_eval_session FOREIGN KEY(session_id) REFERENCES training_session(id),
  CONSTRAINT fk_training_eval_run FOREIGN KEY(run_id) REFERENCES training_run(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
