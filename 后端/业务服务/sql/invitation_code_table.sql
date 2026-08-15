-- 邀请码表结构
-- 用于管理管理员、企业端、导师的注册邀请码
-- 每个角色只有一个有效邀请码，每次使用后重新生成

CREATE TABLE IF NOT EXISTS `invitation_code` (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `user_role` tinyint NOT NULL COMMENT '用户角色：2-管理员，3-企业端，4-导师',
  `invitation_code` varchar(64) NOT NULL COMMENT '当前邀请码（随机生成）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_role`) COMMENT '每个角色只有一个邀请码',
  UNIQUE KEY `uk_invitation_code` (`invitation_code`) COMMENT '邀请码唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码表（用于管理特殊角色注册）';

-- 索引说明
-- 1. uk_user_role: 确保每个角色只有一个邀请码记录
-- 2. uk_invitation_code: 确保邀请码全局唯一

-- 注意：
-- 1. 学生角色（1）不需要邀请码，因此不在本表中
-- 2. 每次成功注册后，对应角色的邀请码会自动重新生成
-- 3. 邀请码使用随机生成算法，长度为12位字符（大写字母和数字）
-- 4. 逻辑删除用于软删除，一般不直接删除记录

-- 查询当前有效邀请码示例：
-- SELECT * FROM invitation_code WHERE is_deleted = 0;

-- 根据角色查询邀请码示例：
-- SELECT invitation_code FROM invitation_code WHERE user_role = 2 AND is_deleted = 0;

-- 验证邀请码示例：
-- SELECT COUNT(*) FROM invitation_code 
-- WHERE invitation_code = '提供的邀请码' AND user_role = 角色代码 AND is_deleted = 0;
