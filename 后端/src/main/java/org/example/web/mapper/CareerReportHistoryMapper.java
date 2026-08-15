package org.example.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.web.entity.CareerReportHistory;

@Mapper
public interface CareerReportHistoryMapper {
    
    int insert(CareerReportHistory careerReportHistory);
    
    int update(CareerReportHistory careerReportHistory);
    
    int deleteById(@Param("id") Long id);
    
    CareerReportHistory selectById(@Param("id") Long id);
    
    List<CareerReportHistory> selectByReportId(@Param("reportId") Long reportId);
    
    CareerReportHistory selectByReportIdAndVersion(@Param("reportId") Long reportId, @Param("version") Integer version);
    
    int deleteByReportId(@Param("reportId") Long reportId);
    
    int logicDeleteById(@Param("id") Long id);
}
