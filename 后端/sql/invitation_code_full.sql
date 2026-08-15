-- ====================== 邀请码表 - 完整创建与初始化脚本 ======================
-- 说明：本脚本用于创建邀请码表并初始化三个角色的邀请码
-- 执行顺序：先创建表，然后插入初始数据
-- 注意：如果表已存在，会先删除后重新创建（谨慎操作）

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 删除已存在的表（如果存在）
DROP TABLE IF EXISTS `invitation_code`;

-- 2. 创建邀请码表
CREATE TABLE `invitation_code` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `user_role` tinyint NOT NULL COMMENT '用户角色：2-管理员，3-企业端，4-导师',
  `invitation_code` varchar(64) NOT NULL COMMENT '当前邀请码（随机生成）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_role`) COMMENT '每个角色只有一个邀请码',
  UNIQUE KEY `uk_invitation_code` (`invitation_code`) COMMENT '邀请码全局唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码表（用于管理特殊角色注册）';

-- 3. 初始化三个角色的邀请码（使用随机生成的邀请码）
-- 注意：这里的邀请码是初始值，注册成功后会自动重新生成
INSERT INTO `invitation_code` (`id`, `user_role`, `invitation_code`) VALUES
-- 管理员（角色2）初始邀请码
(1000000000000000001, 2, 'ADMIN' || SUBSTRING(MD5(RAND()) FROM 1 FOR 8)),
-- 企业端（角色3）初始邀请码
(1000000000000000002, 3, 'ENTERPRISE' || SUBSTRING(MD5(RAND()) FROM 1 FOR 8)),
-- 导师（角色4）初始邀请码
(1000000000000000003, 4, 'MENTOR' || SUBSTRING(MD5(RAND()) FROM 1 FOR 8))
ON DUPLICATE KEY UPDATE `invitation_code` = VALUES(`invitation_code`);

-- 4. 查询验证
SELECT '邀请码表创建完成，初始数据如下：' AS '';
SELECT 
    CASE user_role 
        WHEN 2 THEN '管理员(2)' 
        WHEN 3 THEN '企业端(3)' 
        WHEN 4 THEN '导师(4)' 
    END AS '角色',
    invitation_code AS '初始邀请码',
    create_time AS '创建时间'
FROM invitation_code 
WHERE is_deleted = 0 
ORDER BY user_role;

SET FOREIGN_KEY_CHECKS = 1;

-- ====================== 使用说明 ======================
-- 1. 学生角色（1）不需要邀请码，直接注册即可
-- 2. 管理员、企业端、导师注册时需要提供对应角色的正确邀请码
-- 3. 每次成功注册后，系统会自动重新生成该角色的邀请码
-- 4. 邀请码验证流程：
--    a) 前端传递加密的userRole和invitationCode参数
--    b) 后端解密后验证邀请码是否正确
--    c) 验证成功后注册用户并重新生成邀请码
-- 5. 初始邀请码会在应用启动时自动生成（如果表中不存在）
