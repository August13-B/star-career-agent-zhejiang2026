-- ============================================================
-- 清理「旧密钥写入」的用户数据，仅保留业务/参考数据
--
-- 背景：早期画像数据由另一套密钥加密，现无法解密（bad key）。
--       清理后重新录入的数据将使用当前 .env 密钥，可正常解密。
--
-- 【保留】job_info / job_requirement_profile / job_* / invitation_code / user(账号)
-- 【清理】画像、能力、匹配、报告、对话、成长计划、面试、图片、资源等用户数据
--
-- ⚠️ 执行前请先备份：见 数据库/README.md
-- ============================================================
USE `youthpath`;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `student_profile_history`;
DELETE FROM `student_profile`;
DELETE FROM `student_ability`;
DELETE FROM `student_ability_score_history`;
DELETE FROM `student_ability_score`;
DELETE FROM `match_detail`;
DELETE FROM `match_record`;
DELETE FROM `career_report_history`;
DELETE FROM `career_report`;
DELETE FROM `ai_feedback`;
DELETE FROM `ai_message`;
DELETE FROM `ai_conversation`;
DELETE FROM `grow_task`;
DELETE FROM `grow_plan`;
DELETE FROM `interview_record`;
DELETE FROM `student_image`;
DELETE FROM `user_resource`;
DELETE FROM `biz_feedback`;
DELETE FROM `mentor_student_relation`;
DELETE FROM `ai_task`;

SET FOREIGN_KEY_CHECKS = 1;

-- 自检：应只剩业务数据
SELECT 'job_info' AS t, COUNT(*) AS rows_left FROM `job_info`
UNION ALL SELECT 'job_requirement_profile', COUNT(*) FROM `job_requirement_profile`
UNION ALL SELECT 'invitation_code', COUNT(*) FROM `invitation_code`
UNION ALL SELECT 'user', COUNT(*) FROM `user`
UNION ALL SELECT 'student_profile', COUNT(*) FROM `student_profile`
UNION ALL SELECT 'student_ability_score', COUNT(*) FROM `student_ability_score`;
