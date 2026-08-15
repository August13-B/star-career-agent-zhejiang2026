/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : youthpath

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 22/04/2026 02:31:05
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_conversation
-- ----------------------------
DROP TABLE IF EXISTS `ai_conversation`;
CREATE TABLE `ai_conversation`  (
  `id` bigint NOT NULL COMMENT '对话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `conversation_type` tinyint NOT NULL DEFAULT 1 COMMENT '对话类型：1-职业规划咨询，2-模拟面试，3-技能提升指导，4-其他',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '对话标题',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-进行中，2-已结束',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_conversation_type`(`conversation_type` ASC) USING BTREE,
  CONSTRAINT `fk_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI对话主表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_feedback
-- ----------------------------
DROP TABLE IF EXISTS `ai_feedback`;
CREATE TABLE `ai_feedback`  (
  `id` bigint NOT NULL COMMENT '反馈ID',
  `grow_task_id` bigint NOT NULL COMMENT '关联成长任务ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `feedback_type` tinyint NOT NULL COMMENT '反馈类型：1-点赞，2-点踩，3-详细反馈',
  `score` tinyint NULL DEFAULT NULL COMMENT '评分（1-5分）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '反馈内容',
  `improvement_suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '改进建议',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_feedback_message`(`grow_task_id` ASC) USING BTREE,
  INDEX `idx_feedback_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_feedback_message` FOREIGN KEY (`grow_task_id`) REFERENCES `ai_message` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI对话反馈表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_message`;
CREATE TABLE `ai_message`  (
  `id` bigint NOT NULL COMMENT '消息ID',
  `conversation_id` bigint NOT NULL COMMENT '对话ID',
  `message_type` tinyint NOT NULL COMMENT '消息类型：1-用户输入，2-AI回复，3-系统提示',
  `content_type` tinyint NOT NULL DEFAULT 1 COMMENT '内容类型：1-文本，2-语音，3-图片',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `audio_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '语音文件地址',
  `image_url` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `context_info` json NULL COMMENT '上下文信息（JSON格式）',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '使用的模型名称',
  `response_time` int NULL DEFAULT NULL COMMENT '响应时间（毫秒）',
  `sequence` int NOT NULL COMMENT '消息序号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_conversation`(`conversation_id` ASC) USING BTREE,
  INDEX `idx_message_sequence`(`conversation_id` ASC, `sequence` ASC) USING BTREE,
  CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_task
-- ----------------------------
DROP TABLE IF EXISTS `ai_task`;
CREATE TABLE `ai_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `plan_id` bigint NULL DEFAULT NULL COMMENT '关联成长计划ID',
  `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `task_type` int NULL DEFAULT NULL COMMENT '任务类型：1技能 2证书 3项目 4实习',
  `task_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '任务说明',
  `resource` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '学习资源链接（JSON格式存储）',
  `target_ability` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标能力维度',
  `expected_outcome` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '预期成果',
  `start_date` date NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期',
  `progress` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '执行进度(%)',
  `status` int NULL DEFAULT 0 COMMENT '0未开始 1进行中 2已完成 3已暂停 4已延期',
  `completion_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '完成情况详情',
  `completion_date` datetime NULL DEFAULT NULL COMMENT '实际完成日期',
  `effect_evaluation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '效果评估',
  `effect_score` int NULL DEFAULT NULL COMMENT '效果评分（1-5分）',
  `adjustment_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '动态调整原因',
  `adjustment_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '调整历史记录（JSON格式）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_plan_id`(`plan_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_start_date`(`start_date` ASC) USING BTREE,
  INDEX `idx_end_date`(`end_date` ASC) USING BTREE,
  INDEX `idx_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_user_deleted`(`user_id` ASC, `is_deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for biz_feedback
-- ----------------------------
DROP TABLE IF EXISTS `biz_feedback`;
CREATE TABLE `biz_feedback`  (
  `id` bigint NOT NULL,
  `from_user_id` bigint NOT NULL COMMENT '导师ID',
  `to_user_id` bigint NOT NULL COMMENT '学生ID',
  `match_id` bigint NULL DEFAULT NULL COMMENT '关联匹配结果',
  `report_id` bigint NULL DEFAULT NULL COMMENT '关联职业报告',
  `feedback_type` tinyint NOT NULL COMMENT '1简历点评 2规划建议 3面试评价',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈内容',
  `score` tinyint NULL DEFAULT NULL COMMENT '评分（1-5分）',
  `is_read` tinyint NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_to_user`(`to_user_id` ASC) USING BTREE,
  INDEX `idx_from_user`(`from_user_id` ASC) USING BTREE,
  INDEX `idx_match`(`match_id` ASC) USING BTREE,
  INDEX `idx_report`(`report_id` ASC) USING BTREE,
  CONSTRAINT `fk_feedback_from` FOREIGN KEY (`from_user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_feedback_match` FOREIGN KEY (`match_id`) REFERENCES `match_record` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_feedback_report` FOREIGN KEY (`report_id`) REFERENCES `career_report` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_feedback_to` FOREIGN KEY (`to_user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '导师反馈评价表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for biz_mentor
-- ----------------------------
DROP TABLE IF EXISTS `biz_mentor`;
CREATE TABLE `biz_mentor`  (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `mentor_type` tinyint NOT NULL COMMENT '1高校老师 2企业HR 3技术面试官',
  `real_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `company` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `position` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `specialty` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '擅长岗位（JSON格式）',
  `intro` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_mentor_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '导师/面试官表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for career_report
-- ----------------------------
DROP TABLE IF EXISTS `career_report`;
CREATE TABLE `career_report`  (
  `id` bigint NOT NULL COMMENT '报告ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `match_id` bigint NULL DEFAULT NULL COMMENT '关联匹配结果ID',
  `report_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '报告名称',
  `report_type` tinyint NOT NULL DEFAULT 1 COMMENT '报告类型：1-职业探索报告，2-目标设定报告，3-完整职业规划报告',
  `version` int NOT NULL DEFAULT 1 COMMENT '报告版本号',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-草稿，2-已生成，3-已修改，4-已确认',
  `report_content` json NOT NULL COMMENT '报告内容（JSON格式，支持高度自定义）',
  `template_id` bigint NULL DEFAULT NULL COMMENT '使用的模板ID',
  `feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用户反馈',
  `feedback_score` tinyint NULL DEFAULT NULL COMMENT '反馈评分（1-5分）',
  `feedback_time` datetime NULL DEFAULT NULL COMMENT '反馈时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_report_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_report_match`(`match_id` ASC) USING BTREE,
  CONSTRAINT `fk_report_match` FOREIGN KEY (`match_id`) REFERENCES `match_record` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_report_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '职业报告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for career_report_history
-- ----------------------------
DROP TABLE IF EXISTS `career_report_history`;
CREATE TABLE `career_report_history`  (
  `id` bigint NOT NULL COMMENT '历史记录ID',
  `report_id` bigint NOT NULL COMMENT '关联报告ID',
  `version` int NOT NULL COMMENT '版本号',
  `report_content` json NOT NULL COMMENT '报告内容快照',
  `change_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_history_report`(`report_id` ASC) USING BTREE,
  INDEX `idx_history_report_version`(`report_id` ASC, `version` ASC) USING BTREE,
  CONSTRAINT `fk_history_report` FOREIGN KEY (`report_id`) REFERENCES `career_report` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '职业报告历史版本表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for grow_plan
-- ----------------------------
DROP TABLE IF EXISTS `grow_plan`;
CREATE TABLE `grow_plan`  (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `match_id` bigint NOT NULL COMMENT '来自匹配结果',
  `report_id` bigint NULL DEFAULT NULL COMMENT '关联职业报告ID',
  `target_job` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标岗位',
  `plan_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计划名称',
  `plan_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '成长建议',
  `plan_type` tinyint NOT NULL DEFAULT 1 COMMENT '计划类型：1-短期（3个月），2-中期（6个月），3-长期（1年）',
  `start_date` date NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期',
  `total_status` tinyint NULL DEFAULT 0 COMMENT '0未开始 1进行中 2已完成 3已暂停',
  `progress` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '总进度(%)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_match`(`match_id` ASC) USING BTREE,
  INDEX `idx_report`(`report_id` ASC) USING BTREE,
  CONSTRAINT `fk_plan_match` FOREIGN KEY (`match_id`) REFERENCES `match_record` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_plan_report` FOREIGN KEY (`report_id`) REFERENCES `career_report` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_plan_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成长规划表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for grow_task
-- ----------------------------
DROP TABLE IF EXISTS `grow_task`;
CREATE TABLE `grow_task`  (
  `id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `task_type` tinyint NOT NULL COMMENT '1技能 2证书 3项目 4实习',
  `task_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '任务说明',
  `resource` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '学习资源链接（JSON格式存储）',
  `target_ability` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标能力维度',
  `expected_outcome` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '预期成果',
  `start_date` date NULL DEFAULT NULL COMMENT '开始日期',
  `end_date` date NULL DEFAULT NULL COMMENT '结束日期',
  `progress` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '执行进度(%)',
  `status` tinyint NULL DEFAULT 0 COMMENT '0未开始 1进行中 2已完成 3已暂停 4已延期',
  `completion_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '完成情况详情',
  `completion_date` datetime NULL DEFAULT NULL COMMENT '实际完成日期',
  `effect_evaluation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '效果评估',
  `effect_score` tinyint NULL DEFAULT NULL COMMENT '效果评分（1-5分）',
  `adjustment_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '动态调整原因',
  `adjustment_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '调整历史记录（JSON格式）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  `grow_recourse` json NULL COMMENT '学生提交的任务资源，JSON格式',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_plan`(`plan_id` ASC) USING BTREE,
  CONSTRAINT `fk_task_plan` FOREIGN KEY (`plan_id`) REFERENCES `grow_plan` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成长任务表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for interview_record
-- ----------------------------
DROP TABLE IF EXISTS `interview_record`;
CREATE TABLE `interview_record`  (
  `id` bigint NOT NULL COMMENT '主键编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '面试题目',
  `answer_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用户回答',
  `score` double NULL DEFAULT NULL COMMENT '评分结果',
  `suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '优化建议',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_interview_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '模拟面试记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for invitation_code
-- ----------------------------
DROP TABLE IF EXISTS `invitation_code`;
CREATE TABLE `invitation_code`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成）',
  `user_role` tinyint NOT NULL COMMENT '用户角色：2-管理员，3-企业端，4-导师',
  `invitation_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '当前邀请码（随机生成）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_role`(`user_role` ASC) USING BTREE COMMENT '每个角色只有一个邀请码',
  UNIQUE INDEX `uk_invitation_code`(`invitation_code` ASC) USING BTREE COMMENT '邀请码全局唯一'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '邀请码表（用于管理特殊角色注册）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for job_hard_requirement
-- ----------------------------
DROP TABLE IF EXISTS `job_hard_requirement`;
CREATE TABLE `job_hard_requirement`  (
  `id` bigint NOT NULL COMMENT '主键',
  `job_id` bigint NOT NULL COMMENT '关联岗位主表ID',
  `education_requirement` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '1.学历背景要求',
  `internship_requirement` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '2.实习经历要求',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_hard_job`(`job_id` ASC) USING BTREE,
  CONSTRAINT `fk_hard_job` FOREIGN KEY (`job_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位硬门槛需求表(10维2项)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for job_info
-- ----------------------------
DROP TABLE IF EXISTS `job_info`;
CREATE TABLE `job_info`  (
  `id` bigint NOT NULL COMMENT '非自增主键（雪花算法生成）',
  `job_id` bigint NULL DEFAULT NULL COMMENT '关联岗位要求画像表的id（初始为空，后绑定）',
  `job_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '岗位名称',
  `address` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工作地址',
  `salary_range` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '薪资范围',
  `company_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司名称',
  `industry` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属行业',
  `company_scale` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司规模',
  `company_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司类型',
  `job_code` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '岗位编码',
  `job_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '岗位详情',
  `update_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新日期',
  `company_detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '公司详情',
  `job_source_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '岗位来源地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_job_profile_id`(`job_id` ASC) USING BTREE COMMENT '关联岗位画像表索引',
  CONSTRAINT `fk_job_info_profile` FOREIGN KEY (`job_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位信息表（存储JD原始数据，无入门门槛）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for job_market_info
-- ----------------------------
DROP TABLE IF EXISTS `job_market_info`;
CREATE TABLE `job_market_info`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `job_id` bigint NOT NULL COMMENT '关联岗位主表ID',
  `industry` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属行业',
  `entry_threshold` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '应届生入门门槛',
  `market_supply_demand` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '市场供需情况：1-供大于求，2-供需平衡，3-供不应求',
  `supply_demand_ratio` decimal(5, 2) NULL DEFAULT NULL COMMENT '供需比',
  `salary_entry` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '应届生起薪范围',
  `salary_1year` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '1年经验薪资范围',
  `salary_3year` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '3年经验薪资范围',
  `salary_5year` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '5年经验薪资范围',
  `salary_trend` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '薪资发展趋势描述',
  `major_adaptation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '专业适配度（JSON格式存储各专业适配度）',
  `city_distribution` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '城市分布（JSON格式存储各城市占比）',
  `competition_level` tinyint NULL DEFAULT NULL COMMENT '竞争激烈程度：1-低，2-中，3-高，4-极高',
  `growth_rate` decimal(5, 2) NULL DEFAULT NULL COMMENT '岗位增长率(%)',
  `future_outlook` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '未来前景描述',
  `data_source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据来源',
  `data_update_date` date NULL DEFAULT NULL COMMENT '数据更新日期',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_market_job`(`job_id` ASC) USING BTREE,
  INDEX `idx_market_industry`(`industry` ASC) USING BTREE,
  CONSTRAINT `fk_market_job` FOREIGN KEY (`job_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位市场信息表（应届生适配、薪资趋势、供需情况）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for job_promotion_graph
-- ----------------------------
DROP TABLE IF EXISTS `job_promotion_graph`;
CREATE TABLE `job_promotion_graph`  (
  `id` bigint NOT NULL COMMENT '非自增主键（雪花算法生成）',
  `main_job_id` bigint NOT NULL COMMENT '主岗位id（关联岗位要求画像表）',
  `promotion_job1_id` bigint NULL DEFAULT NULL COMMENT '晋升岗位1-id（关联岗位要求画像表）',
  `promotion_job1_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '晋升岗位1-描述',
  `promotion_job1_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '晋升岗位1-技能差异',
  `promotion_job1_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '晋升岗位1-经验要求',
  `promotion_job1_learning_cycle` int NULL DEFAULT NULL COMMENT '晋升岗位1-学习周期（月）',
  `promotion_job2_id` bigint NULL DEFAULT NULL COMMENT '晋升岗位2-id（关联岗位要求画像表）',
  `promotion_job2_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '晋升岗位2-描述',
  `promotion_job2_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '晋升岗位2-技能差异',
  `promotion_job2_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '晋升岗位2-经验要求',
  `promotion_job2_learning_cycle` int NULL DEFAULT NULL COMMENT '晋升岗位2-学习周期（月）',
  `promotion_job3_id` bigint NULL DEFAULT NULL COMMENT '晋升岗位3-id（关联岗位要求画像表）',
  `promotion_job3_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '晋升岗位3-描述',
  `promotion_job3_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '晋升岗位3-技能差异',
  `promotion_job3_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '晋升岗位3-经验要求',
  `promotion_job3_learning_cycle` int NULL DEFAULT NULL COMMENT '晋升岗位3-学习周期（月）',
  `promotion_job4_id` bigint NULL DEFAULT NULL COMMENT '晋升岗位4-id（关联岗位要求画像表）',
  `promotion_job4_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '晋升岗位4-描述',
  `promotion_job4_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '晋升岗位4-技能差异',
  `promotion_job4_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '晋升岗位4-经验要求',
  `promotion_job4_learning_cycle` int NULL DEFAULT NULL COMMENT '晋升岗位4-学习周期（月）',
  `promotion_job5_id` bigint NULL DEFAULT NULL COMMENT '晋升岗位5-id（关联岗位要求画像表）',
  `promotion_job5_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '晋升岗位5-描述',
  `promotion_job5_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '晋升岗位5-技能差异',
  `promotion_job5_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '晋升岗位5-经验要求',
  `promotion_job5_learning_cycle` int NULL DEFAULT NULL COMMENT '晋升岗位5-学习周期（月）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_main_job_id`(`main_job_id` ASC) USING BTREE COMMENT '主岗位id索引',
  INDEX `fk_promotion_job1`(`promotion_job1_id` ASC) USING BTREE,
  INDEX `fk_promotion_job2`(`promotion_job2_id` ASC) USING BTREE,
  INDEX `fk_promotion_job3`(`promotion_job3_id` ASC) USING BTREE,
  INDEX `fk_promotion_job4`(`promotion_job4_id` ASC) USING BTREE,
  INDEX `fk_promotion_job5`(`promotion_job5_id` ASC) USING BTREE,
  CONSTRAINT `fk_main_job_promotion` FOREIGN KEY (`main_job_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_promotion_job1` FOREIGN KEY (`promotion_job1_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_promotion_job2` FOREIGN KEY (`promotion_job2_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_promotion_job3` FOREIGN KEY (`promotion_job3_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_promotion_job4` FOREIGN KEY (`promotion_job4_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_promotion_job5` FOREIGN KEY (`promotion_job5_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '垂直岗位晋升图谱表（无学历要求）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for job_requirement_profile
-- ----------------------------
DROP TABLE IF EXISTS `job_requirement_profile`;
CREATE TABLE `job_requirement_profile`  (
  `id` bigint NOT NULL COMMENT '岗位需求主键ID',
  `position_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位大类(技术/产品/运营)',
  `industry` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属行业',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '岗位基础描述',
  `level` tinyint NOT NULL DEFAULT 1 COMMENT '职级：1-入门 2-中级 3-高级',
  `hard_weight` decimal(5, 2) NULL DEFAULT 30.00 COMMENT '硬门槛权重(%)',
  `skill_weight` decimal(5, 2) NULL DEFAULT 40.00 COMMENT '专业技能权重(%)',
  `soft_weight` decimal(5, 2) NULL DEFAULT 30.00 COMMENT '软实力权重(%)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_job_category`(`category` ASC) USING BTREE,
  INDEX `idx_job_level`(`level` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位需求主表(基础信息+职级+权重)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for job_skill_requirement
-- ----------------------------
DROP TABLE IF EXISTS `job_skill_requirement`;
CREATE TABLE `job_skill_requirement`  (
  `id` bigint NOT NULL COMMENT '主键',
  `job_id` bigint NOT NULL COMMENT '关联岗位主表ID',
  `professional_skill` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '3.专业技能要求（JSON格式存储标签化数据）',
  `certificate_requirement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '4.证书资质要求（JSON格式存储标签化数据）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_skill_job`(`job_id` ASC) USING BTREE,
  CONSTRAINT `fk_skill_job` FOREIGN KEY (`job_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位专业技能需求表(10维2项)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for job_soft_requirement
-- ----------------------------
DROP TABLE IF EXISTS `job_soft_requirement`;
CREATE TABLE `job_soft_requirement`  (
  `id` bigint NOT NULL COMMENT '主键',
  `job_id` bigint NOT NULL COMMENT '关联岗位主表ID',
  `innovation_ability` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '5.创新能力要求',
  `learning_ability` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '6.学习能力要求',
  `pressure_resistance` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '7.抗压能力要求',
  `communication_ability` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '8.沟通能力要求',
  `problem_solving` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '9.问题解决能力要求',
  `teamwork_ability` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '10.团队协作能力要求',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_soft_job`(`job_id` ASC) USING BTREE,
  CONSTRAINT `fk_soft_job` FOREIGN KEY (`job_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位软实力需求表(10维6项)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for job_transfer_graph
-- ----------------------------
DROP TABLE IF EXISTS `job_transfer_graph`;
CREATE TABLE `job_transfer_graph`  (
  `id` bigint NOT NULL COMMENT '非自增主键（雪花算法生成）',
  `main_job_id` bigint NOT NULL COMMENT '主岗位id（关联岗位要求画像表）',
  `transfer_job1_id` bigint NOT NULL COMMENT '换岗岗位1-id（必填，关联岗位要求画像表）',
  `transfer_job1_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '换岗岗位1-描述（必填）',
  `transfer_job1_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位1-技能差异',
  `transfer_job1_education` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位1-学历要求',
  `transfer_job1_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位1-经验要求',
  `transfer_job1_learning_cycle` int NULL DEFAULT NULL COMMENT '换岗岗位1-学习周期（月）',
  `transfer_job1_difficulty` tinyint NULL DEFAULT NULL COMMENT '换岗岗位1-适配难度：1-低，2-中，3-高',
  `transfer_job2_id` bigint NOT NULL COMMENT '换岗岗位2-id（必填，关联岗位要求画像表）',
  `transfer_job2_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '换岗岗位2-描述（必填）',
  `transfer_job2_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位2-技能差异',
  `transfer_job2_education` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位2-学历要求',
  `transfer_job2_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位2-经验要求',
  `transfer_job2_learning_cycle` int NULL DEFAULT NULL COMMENT '换岗岗位2-学习周期（月）',
  `transfer_job2_difficulty` tinyint NULL DEFAULT NULL COMMENT '换岗岗位2-适配难度：1-低，2-中，3-高',
  `transfer_job3_id` bigint NULL DEFAULT NULL COMMENT '换岗岗位3-id（关联岗位要求画像表）',
  `transfer_job3_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位3-描述',
  `transfer_job3_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位3-技能差异',
  `transfer_job3_education` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位3-学历要求',
  `transfer_job3_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位3-经验要求',
  `transfer_job3_learning_cycle` int NULL DEFAULT NULL COMMENT '换岗岗位3-学习周期（月）',
  `transfer_job3_difficulty` tinyint NULL DEFAULT NULL COMMENT '换岗岗位3-适配难度：1-低，2-中，3-高',
  `transfer_job4_id` bigint NULL DEFAULT NULL COMMENT '换岗岗位4-id（关联岗位要求画像表）',
  `transfer_job4_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位4-描述',
  `transfer_job4_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位4-技能差异',
  `transfer_job4_education` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位4-学历要求',
  `transfer_job4_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位4-经验要求',
  `transfer_job4_learning_cycle` int NULL DEFAULT NULL COMMENT '换岗岗位4-学习周期（月）',
  `transfer_job4_difficulty` tinyint NULL DEFAULT NULL COMMENT '换岗岗位4-适配难度：1-低，2-中，3-高',
  `transfer_job5_id` bigint NULL DEFAULT NULL COMMENT '换岗岗位5-id（关联岗位要求画像表）',
  `transfer_job5_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位5-描述',
  `transfer_job5_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位5-技能差异',
  `transfer_job5_education` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位5-学历要求',
  `transfer_job5_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位5-经验要求',
  `transfer_job5_learning_cycle` int NULL DEFAULT NULL COMMENT '换岗岗位5-学习周期（月）',
  `transfer_job5_difficulty` tinyint NULL DEFAULT NULL COMMENT '换岗岗位5-适配难度：1-低，2-中，3-高',
  `transfer_job6_id` bigint NULL DEFAULT NULL COMMENT '换岗岗位6-id（关联岗位要求画像表）',
  `transfer_job6_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位6-描述',
  `transfer_job6_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位6-技能差异',
  `transfer_job6_education` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位6-学历要求',
  `transfer_job6_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位6-经验要求',
  `transfer_job6_learning_cycle` int NULL DEFAULT NULL COMMENT '换岗岗位6-学习周期（月）',
  `transfer_job6_difficulty` tinyint NULL DEFAULT NULL COMMENT '换岗岗位6-适配难度：1-低，2-中，3-高',
  `transfer_job7_id` bigint NULL DEFAULT NULL COMMENT '换岗岗位7-id（关联岗位要求画像表）',
  `transfer_job7_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位7-描述',
  `transfer_job7_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位7-技能差异',
  `transfer_job7_education` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位7-学历要求',
  `transfer_job7_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位7-经验要求',
  `transfer_job7_learning_cycle` int NULL DEFAULT NULL COMMENT '换岗岗位7-学习周期（月）',
  `transfer_job7_difficulty` tinyint NULL DEFAULT NULL COMMENT '换岗岗位7-适配难度：1-低，2-中，3-高',
  `transfer_job8_id` bigint NULL DEFAULT NULL COMMENT '换岗岗位8-id（关联岗位要求画像表）',
  `transfer_job8_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位8-描述',
  `transfer_job8_skill_diff` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '换岗岗位8-技能差异',
  `transfer_job8_education` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位8-学历要求',
  `transfer_job8_experience` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '换岗岗位8-经验要求',
  `transfer_job8_learning_cycle` int NULL DEFAULT NULL COMMENT '换岗岗位8-学习周期（月）',
  `transfer_job8_difficulty` tinyint NULL DEFAULT NULL COMMENT '换岗岗位8-适配难度：1-低，2-中，3-高',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识（0-未删除，1-已删除）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_main_job_transfer`(`main_job_id` ASC) USING BTREE COMMENT '主岗位id索引',
  INDEX `fk_transfer_job1`(`transfer_job1_id` ASC) USING BTREE,
  INDEX `fk_transfer_job2`(`transfer_job2_id` ASC) USING BTREE,
  INDEX `fk_transfer_job3`(`transfer_job3_id` ASC) USING BTREE,
  INDEX `fk_transfer_job4`(`transfer_job4_id` ASC) USING BTREE,
  INDEX `fk_transfer_job5`(`transfer_job5_id` ASC) USING BTREE,
  INDEX `fk_transfer_job6`(`transfer_job6_id` ASC) USING BTREE,
  INDEX `fk_transfer_job7`(`transfer_job7_id` ASC) USING BTREE,
  INDEX `fk_transfer_job8`(`transfer_job8_id` ASC) USING BTREE,
  CONSTRAINT `fk_main_job_transfer` FOREIGN KEY (`main_job_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_transfer_job1` FOREIGN KEY (`transfer_job1_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_transfer_job2` FOREIGN KEY (`transfer_job2_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_transfer_job3` FOREIGN KEY (`transfer_job3_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_transfer_job4` FOREIGN KEY (`transfer_job4_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_transfer_job5` FOREIGN KEY (`transfer_job5_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_transfer_job6` FOREIGN KEY (`transfer_job6_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_transfer_job7` FOREIGN KEY (`transfer_job7_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_transfer_job8` FOREIGN KEY (`transfer_job8_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '换岗路径图谱表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for match_detail
-- ----------------------------
DROP TABLE IF EXISTS `match_detail`;
CREATE TABLE `match_detail`  (
  `id` bigint NOT NULL,
  `match_id` bigint NOT NULL COMMENT '对应match_record.id',
  `dim_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'dim1~dim10',
  `dim_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '维度名称',
  `dim_type` tinyint NOT NULL COMMENT '1硬门槛 2专业技能 3软实力',
  `student_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '学生情况',
  `job_require` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '岗位要求',
  `score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '得分',
  `gap_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '差距分析',
  `improvement_suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '提升建议',
  `match_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '匹配结果说明',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_match`(`match_id` ASC) USING BTREE,
  CONSTRAINT `fk_detail_match` FOREIGN KEY (`match_id`) REFERENCES `match_record` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '人岗匹配10维详情表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for match_record
-- ----------------------------
DROP TABLE IF EXISTS `match_record`;
CREATE TABLE `match_record`  (
  `id` bigint NOT NULL COMMENT '匹配ID',
  `user_id` bigint NOT NULL COMMENT '学生ID',
  `job_id` bigint NOT NULL COMMENT '岗位ID',
  `level` tinyint NOT NULL COMMENT '职级 1入门 2中级 3高级',
  `hard_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '硬门槛得分',
  `skill_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '专业技能得分',
  `soft_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '软实力得分',
  `education_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '1.学历背景',
  `internship_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '2.实习经历',
  `professional_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '3.专业技能',
  `certificate_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '4.证书资质',
  `innovation_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '5.创新能力',
  `learning_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '6.学习能力',
  `pressure_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '7.抗压能力',
  `communication_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '8.沟通能力',
  `problem_solving_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '9.问题解决能力',
  `teamwork_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '10.团队协作能力',
  `total_score` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '综合总分',
  `match_result` tinyint NOT NULL COMMENT '1强烈推荐 2推荐 3一般 4不推荐',
  `match_status` tinyint NULL DEFAULT 0 COMMENT '匹配状态 0未生成 1生成中 2已完成 3失败',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_job`(`job_id` ASC) USING BTREE,
  CONSTRAINT `fk_match_job` FOREIGN KEY (`job_id`) REFERENCES `job_requirement_profile` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_match_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '人岗匹配主表(10维完整版，字段统一)' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for mentor_student_relation
-- ----------------------------
DROP TABLE IF EXISTS `mentor_student_relation`;
CREATE TABLE `mentor_student_relation`  (
  `id` bigint NOT NULL COMMENT '关系ID（雪花算法）',
  `mentor_id` bigint NOT NULL COMMENT '导师用户ID（关联user表，user_role=4）',
  `student_id` bigint NOT NULL COMMENT '学生用户ID（关联user表，user_role=1）',
  `relation_type` tinyint NOT NULL DEFAULT 4 COMMENT '关系类型：1-职业规划指导，2-简历辅导，3-面试辅导，4-综合管理',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '关系状态：1-当前有效，2-已解除，3-待确认',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `unbind_time` datetime NULL DEFAULT NULL COMMENT '解除绑定时间',
  `unbind_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '解除原因',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mentor_student`(`mentor_id` ASC, `student_id` ASC, `relation_type` ASC) USING BTREE COMMENT '同一导师对学生同一类型关系唯一',
  INDEX `idx_mentor_id`(`mentor_id` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_mentor_student_mentor` FOREIGN KEY (`mentor_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_mentor_student_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '导师学生关系表（定义导师管理学生的范围）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for student_ability
-- ----------------------------
DROP TABLE IF EXISTS `student_ability`;
CREATE TABLE `student_ability`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法生成，非自增）',
  `user_id` bigint NOT NULL COMMENT '关联用户ID（外键，一对一绑定用户）',
  `profile_id` bigint NULL DEFAULT NULL COMMENT '关联学生画像ID',
  `education_requirement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `internship_ability` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `professional_skill` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `certificate_requirement` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `innovation_ability` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `learning_ability` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `pressure_resistance` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `communication_ability` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `problem_solving` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `teamwork_ability` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_ability_user_id`(`user_id` ASC) USING BTREE COMMENT '一个学生仅一条能力记录',
  INDEX `idx_student_ability_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_student_ability_profile`(`profile_id` ASC) USING BTREE,
  CONSTRAINT `fk_student_ability_profile` FOREIGN KEY (`profile_id`) REFERENCES `student_profile` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_student_ability_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生能力维度表（10维标准统一）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for student_ability_score
-- ----------------------------
DROP TABLE IF EXISTS `student_ability_score`;
CREATE TABLE `student_ability_score`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `ability_id` bigint NOT NULL COMMENT '关联学生能力维度ID',
  `education_score` tinyint NULL DEFAULT 0 COMMENT '1.学历背景评分',
  `internship_score` tinyint NULL DEFAULT 0 COMMENT '2.实习经历评分',
  `professional_score` tinyint NULL DEFAULT 0 COMMENT '3.专业技能评分',
  `certificate_score` tinyint NULL DEFAULT 0 COMMENT '4.证书资质评分',
  `innovation_score` tinyint NULL DEFAULT 0 COMMENT '5.创新能力评分',
  `learning_score` tinyint NULL DEFAULT 0 COMMENT '6.学习能力评分',
  `pressure_score` tinyint NULL DEFAULT 0 COMMENT '7.抗压能力评分',
  `communication_score` tinyint NULL DEFAULT 0 COMMENT '8.沟通能力评分',
  `problem_solving_score` tinyint NULL DEFAULT 0 COMMENT '9.问题解决评分',
  `teamwork_score` tinyint NULL DEFAULT 0 COMMENT '10.团队协作评分',
  `total_score` decimal(5, 1) NULL DEFAULT 0.0 COMMENT '能力总评分',
  `industry_rank` int NULL DEFAULT NULL COMMENT '行业排名百分比',
  `peer_rank` int NULL DEFAULT NULL COMMENT '同届学生排名百分比',
  `score_type` tinyint NOT NULL DEFAULT 1 COMMENT '评分类型：1-系统自动评分 2-导师评分 3-企业评分',
  `score_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '评分评语',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_student_score_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_student_score_ability`(`ability_id` ASC) USING BTREE,
  CONSTRAINT `fk_student_score_ability` FOREIGN KEY (`ability_id`) REFERENCES `student_ability` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_student_score_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生能力评分表（10维标准统一）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for student_ability_score_history
-- ----------------------------
DROP TABLE IF EXISTS `student_ability_score_history`;
CREATE TABLE `student_ability_score_history`  (
  `id` bigint NOT NULL COMMENT '历史分数ID（雪花算法）',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `ability_id` bigint NOT NULL COMMENT '关联学生能力维度ID',
  `score_id` bigint NOT NULL COMMENT '关联原分数表ID',
  `profile_history_id` bigint NULL DEFAULT NULL COMMENT '关联对应的画像历史版本ID',
  `version` int NOT NULL COMMENT '分数版本号（与画像版本号保持一致）',
  `education_score` tinyint NULL DEFAULT 0 COMMENT '1.学历背景评分',
  `internship_score` tinyint NULL DEFAULT 0 COMMENT '2.实习经历评分',
  `professional_score` tinyint NULL DEFAULT 0 COMMENT '3.专业技能评分',
  `certificate_score` tinyint NULL DEFAULT 0 COMMENT '4.证书资质评分',
  `innovation_score` tinyint NULL DEFAULT 0 COMMENT '5.创新能力评分',
  `learning_score` tinyint NULL DEFAULT 0 COMMENT '6.学习能力评分',
  `pressure_score` tinyint NULL DEFAULT 0 COMMENT '7.抗压能力评分',
  `communication_score` tinyint NULL DEFAULT 0 COMMENT '8.沟通能力评分',
  `problem_solving_score` tinyint NULL DEFAULT 0 COMMENT '9.问题解决评分',
  `teamwork_score` tinyint NULL DEFAULT 0 COMMENT '10.团队协作评分',
  `total_score` decimal(5, 1) NULL DEFAULT 0.0 COMMENT '能力总评分',
  `industry_rank` int NULL DEFAULT NULL COMMENT '行业排名百分比',
  `peer_rank` int NULL DEFAULT NULL COMMENT '同届学生排名百分比',
  `score_type` tinyint NOT NULL DEFAULT 1 COMMENT '评分类型：1-系统自动评分 2-导师评分 3-企业评分',
  `score_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '评分评语',
  `change_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分数变更原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分数记录时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_score_history_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_score_history_ability`(`ability_id` ASC) USING BTREE,
  INDEX `idx_score_history_version`(`user_id` ASC, `version` ASC) USING BTREE,
  INDEX `idx_score_history_profile`(`profile_history_id` ASC) USING BTREE,
  INDEX `fk_score_history_origin`(`score_id` ASC) USING BTREE,
  CONSTRAINT `fk_score_history_ability` FOREIGN KEY (`ability_id`) REFERENCES `student_ability` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_score_history_origin` FOREIGN KEY (`score_id`) REFERENCES `student_ability_score` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_score_history_profile` FOREIGN KEY (`profile_history_id`) REFERENCES `student_profile_history` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_score_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生能力分数历史表（保留分数变化轨迹）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for student_image
-- ----------------------------
DROP TABLE IF EXISTS `student_image`;
CREATE TABLE `student_image`  (
  `id` bigint NOT NULL COMMENT '主键ID（雪花算法）',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片存储路径',
  `image_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'other' COMMENT '图片类型',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_is_deleted`(`is_deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生图片表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for student_profile
-- ----------------------------
DROP TABLE IF EXISTS `student_profile`;
CREATE TABLE `student_profile`  (
  `id` bigint NOT NULL COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `version` int NOT NULL DEFAULT 1 COMMENT '当前版本号',
  `user_name` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户姓名（加密）',
  `gender` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号（加密）',
  `email` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱（加密）',
  `college` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '学院（加密）',
  `major` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专业（加密）',
  `grade` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `profile_status` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `age` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `graduation_date` datetime NULL DEFAULT NULL COMMENT '毕业日期',
  `career_intentions` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职业意向',
  `job_intention_detail` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职位意向详情',
  `target_city` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '目标城市',
  `expected_salary` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '期望薪资范围',
  `industry_preference` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '行业偏好',
  `work_type_preference` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '工作类型偏好',
  `max_learning_cycle` int NULL DEFAULT NULL COMMENT '可接受的最长学习周期（月）',
  `education` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '学历',
  `work_experience` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '工作经历',
  `project_experience` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '项目经验',
  `skill` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '技能（加密）',
  `certificate` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '证书（加密）',
  `student_group` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `privacy_level` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_stu_profile_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_stu_profile_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_stu_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生核心画像表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for student_profile_history
-- ----------------------------
DROP TABLE IF EXISTS `student_profile_history`;
CREATE TABLE `student_profile_history`  (
  `id` bigint NOT NULL COMMENT '历史记录ID',
  `profile_id` bigint NOT NULL COMMENT '关联学生画像ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `version` int NOT NULL COMMENT '版本号',
  `profile_data` json NOT NULL COMMENT '画像数据快照（JSON格式）',
  `change_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_history_profile`(`profile_id` ASC) USING BTREE,
  INDEX `idx_history_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_history_version`(`profile_id` ASC, `version` ASC) USING BTREE,
  CONSTRAINT `fk_history_profile` FOREIGN KEY (`profile_id`) REFERENCES `student_profile` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生画像历史版本表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL COMMENT '用户唯一主键ID（非自增，用雪花ID）',
  `user_account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号（学号/自定义，唯一）',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `user_password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（加密存储，如SHA256+盐值）',
  `user_role` tinyint NOT NULL DEFAULT 1 COMMENT '用户角色：1-学生（默认），2-管理员，3-企业端，4-导师',
  `user_status` tinyint NOT NULL DEFAULT 1 COMMENT '账号状态：1-正常，2-未激活，3-冻结，4-注销',
  `register_type` tinyint NOT NULL DEFAULT 1 COMMENT '注册方式：1-手机号 2-邮箱 3-微信 4-QQ 5-学号',
  `phone` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号（允许为空）',
  `email` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱（允许为空）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_account`(`user_account` ASC) USING BTREE COMMENT '账号唯一索引'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户核心信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for user_resource
-- ----------------------------
DROP TABLE IF EXISTS `user_resource`;
CREATE TABLE `user_resource`  (
  `id` bigint NOT NULL COMMENT '资源ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `resource_type` tinyint NOT NULL COMMENT '资源类型：1-证书，2-奖状，3-成绩单，4-作品集，5-其他',
  `resource_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '资源名称',
  `resource_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '资源描述',
  `file_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件存储路径（核心字段）',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始文件名',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件类型（MIME类型）',
  `file_extension` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件扩展名',
  `issue_date` date NULL DEFAULT NULL COMMENT '证书/奖状颁发日期',
  `expire_date` date NULL DEFAULT NULL COMMENT '证书过期日期（如有）',
  `issuer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '颁发机构',
  `is_verified` tinyint NOT NULL DEFAULT 0 COMMENT '是否已验证：0-未验证，1-已验证',
  `verify_time` datetime NULL DEFAULT NULL COMMENT '验证时间',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_resource_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_resource_type`(`resource_type` ASC) USING BTREE,
  CONSTRAINT `fk_resource_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户资源表（存储证书、奖状、图片路径）' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
