package org.example.web.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;
import org.example.web.entity.training.TrainingData.*;

@Mapper
public interface TrainingWorkspaceMapper {
    @Select("SELECT * FROM training_session_config WHERE session_id=#{id}") Config config(Long id);
    @Insert("""
        INSERT INTO training_session_config(session_id,template_snapshot,difficulty,use_for_profile,baseline_score_id,baseline_profile_version,artifact_draft)
        VALUES(#{sessionId},#{templateSnapshot},#{difficulty},#{useForProfile},#{baselineScoreId},#{baselineProfileVersion},#{artifactDraft})
        """) void insertConfig(Config value);
    @Update("UPDATE training_session_config SET artifact_draft=#{content},artifact_draft_version=artifact_draft_version+1 WHERE session_id=#{id} AND artifact_draft_version=#{version}")
    int draft(@Param("id") Long id,@Param("content") String content,@Param("version") int version);
    @Update("UPDATE training_session_config SET selected_artifact_id=#{artifact} WHERE session_id=#{id}")
    void freeze(@Param("id") Long id,@Param("artifact") Long artifact);
    @Select("SELECT * FROM training_artifact WHERE session_id=#{id} ORDER BY revision DESC LIMIT 1") Artifact latest(Long id);
    @Select("SELECT * FROM training_artifact WHERE session_id=#{id} AND revision=#{revision}")
    Artifact revision(@Param("id") Long id,@Param("revision") int revision);
    @Select("SELECT * FROM training_artifact WHERE session_id=#{id} AND client_request_id=#{request}")
    Artifact requested(@Param("id") Long id,@Param("request") String request);
    @Select("SELECT id,session_id,revision,create_time FROM training_artifact WHERE session_id=#{id} ORDER BY revision DESC") List<Artifact> revisions(Long id);
    @Insert("INSERT INTO training_artifact(id,session_id,revision,client_request_id,input_hash,content_json) VALUES(#{id},#{sessionId},#{revision},#{clientRequestId},#{inputHash},#{contentJson})")
    void insertArtifact(Artifact value);
    @Select("SELECT * FROM training_profile_application WHERE session_id=#{id}") Application application(Long id);
    @Insert("INSERT INTO training_profile_application(session_id,status,message) VALUES(#{id},#{status},#{message})")
    void applicationQueued(@Param("id") Long id,@Param("status") String status,@Param("message") String message);
    @Update("""
        UPDATE training_profile_application SET status=#{status},attempt=#{attempt},before_scores=#{beforeScores},after_scores=#{afterScores},
        profile_version=#{profileVersion},score_history_id=#{scoreHistoryId},message=#{message},update_time=CURRENT_TIMESTAMP(3) WHERE session_id=#{sessionId}
        """) void updateApplication(Application value);
    @Select("SELECT session_id FROM training_profile_application WHERE status='pending' ORDER BY update_time LIMIT 20") List<Long> pendingApplications();
    @Select("SELECT session_id AS sessionId,task_id AS taskId,plan_id AS planId,suggestion_index AS suggestionIndex FROM training_growth_link WHERE session_id=#{id}") Map<String,Object> growth(Long id);
    @Insert("INSERT INTO training_growth_link(session_id,task_id,plan_id,suggestion_index) VALUES(#{id},#{task},#{plan},#{suggestion})")
    void linkGrowth(@Param("id") Long id,@Param("task") Long task,@Param("plan") Long plan,@Param("suggestion") int suggestion);
    @Select("""
        SELECT e.* FROM training_evaluation e JOIN training_session s ON s.id=e.session_id
        LEFT JOIN training_session_config c ON c.session_id=s.id
        WHERE s.user_id=#{user} AND s.template_id=#{template} AND s.id<#{id} AND e.status='valid'
        AND COALESCE(c.difficulty,'standard')=#{difficulty} ORDER BY s.id DESC LIMIT 1
        """) Evaluation previous(@Param("user") Long user,@Param("template") String template,@Param("id") Long id,@Param("difficulty") String difficulty);
}
