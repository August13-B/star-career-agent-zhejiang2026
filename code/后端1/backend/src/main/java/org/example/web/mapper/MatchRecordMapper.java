package org.example.web.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.web.entity.MatchRecord;

/**
 * 人岗匹配记录Mapper接口
 * 提供对match_record表的数据库操作
 * 包括插入、更新、删除、查询等基本CRUD操作
 * 
 * @author 系统生成
 * @version 1.0
 */
@Mapper
public interface MatchRecordMapper {
    
    /**
     * 插入匹配记录
     * 
     * @param matchRecord 匹配记录实体对象
     * @return 受影响的行数
     */
    int insert(MatchRecord matchRecord);
    
    /**
     * 更新匹配记录
     * 
     * @param matchRecord 匹配记录实体对象
     * @return 受影响的行数
     */
    int update(MatchRecord matchRecord);
    
    /**
     * 根据ID物理删除匹配记录
     * 
     * @param id 匹配记录ID
     * @return 受影响的行数
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据ID查询匹配记录
     * 
     * @param id 匹配记录ID
     * @return 匹配记录实体对象
     */
    MatchRecord selectById(@Param("id") Long id);
    
    /**
     * 根据用户ID查询匹配记录列表
     * 
     * @param userId 用户ID
     * @return 匹配记录列表
     */
    List<MatchRecord> selectByUserId(@Param("userId") Long userId);
    
    /**
     * 根据岗位ID查询匹配记录列表
     * 
     * @param jobId 岗位ID
     * @return 匹配记录列表
     */
    List<MatchRecord> selectByJobId(@Param("jobId") Long jobId);
    
    /**
     * 根据用户ID和岗位ID查询匹配记录
     * 查询特定用户和岗位的匹配记录
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @return 匹配记录实体对象
     */
    MatchRecord selectByUserIdAndJobId(@Param("userId") Long userId, @Param("jobId") Long jobId);
    
    /**
     * 查询所有匹配记录（未删除的）
     * 
     * @return 匹配记录列表
     */
    List<MatchRecord> selectAll();
    
    /**
     * 更新匹配状态
     * 
     * @param id 匹配记录ID
     * @param matchStatus 匹配状态（0-未生成，1-生成中，2-已完成，3-失败）
     * @return 受影响的行数
     */
    int updateMatchStatus(@Param("id") Long id, @Param("matchStatus") Integer matchStatus);
    
    /**
     * 更新匹配结果
     * 
     * @param id 匹配记录ID
     * @param matchResult 匹配结果（1-强烈推荐，2-推荐，3-一般，4-不推荐）
     * @return 受影响的行数
     */
    int updateMatchResult(@Param("id") Long id, @Param("matchResult") Integer matchResult);
    
    /**
     * 更新匹配分数
     * 更新匹配记录的各项分数和总分
     * 
     * @param matchRecord 包含新分数的匹配记录对象
     * @return 受影响的行数
     */
    int updateScores(MatchRecord matchRecord);
    
    /**
     * 根据匹配结果查询匹配记录列表
     * 
     * @param matchResult 匹配结果代码
     * @return 匹配记录列表
     */
    List<MatchRecord> selectByMatchResult(@Param("matchResult") Integer matchResult);
    
    /**
     * 根据匹配状态查询匹配记录列表
     * 
     * @param matchStatus 匹配状态代码
     * @return 匹配记录列表
     */
    List<MatchRecord> selectByMatchStatus(@Param("matchStatus") Integer matchStatus);
    
    /**
     * 逻辑删除匹配记录
     * 将is_deleted字段置为1，实现软删除
     * 
     * @param id 匹配记录ID
     * @return 受影响的行数
     */
    int logicDeleteById(@Param("id") Long id);
}
