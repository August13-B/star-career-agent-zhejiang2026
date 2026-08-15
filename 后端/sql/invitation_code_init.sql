-- 创建邀请码表
CREATE TABLE IF NOT EXISTS `invitation_code` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `user_role` tinyint NOT NULL COMMENT '用户角色：2-管理员，3-企业端，4-导师',
  `invitation_code` varchar(64) NOT NULL COMMENT '当前邀请码（随机生成）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_role`) COMMENT '每个角色只有一个邀请码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码表';

-- 初始化三个角色的邀请码（初始邀请码，注册后会重新生成）
-- 管理员（角色2）初始邀请码
INSERT INTO `invitation_code` (`id`, `user_role`, `invitation_code`) 
VALUES (1000000000000000001, 2, 'ADMIN_INVITE_001')
ON DUPLICATE KEY UPDATE `invitation_code` = VALUES(`invitation_code`);

-- 企业端（角色3）初始邀请码
INSERT INTO `invitation_code` (`id`, `user_role`, `invitation_code`) 
VALUES (1000000000000000002, 3, 'ENTERPRISE_INVITE_001')
ON DUPLICATE KEY UPDATE `invitation_code` = VALUES(`invitation_code`);

-- 导师（角色4）初始邀请码
INSERT INTO `invitation_code` (`id`, `user_role`, `invitation_code`) 
VALUES (1000000000000000003, 4, 'MENTOR_INVITE_001')
ON DUPLICATE KEY UPDATE `invitation_code` = VALUES(`invitation_code`);
