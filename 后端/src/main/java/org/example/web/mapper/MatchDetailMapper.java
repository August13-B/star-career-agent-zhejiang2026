package org.example.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.web.entity.MatchDetail;

@Mapper
public interface MatchDetailMapper {
    
    int insert(MatchDetail matchDetail);
    
    int update(MatchDetail matchDetail);
    
    int deleteById(@Param("id") Long id);
    
    MatchDetail selectById(@Param("id") Long id);
    
    List<MatchDetail> selectByMatchId(@Param("matchId") Long matchId);
    
    int deleteByMatchId(@Param("matchId") Long matchId);
    
    List<MatchDetail> selectByMatchIdAndDimType(@Param("matchId") Long matchId, @Param("dimType") Integer dimType);
    
    MatchDetail selectByMatchIdAndDimCode(@Param("matchId") Long matchId, @Param("dimCode") String dimCode);
    
    int batchInsert(@Param("list") List<MatchDetail> matchDetailList);
}
