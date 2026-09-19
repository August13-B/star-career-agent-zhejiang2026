package org.example.web.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.example.web.entity.GrowTask;

/**
 * 成长任务表 Mapper（MyBatis-Plus BaseMapper）
 *
 * <p>每个 keyAction 一条；`status` / `progress` / `completion_detail` 标记完成情况。
 */
@Mapper
public interface GrowTaskMapper extends BaseMapper<GrowTask> {
}
