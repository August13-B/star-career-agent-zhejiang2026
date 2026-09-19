package org.example.web.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.example.web.entity.GrowPlan;

/**
 * 成长规划表 Mapper（MyBatis-Plus BaseMapper）
 *
 * <p>用于把职业报告第 6 段输出的结构化 1/3/5 年目标落库，并支撑后续计划跟踪/完成情况。
 */
@Mapper
public interface GrowPlanMapper extends BaseMapper<GrowPlan> {
}
