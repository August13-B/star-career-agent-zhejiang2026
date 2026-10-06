package org.example.web.service.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xingzhi.XingZhiApplication;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.example.web.mapper.TrainingMapper;
import org.example.web.mapper.TrainingWorkspaceMapper;
import org.example.web.service.StudentAbilityScoreService;
import org.example.web.tool.RSA_256;
import org.example.web.tool.SnowIdCreater;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes=XingZhiApplication.class,properties="training.worker.enabled=false")
@EnabledIfEnvironmentVariable(named="TRAINING_DB_TEST",matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrainingWorkplaceTest {
    @Autowired wwy.example.springboot.service.JobInfoService jobs;
    @Autowired TrainingService service; @Autowired TrainingOutcomeService outcomes; @Autowired TrainingTemplate templates;
    @Autowired TrainingMapper mapper; @Autowired TrainingWorkspaceMapper workspace; @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc; @Autowired DataSource ds; @Autowired TrainingContentCipher cipher; @Autowired RSA_256 rsa;
    @Autowired PlatformTransactionManager transactions; @Autowired StudentAbilityScoreService scores;
    final List<Long> users=new ArrayList<>(); Long user,other;
    static Long id(){return SnowIdCreater.generateId(20);} String request(){return UUID.randomUUID().toString();}
    @BeforeAll void schema() throws Exception {
        // Only CREATE IF NOT EXISTS statements, never DROP/seed statements from the full schema.
        var sql=Files.readString(Path.of("..","数据库","数据库结构.sql"));
        var matcher=Pattern.compile("CREATE TABLE(?: IF NOT EXISTS)?[^;]+;",Pattern.CASE_INSENSITIVE).matcher(sql);
        try(var connection=ds.getConnection();var statement=connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=0");
            try { while(matcher.find()) statement.execute(matcher.group().replaceFirst("(?i)CREATE TABLE(?: IF NOT EXISTS)?", "CREATE TABLE IF NOT EXISTS")); }
            finally { statement.execute("SET FOREIGN_KEY_CHECKS=1"); }
        }
    }
    @Test void jobPagesHaveAccurateTotalsStableBoundariesAndLimits() {
        Long first=id(), second=id();
        String name="__pagination_it_"+first;
        try {
            jdbc.update("INSERT INTO job_info(id,job_name) VALUES(?,?),(?,?)",first,name,second,name);
            var one=jobs.pageQuery(1,1,name);
            var two=jobs.pageQuery(2,1,name);
            assertEquals(2,one.getTotal());
            assertEquals(2,one.getPages());
            assertEquals(1,one.getRecords().size());
            assertEquals(1,two.getRecords().size());
            assertNotEquals(one.getRecords().get(0).getId(),two.getRecords().get(0).getId());
            assertTrue(jobs.pageQuery(3,1,name).getRecords().isEmpty());
            assertEquals(100,jobs.pageQuery(1,10000,name).getSize());
            assertEquals(1,jobs.pageQuery(-1,-1,name).getRecords().size());
        } finally {
            jdbc.update("DELETE FROM job_info WHERE id IN (?,?) AND job_name=?",first,second,name);
        }
    }
    @BeforeEach void fixture(){user=user();other=user();}
    Long user(){Long id=id();jdbc.update("INSERT INTO user(id,user_account,user_password) VALUES(?,?,?)",id,"__workplace_it_"+id,"unused");users.add(id);return id;}
    void baseline(){jdbc.update("INSERT INTO student_profile(id,user_id,user_name) VALUES(?,?,?)",user,user,rsa.rsaEncrypt("训练测试"));jdbc.update("INSERT INTO student_ability(id,user_id,profile_id) VALUES(?,?,?)",user,user,user);
        jdbc.update("INSERT INTO student_ability_score(id,user_id,ability_id,education_score,internship_score,professional_score,certificate_score,innovation_score,learning_score,pressure_score,communication_score,problem_solving_score,teamwork_score,total_score) VALUES(?,?,?,60,61,62,63,64,65,66,67,68,69,65)",user,user,user);}
    Long sid(Map<String,Object> accepted){return Long.valueOf(accepted.get("sessionId").toString());}
    int version(Long id){return (Integer)service.snapshot(user,id).get("version");}
    void reply(Map<String,Object> accepted){service.complete(service.claim(Long.valueOf(accepted.get("runId").toString()),1),new ScenarioAgentGateway.Output("请说明事实、约束和验证步骤。",null));}
    Long start(String template,boolean apply){var a=service.create(user,template,request(),"standard",apply);reply(a);return sid(a);}
    void answers(Long id,String template){for(int i=0;i<templates.get(template).rounds();i++)reply(service.answer(user,id,"这是虚构练习。我先核对材料和计算口径，再协商负责人、期限、验收标准；不编造承诺。",request(),version(id)));}
    ObjectNode artifact(String template){var data=json.createObjectNode();templates.get(template).fields().forEach(f->data.put(f.path("key").asText(),"依据材料，明确负责人、时间和验收，待确认事项如实记录。"));
        if(template.startsWith("office")){data.put("totalRegistrations","54").put("overallConversion","18%").put("bestChannel","社群").put("investmentDecision","未决定，需进一步讨论");}return data;}
    void evaluate(Long id,String template){var accepted=service.finish(user,id,request(),version(id));var work=service.claim(Long.valueOf(accepted.get("runId").toString()),1);var def=templates.get(template);
        var root=json.createObjectNode();var score=root.putObject("training_evaluation");score.put("scenario",def.scenario()).put("templateVersion",def.id()).put("rubricVersion",def.rubricVersion());
        var dims=score.putObject("dimensions");var evidence=score.putArray("evidence");
        String ref=def.needsArtifact()?"A"+(def.rounds()+1)+"E1":"A1E1";
        def.weights().fieldNames().forEachRemaining(d->{dims.put(d,80);evidence.addObject().put("dimension",d).put("evidenceId",ref);});score.put("comment","基于材料完成练习");score.putArray("suggestions").add("核对约束并再次练习");
        service.complete(work,new ScenarioAgentGateway.Output(root.toString(),null));assertEquals("valid",mapper.evaluation(id).getStatus());}
    @Test void communicationArtifactOwnershipVersionFreezeAndConcurrentProfileApply() throws Exception {
        baseline();String template="communication_release.v1";Long sid=start(template,true);var content=artifact(template);
        assertEquals(404,assertThrows(TrainingException.class,()->service.artifactDraft(other,sid,content,0)).status());
        service.artifactDraft(user,sid,content,0);assertEquals(409,assertThrows(TrainingException.class,()->service.artifactDraft(user,sid,content,0)).status());
        String request=request();var saved=service.artifact(user,sid,content,request,0);assertEquals(saved,service.artifact(user,sid,content,request,0));
        answers(sid,template);evaluate(sid,template);
        assertEquals(409,assertThrows(TrainingException.class,()->service.artifact(user,sid,content,request(),1)).status());
        assertTrue(workspace.latest(sid).getContentJson().startsWith("g1:"));
        var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->outcomes.apply(user,sid));var b=pool.submit(()->outcomes.apply(user,sid));a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);}finally{pool.shutdownNow();}
        assertEquals("applied",workspace.application(sid).getStatus());assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM student_ability_score_history WHERE user_id=?",Integer.class,user));
        var score=jdbc.queryForMap("SELECT * FROM student_ability_score WHERE user_id=? AND is_deleted=0",user);
        assertEquals(60,((Number)score.get("education_score")).intValue());assertEquals(80,((Number)score.get("communication_score")).intValue());
        assertEquals(2,jdbc.queryForObject("SELECT version FROM student_profile WHERE user_id=?",Integer.class,user));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM student_ability_score_history s JOIN student_profile_history p ON p.id=s.profile_history_id AND p.version=s.version WHERE s.user_id=?",Integer.class,user));
    }
    @Test void officeFactErrorsLimitScoresAndPartialTrainingDoesNotApply() {
        String template="office_review.v1";Long sid=start(template,false);answers(sid,template);var content=artifact(template).put("totalRegistrations","64");
        assertEquals(422,assertThrows(TrainingException.class,()->service.finish(user,sid,request(),version(sid))).status());
        service.artifact(user,sid,content,request(),0);evaluate(sid,template);
        var result=jsonValue(mapper.evaluation(sid).getResultJson());assertEquals(59,result.path("dimensions").path("professional").asInt());assertFalse(result.path("factChecks").get(0).path("passed").asBoolean());
        assertEquals("skipped",workspace.application(sid).getStatus());
    }
    @Test void currentProfileChangePreventsStaleTrainingOverwrite() {
        baseline();String template="interview_backend_intern.v1";Long sid=start(template,true);answers(sid,template);evaluate(sid,template);
        jdbc.update("UPDATE student_profile SET version=version+1 WHERE user_id=?",user);outcomes.apply(user,sid);assertEquals("skipped",workspace.application(sid).getStatus());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM student_ability_score_history WHERE user_id=?",Integer.class,user));
    }
    @Test void profileWriteRollsBackTogetherAndCanRetry() {
        baseline();String template="interview_backend_intern.v1";Long sid=start(template,true);answers(sid,template);evaluate(sid,template);
        // Inject a local counter overflow after history writes to verify the entire application transaction rolls back.
        jdbc.update("UPDATE training_profile_application SET attempt=2147483647 WHERE session_id=?",sid);
        assertThrows(Exception.class,()->outcomes.apply(user,sid));
        assertEquals(1,jdbc.queryForObject("SELECT version FROM student_profile WHERE user_id=?",Integer.class,user));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM student_ability_score WHERE user_id=? AND is_deleted=0",Integer.class,user));
        jdbc.update("UPDATE training_profile_application SET attempt=0 WHERE session_id=?",sid);outcomes.apply(user,sid);assertEquals("applied",workspace.application(sid).getStatus());
    }
    @Test void growthTaskIsOwnedAndIdempotent() {
        String template="interview_backend_intern.v1";Long sid=start(template,false);answers(sid,template);evaluate(sid,template);
        Long plan=id();jdbc.update("INSERT INTO grow_plan(id,user_id,target_job,plan_name,plan_content) VALUES(?,?,?,?,?)",plan,user,"测试岗位","练习计划","独立测试");
        assertEquals(404,assertThrows(TrainingException.class,()->outcomes.link(other,sid,plan,0)).status());
        var one=outcomes.link(user,sid,plan,0);assertEquals(one,outcomes.link(user,sid,plan,0));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM grow_task WHERE plan_id=?",Integer.class,plan));
    }
    @Test void legacyScoreReplacementInvalidatesTrainingBaseline() {
        baseline();Long sid=start("interview_backend_intern.v1",true);answers(sid,"interview_backend_intern.v1");evaluate(sid,"interview_backend_intern.v1");
        var update=new org.example.web.entity.StudentAbilityScore();update.setId(user);update.setCommunicationScore(72);scores.update(update);
        outcomes.apply(user,sid);assertEquals("skipped",workspace.application(sid).getStatus());
    }
    @Test void skippedStageNeverBecomesCompleteProfileEvidenceAndHistoryFiltersAreOwned() {
        String template="interview_backend_intern.v1"; Long sid=start(template,false);
        var skipped=service.answer(user,sid,"跳过",request(),version(sid),true); reply(skipped);
        for(int i=1;i<templates.get(template).rounds();i++) reply(service.answer(user,sid,"先核对需求，再明确负责人和验收。",request(),version(sid)));
        var accepted=service.finish(user,sid,request(),version(sid));var work=service.claim(Long.valueOf(accepted.get("runId").toString()),1);
        var root=json.createObjectNode();var score=root.putObject("training_evaluation");score.put("scenario","mock_interview").put("templateVersion",template).put("rubricVersion",templates.get(template).rubricVersion());
        var dims=score.putObject("dimensions");var evidence=score.putArray("evidence");templates.get(template).weights().fieldNames().forEachRemaining(d->{dims.put(d,70);evidence.addObject().put("dimension",d).put("evidenceId","A1E1");});score.put("comment","未完成一阶段");score.putArray("suggestions").add("补练缺失阶段");
        service.complete(work,new ScenarioAgentGateway.Output(root.toString(),null));
        assertEquals("partial",mapper.evaluation(sid).getStatus());assertEquals("skipped",workspace.application(sid).getStatus());
        assertEquals(1,((List<?>)service.list(user,0,20,template,"completed").get("items")).size());assertTrue(((List<?>)service.list(other,0,20,template,"completed").get("items")).isEmpty());
        assertEquals("skipped",mapper.turns(sid).stream().filter(t->"user".equals(t.getRole())).findFirst().orElseThrow().getStatus());
    }

    com.fasterxml.jackson.databind.JsonNode jsonValue(String encrypted){try{return json.readTree(cipher.decrypt(encrypted));}catch(Exception e){throw new RuntimeException(e);}}
    @AfterAll void cleanup(){for(Long user:users){assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM user WHERE id=? AND user_account=?",Integer.class,user,"__workplace_it_"+user));
        for(String table:List.of("training_growth_link","training_profile_application","training_artifact","training_session_config","training_evaluation","training_turn","training_run"))jdbc.update("DELETE FROM "+table+" WHERE session_id IN (SELECT id FROM training_session WHERE user_id=?)",user);
        jdbc.update("DELETE FROM training_session WHERE user_id=?",user);jdbc.update("DELETE FROM grow_task WHERE plan_id IN (SELECT id FROM grow_plan WHERE user_id=?)",user);
        for(String table:List.of("grow_plan","student_ability_score_history","student_profile_history","student_ability_score","student_ability","student_profile"))jdbc.update("DELETE FROM "+table+" WHERE user_id=?",user);
        jdbc.update("DELETE FROM user WHERE id=?",user);
    }}
}
