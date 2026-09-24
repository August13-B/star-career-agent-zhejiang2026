package org.example.web.service.training;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.example.web.mapper.TrainingMapper;
import org.example.web.mapper.TrainingWorkspaceMapper;
import org.example.web.service.GrowPlanService;
import org.example.web.tool.RSA_256;
import org.example.web.tool.SnowIdCreater;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Short local transactions only: no model/network calls while holding profile locks. */
@Service
public class TrainingOutcomeService {
    private static final List<String> DIMENSIONS = List.of("education", "internship", "professional", "certificate", "innovation", "learning", "pressure", "communication", "problem_solving", "teamwork");
    private final TrainingMapper db;
    private final TrainingWorkspaceMapper workspace;
    private final JdbcTemplate jdbc;
    private final TrainingContentCipher cipher;
    private final ObjectMapper json;
    private final RSA_256 rsa;
    private final GrowPlanService growth;
    public TrainingOutcomeService(TrainingMapper db, TrainingWorkspaceMapper workspace, JdbcTemplate jdbc, TrainingContentCipher cipher, ObjectMapper json, RSA_256 rsa, GrowPlanService growth) {
        this.db=db; this.workspace=workspace; this.jdbc=jdbc; this.cipher=cipher; this.json=json; this.rsa=rsa; this.growth=growth;
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void apply(Long user, Long sessionId) {
        db.lockUser(user);
        var session=db.lockSession(sessionId,user);
        if(session==null) throw missing();
        var application=workspace.application(sessionId);
        if(application==null || !Set.of("pending","failed").contains(application.getStatus())) return;
        var config=workspace.config(sessionId); var evaluation=db.evaluation(sessionId);
        if(config==null || !config.getUseForProfile() || evaluation==null || !"valid".equals(evaluation.getStatus())) { skip(application,"训练尚未满足画像更新条件"); return; }
        if(config.getBaselineScoreId()==null) { skip(application,"训练开始时尚无完整能力基线；请先完善个人画像并完成能力测评，再开始新训练"); return; }
        var profiles=jdbc.queryForList("SELECT * FROM student_profile WHERE user_id=? AND is_deleted=0 FOR UPDATE",user);
        var abilities=jdbc.queryForList("SELECT * FROM student_ability WHERE user_id=? AND is_deleted=0 FOR UPDATE",user);
        // Lock the user's whole score range, including its insertion gap, before reading the baseline.
        var scores=jdbc.queryForList("SELECT * FROM student_ability_score WHERE user_id=? ORDER BY update_time DESC,id DESC FOR UPDATE",user);
        var active=scores.stream().filter(s->number(s.get("is_deleted"))==0 && number(s.get("score_type"))==1).toList();
        if(profiles.size()!=1 || abilities.size()!=1 || active.size()!=1) { skip(application,"当前画像或系统能力基线不唯一/不存在，请先完成能力测评"); return; }
        var profile=profiles.get(0); var ability=abilities.get(0); var old=active.get(0);
        if(!Objects.equals(numberLong(ability.get("profile_id")),numberLong(profile.get("id"))) || !Objects.equals(numberLong(old.get("ability_id")),numberLong(ability.get("id")))) { skip(application,"能力记录与个人画像关联不完整，请先完善画像"); return; }
        if(!Objects.equals(numberLong(old.get("id")),config.getBaselineScoreId()) || number(profile.get("version"))!=config.getBaselineProfileVersion()) { skip(application,"训练期间能力基线或画像已更新，本次保留练习反馈，避免覆盖较新数据"); return; }
        for(String dimension:DIMENSIONS) if(!(old.get(dimension+"_score") instanceof Number) || number(old.get(dimension+"_score"))<0 || number(old.get(dimension+"_score"))>100) { skip(application,"能力基线包含缺失或无效维度，请先重新测评"); return; }
        JsonNode result=read(cipher.decrypt(evaluation.getResultJson()));
        Map<String,Object> before=scoresView(old), after=new LinkedHashMap<>(before);
        result.path("dimensions").fields().forEachRemaining(e->{ if(!DIMENSIONS.contains(e.getKey())) throw new IllegalStateException("Unexpected training dimension"); after.put(e.getKey(),e.getValue().asInt()); });
        double hard=DIMENSIONS.subList(0,4).stream().mapToInt(d->number(after.get(d))).average().orElseThrow();
        double soft=DIMENSIONS.subList(4,10).stream().mapToInt(d->number(after.get(d))).average().orElseThrow();
        double total=Math.round((hard*.3+soft*.7)*10)/10.0; after.put("total",total);
        Integer historyMax=jdbc.queryForObject("SELECT COALESCE(MAX(version),0) FROM student_profile_history WHERE user_id=?",Integer.class,user);
        int version=Math.max(number(profile.get("version")),historyMax);
        // Keep both the actual baseline and the new observation, with matching profile/score history IDs.
        Integer baselineHistory=jdbc.queryForObject("SELECT COUNT(*) FROM student_ability_score_history WHERE user_id=? AND score_id=? AND version=?",Integer.class,user,old.get("id"),version);
        if(baselineHistory==0) {
            if(historyMax>0 && historyMax>=version) version++;
            history(user,profile,old,version,"职场训练前的能力基线");
        }
        version++;
        long scoreId=id(); String comment=rsa.rsaEncrypt("职场模拟训练更新，仅代表本次练习观察");
        List<Object> values=new ArrayList<>(List.of(scoreId,user,ability.get("id")));
        DIMENSIONS.forEach(d->values.add(after.get(d))); values.add(total); values.add(comment);
        jdbc.update("UPDATE student_ability_score SET is_deleted=1 WHERE id=?",old.get("id"));
        String columns=String.join(",",DIMENSIONS.stream().map(d->d+"_score").toList());
        jdbc.update("INSERT INTO student_ability_score(id,user_id,ability_id,"+columns+",total_score,score_comment,score_type) VALUES("+String.join(",",Collections.nCopies(values.size(),"?"))+",1)",values.toArray());
        jdbc.update("UPDATE student_profile SET version=?,update_time=CURRENT_TIMESTAMP WHERE id=?",version,profile.get("id"));
        profile.put("version",version);
        var saved=jdbc.queryForMap("SELECT * FROM student_ability_score WHERE id=?",scoreId);
        long historyId=history(user,profile,saved,version,"职场训练 "+sessionId+" · latest_observation_v1");
        application.setStatus("applied"); application.setAttempt(Math.addExact(application.getAttempt(),1)); application.setBeforeScores(cipher.encrypt(write(before))); application.setAfterScores(cipher.encrypt(write(after)));
        application.setProfileVersion(version); application.setScoreHistoryId(historyId); application.setMessage("已更新本次覆盖的能力维度，并保留更新前后历史；未覆盖维度保持原值"); workspace.updateApplication(application);
    }

    private long history(Long user, Map<String,Object> profile, Map<String,Object> score, int version, String reason) {
        long profileHistory=id(), scoreHistory=id(); var snapshot=new LinkedHashMap<>(profile); snapshot.put("version",version);
        jdbc.update("INSERT INTO student_profile_history(id,profile_id,user_id,version,profile_data,change_reason) VALUES(?,?,?,?,?,?)",profileHistory,profile.get("id"),user,version,write(snapshot),reason);
        String columns=String.join(",",DIMENSIONS.stream().map(d->d+"_score").toList());
        jdbc.update("INSERT INTO student_ability_score_history(id,user_id,ability_id,score_id,profile_history_id,version,"+columns+",total_score,industry_rank,peer_rank,score_type,score_comment,change_reason) SELECT ?,user_id,ability_id,id,?,?,"+columns+",total_score,industry_rank,peer_rank,score_type,score_comment,? FROM student_ability_score WHERE id=?",scoreHistory,profileHistory,version,reason,score.get("id"));
        return scoreHistory;
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> plans(Long user) {
        return jdbc.queryForList("SELECT id,plan_name AS name FROM grow_plan WHERE user_id=? AND is_deleted=0 ORDER BY update_time DESC",user).stream().map(p->Map.<String,Object>of("id",p.get("id").toString(),"name",p.get("name"))).toList();
    }

    @Transactional
    public Map<String,Object> link(Long user, Long sessionId, Long planId, int suggestion) {
        db.lockUser(user); if(db.lockSession(sessionId,user)==null) throw missing();
        var previous=workspace.growth(sessionId);
        if(previous!=null) return Map.of("taskId",previous.get("taskId").toString(),"planId",previous.get("planId").toString());
        var evaluation=db.evaluation(sessionId);
        if(evaluation==null || !Set.of("valid","partial").contains(evaluation.getStatus())) throw new TrainingException(409,"FEEDBACK_REQUIRED","请先完成有效的训练反馈");
        var suggestions=read(cipher.decrypt(evaluation.getResultJson())).path("suggestions");
        if(suggestion<0 || suggestion>=suggestions.size()) throw new TrainingException(422,"SUGGESTION_INVALID","请选择有效的练习建议");
        if(jdbc.queryForList("SELECT id FROM grow_plan WHERE id=? AND user_id=? AND is_deleted=0 FOR UPDATE",planId,user).isEmpty()) throw missing();
        String text=suggestions.get(suggestion).asText();
        var task=growth.addTask(user,Map.of("planId",planId,"taskName","职场训练后的专项练习","taskDesc",text+"\n来源：职场训练 "+sessionId,"expectedOutcome","完成建议中的练习并在成长任务中记录结果；任务完成不会直接提升能力分数","targetAbility","职场训练"));
        Long taskId=Long.valueOf(task.get("taskId").toString()); workspace.linkGrowth(sessionId,taskId,planId,suggestion);
        return Map.of("taskId",taskId.toString(),"planId",planId.toString());
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void failed(Long sessionId) {
        Long user=owner(sessionId); db.lockUser(user); db.lockSession(sessionId,user);
        var application=workspace.application(sessionId);
        if(application!=null && Set.of("pending","failed").contains(application.getStatus())) {
            application.setStatus("failed"); application.setMessage("画像更新暂未成功；训练反馈已保存，可重试更新"); workspace.updateApplication(application);
        }
    }
    public List<Long> pending() { return workspace.pendingApplications(); }
    public Long owner(Long id) { return jdbc.queryForObject("SELECT user_id FROM training_session WHERE id=?",Long.class,id); }
    private void skip(org.example.web.entity.training.TrainingData.Application application,String message){ application.setStatus("skipped"); application.setMessage(message); workspace.updateApplication(application); }
    private Map<String,Object> scoresView(Map<String,Object> score){Map<String,Object> view=new LinkedHashMap<>(); DIMENSIONS.forEach(d->view.put(d,score.get(d+"_score"))); view.put("total",score.get("total_score"));return view;}
    private int number(Object value){return value instanceof Number n?n.intValue():-1;}
    private Long numberLong(Object value){return value instanceof Number n?n.longValue():null;}
    private static long id(){return SnowIdCreater.generateId(20);}
    private TrainingException missing(){return new TrainingException(404,"TRAINING_NOT_FOUND","记录不存在或无权访问");}
    private JsonNode read(String value){try{return json.readTree(value);}catch(Exception e){throw new IllegalStateException("训练结果读取失败");}}
    private String write(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("画像历史序列化失败");}}
}
