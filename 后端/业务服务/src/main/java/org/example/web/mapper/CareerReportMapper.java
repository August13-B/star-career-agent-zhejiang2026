package org.example.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.web.entity.CareerReport;

@Mapper
public interface CareerReportMapper {
    
    int insert(CareerReport careerReport);
    
    int update(CareerReport careerReport);
    
    int deleteById(@Param("id") Long id);
    
    CareerReport selectById(@Param("id") Long id);
    
    List<CareerReport> selectByUserId(@Param("userId") Long userId);
    
    CareerReport selectByMatchId(@Param("matchId") Long matchId);
    
    List<CareerReport> selectByReportType(@Param("reportType") Integer reportType);
    
    List<CareerReport> selectByUserIdAndType(@Param("userId") Long userId, @Param("reportType") Integer reportType);
    
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    int updateVersion(@Param("id") Long id, @Param("version") Integer version);
    
    int updateFeedback(@Param("id") Long id, @Param("feedback") String feedback, @Param("feedbackScore") Integer feedbackScore);
    
    int logicDeleteById(@Param("id") Long id);
}
