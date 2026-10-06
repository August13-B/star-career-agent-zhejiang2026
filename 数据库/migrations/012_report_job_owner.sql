-- Non-destructive: preserve all existing users and reports.
USE `youthpath`;
CREATE TABLE IF NOT EXISTS career_report_job (
  job_id varchar(128) NOT NULL,
  user_id bigint NOT NULL,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (job_id),
  KEY idx_report_job_user (user_id),
  CONSTRAINT fk_report_job_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
