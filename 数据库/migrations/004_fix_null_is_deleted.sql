-- ============================================================
-- 修复 is_deleted 为 NULL 的旧行
--
-- 背景：画像/能力等表以 is_deleted 做逻辑删除过滤（WHERE is_deleted = 0），
--       但部分行插入时未写该列 → NULL → 查询永远匹配不到（保存成功却读不出来）
--
-- 注意：本脚本只更新「确实存在 is_deleted 列」的表。
--       student_profile_history / student_ability_score_history / ai_message
--       等表本身没有 is_deleted 列，早期版本误写会导致整脚本中断（ERROR 1054）。
-- ============================================================
USE `youthpath`;

UPDATE `student_profile`          SET is_deleted = '0' WHERE is_deleted IS NULL;
UPDATE `student_ability`          SET is_deleted = '0' WHERE is_deleted IS NULL;
UPDATE `student_ability_score`    SET is_deleted = 0   WHERE is_deleted IS NULL;
UPDATE `student_image`            SET is_deleted = '0' WHERE is_deleted IS NULL;
UPDATE `user_resource`            SET is_deleted = '0' WHERE is_deleted IS NULL;
UPDATE `career_report`            SET is_deleted = 0   WHERE is_deleted IS NULL;
UPDATE `match_record`             SET is_deleted = 0   WHERE is_deleted IS NULL;

SELECT 'student_profile' AS t, COUNT(*) AS visible_rows FROM `student_profile` WHERE is_deleted = '0'
UNION ALL SELECT 'student_ability', COUNT(*) FROM `student_ability` WHERE is_deleted = '0';
