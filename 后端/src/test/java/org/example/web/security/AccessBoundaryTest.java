package org.example.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import com.xingzhi.XingZhiApplication;
import org.example.web.tool.JwtUtil;
import org.example.web.tool.SnowIdCreater;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=XingZhiApplication.class, properties="training.worker.enabled=false")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="TRAINING_DB_TEST", matches="true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessBoundaryTest {
    @Autowired MockMvc http;
    @Autowired JdbcTemplate db;
    @Autowired DataSource ds;
    @Autowired ReportJobRegistry jobs;
    @Autowired ObjectMapper json;
    @Autowired org.example.web.tool.RSA_256 rsa;
    long owner, other, job, profile, ability, score, conversation, report, plan, task;
    String a, b;

    @BeforeAll void schema() throws Exception {
        var matcher=Pattern.compile("CREATE TABLE(?: IF NOT EXISTS)?[^;]+;",Pattern.CASE_INSENSITIVE)
            .matcher(Files.readString(Path.of("..","数据库","数据库结构.sql")));
        try(var connection=ds.getConnection();var stmt=connection.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS=0");
            try { while(matcher.find()) stmt.execute(matcher.group().replaceFirst("(?i)CREATE TABLE(?: IF NOT EXISTS)?", "CREATE TABLE IF NOT EXISTS")); }
            finally { stmt.execute("SET FOREIGN_KEY_CHECKS=1"); }
        }
    }
    long id() { return SnowIdCreater.generateId(20); }
    String token(long user) { return JwtUtil.genToken(Map.of("id", user, "userRole", 2)); }
    @BeforeEach void fixture() {
        owner=id(); other=id(); profile=id(); ability=id(); score=id(); conversation=id(); report=id(); plan=id(); task=id(); job=id();
        for(long user:new long[]{owner,other})
            db.update("INSERT INTO user(id,user_account,user_password,user_role,user_status,is_deleted) VALUES(?,?,?,1,1,0)", user,"__security_it_"+user,"unused");
        db.update("INSERT INTO student_profile(id,user_id,user_name) VALUES(?,?,?)",profile,owner,rsa.rsaEncrypt("权限测试"));
        db.update("INSERT INTO student_ability(id,user_id,profile_id) VALUES(?,?,?)",ability,owner,profile);
        db.update("INSERT INTO student_ability_score(id,user_id,ability_id) VALUES(?,?,?)",score,owner,ability);
        db.update("INSERT INTO ai_conversation(id,user_id,conversation_type,title,status) VALUES(?,?,1,'security fixture',1)",conversation,owner);
        db.update("INSERT INTO career_report(id,user_id,report_name,report_type,version,status,report_content) VALUES(?,?,'security fixture',1,1,2,'{}')",report,owner);
        db.update("INSERT INTO grow_plan(id,user_id,plan_name,plan_type,target_job,plan_content) VALUES(?,?,'security fixture',2,'fixture','fixture')",plan,owner);
        db.update("INSERT INTO grow_task(id,plan_id,task_name,task_type,status) VALUES(?,?,'security fixture',1,0)",task,plan);
        db.update("INSERT INTO job_info(id,job_name) VALUES(?,'__security_it_job')",job);
        a=token(owner); b=token(other);
    }
    @AfterEach void cleanup() {
        // Only exact IDs created above; no existing user or job is modified.
        db.update("DELETE FROM job_info WHERE id=? AND job_name='__security_it_job'",job);
        for(long user:new long[]{owner,other}) {
            db.update("DELETE FROM career_report_job WHERE user_id=?",user);
            db.update("DELETE FROM grow_task WHERE plan_id IN (SELECT id FROM grow_plan WHERE user_id=?)",user);
            for(String table:new String[]{"grow_plan","career_report","ai_conversation","student_ability_score","student_ability","student_profile"})
                db.update("DELETE FROM "+table+" WHERE user_id=?",user);
            db.update("DELETE FROM user WHERE id=? AND user_account=?",user,"__security_it_"+user);
        }
    }
    @Test void anonymousCanReadKnowledgeButCannotWriteOrDeleteUsers() throws Exception {
        http.perform(get("/job-info/page")).andExpect(status().isOk());
        http.perform(put("/job-info/"+job).contentType("application/json").content("{}" )).andExpect(status().isUnauthorized());
        http.perform(delete("/job-info/"+job)).andExpect(status().isUnauthorized());
        http.perform(post("/user/deleteById").param("start","1").param("end","1")).andExpect(status().isUnauthorized());
    }
    @Test void liveRoleOverridesSignedRoleAndAdminCanMaintainOwnFixtureJob() throws Exception {
        // Token deliberately claims admin, but database role is student.
        http.perform(delete("/job-info/"+job).header("Authorization",a)).andExpect(status().isForbidden());
        http.perform(post("/new/get_info").header("Authorization",a)).andExpect(status().isForbidden());
        db.update("UPDATE user SET user_role=2 WHERE id=?",owner);
        http.perform(put("/job-info/"+job).header("Authorization",a).contentType("application/json").content("{\"address\":\"fixture\"}")).andExpect(status().isOk());
        assertEquals("fixture",db.queryForObject("SELECT address FROM job_info WHERE id=?",String.class,job));
    }
    @Test void disabledAndDeletedUsersCannotReuseValidTokens() throws Exception {
        db.update("UPDATE user SET user_status=0 WHERE id=?",other);
        http.perform(get("/training/sessions").header("Authorization",b)).andExpect(status().isUnauthorized());
        db.update("UPDATE user SET user_status=1,is_deleted=1 WHERE id=?",other);
        http.perform(get("/student/"+profile).header("Authorization",b)).andExpect(status().isUnauthorized());
    }
    @Test void profilesRequireOwnerForReadWriteAndWholeBatch() throws Exception {
        http.perform(get("/student/"+profile).header("Authorization",a)).andExpect(status().isOk());
        http.perform(get("/student/"+profile).header("Authorization",b)).andExpect(status().isNotFound());
        http.perform(put("/student/update").header("Authorization",b).contentType("application/json").content("{\"id\":\""+profile+"\",\"targetCity\":\"other\"}")).andExpect(status().isNotFound());
        http.perform(post("/student/condition").header("Authorization",b).contentType("application/json").content("{\"userId\":\""+owner+"\"}")).andExpect(status().isForbidden());
        http.perform(post("/student/batchdelete").header("Authorization",b).contentType("application/json").content("[\""+profile+"\"]")).andExpect(status().isNotFound());
        http.perform(post("/student/batchdelete").header("Authorization",a).contentType("application/json").content("[\""+profile+"\",\"1\"]")).andExpect(status().isNotFound());
        assertEquals(0,db.queryForObject("SELECT is_deleted FROM student_profile WHERE id=?",Integer.class,profile));
    }
    @Test void unfilteredListsAndForeignRelationsAreBlocked() throws Exception {
        for(String path:new String[]{"/student/all","/ability/all","/ability/score/all"})
            http.perform(get(path).header("Authorization",b)).andExpect(status().isForbidden());
        http.perform(post("/ability/insert").header("Authorization",b).contentType("application/json").content("{\"profileId\":\""+profile+"\"}")).andExpect(status().isNotFound());
        http.perform(post("/ability/score/insert").header("Authorization",b).contentType("application/json").content("{\"abilityId\":\""+ability+"\"}")).andExpect(status().isNotFound());
        http.perform(post("/ability/score/batchselect").header("Authorization",b).contentType("application/json").content("[\""+score+"\"]")).andExpect(status().isNotFound());
    }
    @Test void chatHistoryAndInjectedUserAreBlockedBeforePlatformCall() throws Exception {
        http.perform(get("/ai-conversation/history/"+conversation).header("Authorization",b)).andExpect(status().isNotFound());
        http.perform(post("/ai-conversation/create").header("Authorization",b).contentType("application/json").content("{\"user_id\":\""+owner+"\"}")).andExpect(status().isForbidden());
        http.perform(post("/ai-conversation/send-stream").header("Authorization",b).contentType("application/json").content("{\"conversation_id\":\""+conversation+"\",\"content\":\"x\"}")).andExpect(status().isNotFound());
    }
    @Test void growthPatchChecksPlanOwner() throws Exception {
        http.perform(get("/grow/plans").param("userId",Long.toString(owner)).header("Authorization",b)).andExpect(status().isForbidden());
        http.perform(patch("/grow/tasks/"+task).header("Authorization",b).contentType("application/json").content("{\"status\":2}")).andExpect(status().isNotFound());
        assertEquals(0,db.queryForObject("SELECT status FROM grow_task WHERE id=?",Integer.class,task));
    }
    @Test void invalidQuizAnswersDoNotReplaceExistingScores() throws Exception {
        var bank = json.readTree(new org.springframework.core.io.ClassPathResource("data/ability-questions.json").getInputStream());
        var perDimension = new java.util.LinkedHashMap<String,Map<String,Object>>();
        for (var question : bank.path("questions"))
            perDimension.putIfAbsent(question.path("dim").asText(), Map.of("id", question.path("id").asText(), "k", 0));
        var valid = new java.util.ArrayList<>(perDimension.values());
        var duplicate = new java.util.ArrayList<>(valid); duplicate.set(1, duplicate.get(0));
        var invalidOption = new java.util.ArrayList<>(valid); invalidOption.set(0, Map.of("id",valid.get(0).get("id"), "k", 999));
        var missingDimension = new java.util.ArrayList<>(valid); missingDimension.remove(0);
        for (var answers : java.util.List.of(java.util.List.of(), duplicate, invalidOption, missingDimension))
            http.perform(post("/ability/quiz/submit").header("Authorization",a).contentType("application/json")
                .content(json.writeValueAsString(Map.of("answers",answers)))).andExpect(status().isBadRequest());
        assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM student_ability_score WHERE user_id=? AND is_deleted=0",Integer.class,owner));
        assertEquals(0, db.queryForObject("SELECT is_deleted FROM student_ability_score WHERE id=?",Integer.class,score));
    }
    @Test void reportDownloadsAndJobsAreOwnerBound() throws Exception {
        jobs.register("security_"+owner,owner);
        for(String path:new String[]{"/career-report/"+report,"/career-report/"+report+"/export/pdf","/career-report/jobs/security_"+owner})
            http.perform(get(path).header("Authorization",b)).andExpect(status().isNotFound());
        http.perform(post("/career-report/jobs/security_"+owner+"/cancel").header("Authorization",b)).andExpect(status().isNotFound());
        http.perform(get("/resume/export/"+owner).header("Authorization",b)).andExpect(status().isForbidden());
    }
}
