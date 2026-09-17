-- ============================================================
-- 迁移：为百宝箱（Tbox）对接增加会话级/消息级映射字段
-- 适用：已有数据的 youthpath 库（无需重建表）
-- 执行：mysql -u root -p --default-character-set=utf8mb4 < 数据库/migrations/001_add_tbox_ids.sql
-- ============================================================
USE `youthpath`;

-- 会话级：本地对话 ↔ 百宝箱会话
ALTER TABLE `ai_conversation`
  ADD COLUMN `tbox_session_id`      varchar(64) NULL DEFAULT NULL COMMENT '百宝箱平台会话ID' AFTER `is_deleted`,
  ADD COLUMN `tbox_conversation_id` varchar(64) NULL DEFAULT NULL COMMENT '百宝箱应用会话ID' AFTER `tbox_session_id`;

-- 消息级：本地消息 ↔ 百宝箱消息/请求（用于历史回捞与三方对账）
ALTER TABLE `ai_message`
  ADD COLUMN `tbox_message_id` varchar(64) NULL DEFAULT NULL COMMENT '百宝箱平台消息ID' AFTER `sequence`,
  ADD COLUMN `tbox_request_id` varchar(64) NULL DEFAULT NULL COMMENT '百宝箱请求ID（用于三方对账）' AFTER `tbox_message_id`;
