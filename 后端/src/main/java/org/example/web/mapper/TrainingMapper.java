package org.example.web.mapper;

import java.util.List;
import org.apache.ibatis.annotations.*;
import org.example.web.entity.training.TrainingData.*;

@Mapper
public interface TrainingMapper {
    @Select("SELECT id FROM user WHERE id=#{userId} FOR UPDATE")
    Long lockUser(Long userId);

    @Select("SELECT * FROM training_session WHERE id=#{id} AND user_id=#{userId} FOR UPDATE")
    Session lockSession(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM training_session WHERE id=#{id} AND user_id=#{userId}")
    Session session(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT * FROM training_session WHERE id=#{id} FOR UPDATE")
    Session lockWorkerSession(Long id);

    @Select("SELECT * FROM training_session WHERE user_id=#{userId} AND client_request_id=#{requestId}")
    Session createdRequest(@Param("userId") Long userId, @Param("requestId") String requestId);

    @Select("SELECT * FROM training_session WHERE user_id=#{userId} ORDER BY create_time DESC,id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Session> sessions(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM training_session WHERE user_id=#{userId} AND (#{templateId} IS NULL OR template_id=#{templateId}) AND (#{status} IS NULL OR status=#{status}) ORDER BY create_time DESC,id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Session> filteredSessions(@Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset, @Param("templateId") String templateId, @Param("status") String status);

    @Insert("""
        INSERT INTO training_session(id,user_id,template_id,status,answered_count,version,draft,draft_version,client_request_id)
        VALUES(#{id},#{userId},#{templateId},#{status},#{answeredCount},#{version},#{draft},#{draftVersion},#{clientRequestId})
        """)
    void insertSession(Session row);

    @Update("UPDATE training_session SET status=#{status},answered_count=#{answeredCount},version=version+1,update_time=CURRENT_TIMESTAMP(3) WHERE id=#{id}")
    void updateSession(Session row);

    @Update("UPDATE training_session SET draft=#{draft},draft_version=draft_version+1 WHERE id=#{id} AND draft_version=#{expectedVersion}")
    int saveDraft(@Param("id") Long id, @Param("draft") String draft, @Param("expectedVersion") int expectedVersion);

    @Select("SELECT * FROM training_turn WHERE session_id=#{id} ORDER BY ordinal")
    List<Turn> turns(Long id);

    @Select("SELECT COALESCE(MAX(ordinal),0)+1 FROM training_turn WHERE session_id=#{id}")
    int nextOrdinal(Long id);

    @Insert("INSERT INTO training_turn(id,session_id,run_id,role,ordinal,content,status) VALUES(#{id},#{sessionId},#{runId},#{role},#{ordinal},#{content},#{status})")
    void insertTurn(Turn row);

    @Update("UPDATE training_turn SET content=#{content},status=#{status} WHERE id=#{id}")
    void updateTurn(@Param("id") Long id, @Param("content") String content, @Param("status") String status);

    @Update("UPDATE training_turn SET status=#{status} WHERE id=#{id}")
    void turnStatus(@Param("id") Long id, @Param("status") String status);

    @Select("SELECT * FROM training_run WHERE id=#{id}")
    Run run(Long id);

    @Select("SELECT * FROM training_run WHERE id=#{id} FOR UPDATE")
    Run lockRun(Long id);

    @Select("SELECT * FROM training_run WHERE session_id=#{id} ORDER BY create_time DESC,id DESC LIMIT 1")
    Run latestRun(Long id);

    @Select("SELECT * FROM training_run WHERE session_id=#{id} AND client_request_id=#{requestId}")
    Run requestedRun(@Param("id") Long id, @Param("requestId") String requestId);

    @Select("SELECT COUNT(*) FROM training_run r JOIN training_session s ON s.id=r.session_id WHERE s.user_id=#{userId} AND r.status IN ('queued','running')")
    int busyUser(Long userId);

    @Insert("""
        INSERT INTO training_run(id,session_id,operation,status,attempt,client_request_id,input_hash,request_json,response_message_id)
        VALUES(#{id},#{sessionId},#{operation},#{status},#{attempt},#{clientRequestId},#{inputHash},#{requestJson},#{responseMessageId})
        """)
    void insertRun(Run row);

    @Update("""
        UPDATE training_run SET status=#{status},attempt=#{attempt},error_code=#{errorCode},error_message=#{errorMessage},raw_result=#{rawResult},update_time=CURRENT_TIMESTAMP(3)
        WHERE id=#{id}
        """)
    void updateRun(Run row);

    @Select("SELECT * FROM training_run WHERE status IN ('queued','running') ORDER BY create_time")
    List<Run> unfinishedRuns();

    @Select("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('training_session','training_turn','training_run','training_evaluation')")
    int tableCount();

    @Select("SELECT * FROM training_evaluation WHERE session_id=#{id}")
    Evaluation evaluation(Long id);

    @Insert("INSERT INTO training_evaluation(id,session_id,run_id,status,result_json,message) VALUES(#{id},#{sessionId},#{runId},#{status},#{resultJson},#{message})")
    void insertEvaluation(Evaluation row);
}
