-- ============================================================
-- 008 新增「成长任务完成情况记录」表（时间线，多条）
--
-- 背景：个人成长栏目里，用户可对某个成长任务**多次记录完成情况**（按时间线），
--       并最终打勾完成。grow_task.completion_detail 只能存一条，故新增记录表。
--
-- 幂等：CREATE TABLE IF NOT EXISTS，可重复执行。
-- ============================================================
USE `youthpath`;

CREATE TABLE IF NOT EXISTS `grow_task_record` (
  `id` bigint NOT NULL COMMENT '记录ID（雪花）',
  `task_id` bigint NOT NULL COMMENT '关联成长任务ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '完成情况记录内容',
  `record_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_record_task`(`task_id` ASC) USING BTREE,
  INDEX `idx_record_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_record_task` FOREIGN KEY (`task_id`) REFERENCES `grow_task` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成长任务完成情况记录（时间线，多条）' ROW_FORMAT = DYNAMIC;
