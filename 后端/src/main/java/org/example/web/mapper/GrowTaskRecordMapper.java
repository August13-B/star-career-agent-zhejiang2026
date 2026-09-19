package org.example.web.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.example.web.entity.GrowTaskRecord;

/**
 * 成长任务完成情况记录 Mapper（时间线，多条）
 */
@Mapper
public interface GrowTaskRecordMapper extends BaseMapper<GrowTaskRecord> {
}
