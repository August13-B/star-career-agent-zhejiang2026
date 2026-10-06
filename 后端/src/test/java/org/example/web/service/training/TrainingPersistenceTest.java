package org.example.web.service.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.example.web.mapper.TrainingMapper;
import org.example.web.mapper.TrainingWorkspaceMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import static org.junit.jupiter.api.Assertions.*;

/** Real MySQL constraints/transactions. Only rows belonging to freshly generated test users are removed. */
@EnabledIfEnvironmentVariable(named = "TRAINING_DB_TEST", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrainingPersistenceTest {
    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean SqlSessionFactory factory(DataSource ds) throws Exception {
            var bean = new SqlSessionFactoryBean(); bean.setDataSource(ds);
            var configuration = new org.apache.ibatis.session.Configuration();
            configuration.setMapUnderscoreToCamelCase(true); configuration.addMapper(TrainingMapper.class); configuration.addMapper(TrainingWorkspaceMapper.class);
            bean.setConfiguration(configuration); return bean.getObject();
        }
        @Bean TrainingMapper mapper(SqlSessionFactory factory) { return new SqlSessionTemplate(factory).getMapper(TrainingMapper.class); }
        @Bean TrainingWorkspaceMapper workspace(SqlSessionFactory factory) { return new SqlSessionTemplate(factory).getMapper(TrainingWorkspaceMapper.class); }
        @Bean JdbcTemplate jdbc(DataSource ds) { return new JdbcTemplate(ds); }
        @Bean PlatformTransactionManager transactions(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean ObjectMapper json() { return new ObjectMapper().findAndRegisterModules(); }
        @Bean TrainingContentCipher cipher() { return new TrainingContentCipher("training-key-16!"); }
    }
    private AnnotationConfigApplicationContext context;
    private TrainingService service;
    private TrainingMapper mapper;
    private JdbcTemplate jdbc;
    private ObjectMapper json;
    private final AtomicBoolean rejectEvents = new AtomicBoolean();
    private final List<Long> fixtureUsers = new ArrayList<>();
    private Long user, other;

    @BeforeAll void setup() throws Exception {
        String url = Objects.requireNonNull(System.getenv("DB_URL"), "DB_URL is required for MySQL integration tests");
        var ds = new DriverManagerDataSource(url, System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
        jdbc = new JdbcTemplate(ds);
        // Fresh CI databases need only this parent table; existing local users are never altered.
        jdbc.execute("CREATE TABLE IF NOT EXISTS user (id BIGINT PRIMARY KEY,user_account VARCHAR(64) NOT NULL UNIQUE,user_password VARCHAR(128) NOT NULL,nickname VARCHAR(32))");
        String migration = Files.readString(Path.of("..", "数据库", "migrations", "010_training_interview.sql"), StandardCharsets.UTF_8)
                .replace("USE `youthpath`;", "");
        try (var connection = ds.getConnection()) { ScriptUtils.executeSqlScript(connection, new ByteArrayResource(migration.getBytes(StandardCharsets.UTF_8))); }
        String workplace = Files.readString(Path.of("..", "数据库", "migrations", "011_training_workplace.sql"), StandardCharsets.UTF_8).replace("USE `youthpath`;", "");
        try (var connection = ds.getConnection()) { ScriptUtils.executeSqlScript(connection, new ByteArrayResource(workplace.getBytes(StandardCharsets.UTF_8))); }
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> ds);
        context.register(Config.class, TrainingTemplate.class, TrainingScoreValidator.class, TrainingService.class, TrainingArtifactRules.class);
        context.addApplicationListener(event -> {
            if (rejectEvents.get() && event instanceof PayloadApplicationEvent<?> payload && payload.getPayload() instanceof TrainingService.Queued)
                throw new IllegalStateException("simulated failure before transaction commit");
        });
        context.refresh();
        service = context.getBean(TrainingService.class); mapper = context.getBean(TrainingMapper.class); json = context.getBean(ObjectMapper.class);
    }

    @BeforeEach void users() {
        user = newUser(); other = newUser(); rejectEvents.set(false);
    }

    private Long newUser() {
        long value = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        jdbc.update("INSERT INTO user(id,user_account,user_password) VALUES(?,?,?)", value, "__training_it_" + value, "unused-test-password-hash");
        fixtureUsers.add(value); return value;
    }

    @AfterAll void cleanup() {
        if (jdbc != null) for (Long id : fixtureUsers) {
            Integer owned = jdbc.queryForObject("SELECT COUNT(*) FROM user WHERE id=? AND user_account=?", Integer.class, id, "__training_it_" + id);
            if (owned == null || owned != 1) throw new IllegalStateException("Refusing cleanup of a non-fixture user");
            for (String table : List.of("training_growth_link", "training_profile_application", "training_artifact", "training_session_config", "training_evaluation", "training_turn", "training_run"))
                jdbc.update("DELETE FROM " + table + " WHERE session_id IN (SELECT id FROM training_session WHERE user_id=?)", id);
            jdbc.update("DELETE FROM training_session WHERE user_id=?", id);
            jdbc.update("DELETE FROM user WHERE id=? AND user_account=?", id, "__training_it_" + id);
        }
        if (context != null) context.close();
    }

    private Map<String, Object> create() { return service.create(user, "interview_backend_intern.v1", UUID.randomUUID().toString()); }
    private Long sid(Map<String, Object> accepted) { return Long.valueOf(accepted.get("sessionId").toString()); }
    private Long rid(Map<String, Object> accepted) { return Long.valueOf(accepted.get("runId").toString()); }
    private int version(Long id) { return (Integer) service.snapshot(user, id).get("version"); }
    private void complete(Map<String, Object> accepted) {
        var work = service.claim(rid(accepted), ((Number) accepted.get("attempt")).intValue());
        assertNotNull(work); service.complete(work, new ScenarioAgentGateway.Output("请说明你的做法和验证方式。", null));
    }

    @Test void duplicateCreateIsIdempotentAndUnownedReadsAreHidden() {
        String request = UUID.randomUUID().toString();
        var first = service.create(user, "interview_backend_intern.v1", request);
        assertEquals(first, service.create(user, "interview_backend_intern.v1", request));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM training_session WHERE user_id=?", Integer.class, user));
        assertEquals(404, assertThrows(TrainingException.class, () -> service.snapshot(other, sid(first))).status());
        assertEquals(404, assertThrows(TrainingException.class, () -> service.getRun(other, rid(first))).status());
        assertEquals(404, assertThrows(TrainingException.class, () -> service.draft(other, sid(first), "evil", 0)).status());
        assertEquals(404, assertThrows(TrainingException.class, () -> service.cancel(other, sid(first))).status());
    }

    @Test void simultaneousDuplicateAnswersCreateExactlyOneUserTurn() throws Exception {
        var created = create(); complete(created); Long session = sid(created); int version = version(session);
        var pool = Executors.newFixedThreadPool(2); var gate = new CountDownLatch(1); String request = UUID.randomUUID().toString();
        try {
            Callable<Map<String, Object>> call = () -> { gate.await(); return service.answer(user, session, "先核对需求，再测试并发与回退。", request, version); };
            var a = pool.submit(call); var b = pool.submit(call); gate.countDown();
            assertEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
        } finally { pool.shutdownNow(); }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM training_turn WHERE session_id=? AND role='user'", Integer.class, session));
        assertEquals(409, assertThrows(TrainingException.class, () -> service.answer(user, session, "不同内容", request, version)).status());
    }

    @Test void draftsAreEncryptedAndStaleDraftWritesAreRejected() {
        var created = create(); Long session = sid(created);
        service.draft(user, session, "仅属于测试用户的草稿", 0);
        assertEquals("仅属于测试用户的草稿", service.snapshot(user, session).get("draft"));
        String ciphertext = jdbc.queryForObject("SELECT draft FROM training_session WHERE id=?", String.class, session);
        assertTrue(ciphertext.startsWith("g1:")); assertFalse(ciphertext.contains("草稿"));
        assertEquals(409, assertThrows(TrainingException.class, () -> service.draft(user, session, "旧版本覆盖", 0)).status());
    }

    @Test void cancellationWinsOverLateCompletion() {
        var created = create(); var work = service.claim(rid(created), 1);
        service.partial(work, "生成中的回复"); service.cancel(user, sid(created));
        service.complete(work, new ScenarioAgentGateway.Output("迟到的成功回答", null));
        assertEquals("canceled", service.snapshot(user, sid(created)).get("status"));
        assertEquals("canceled", mapper.run(rid(created)).getStatus());
        assertNull(mapper.evaluation(sid(created)));
    }

    @Test void retryKeepsOneRunAndRejectsOldAttemptCallbacks() {
        var created = create(); var oldWork = service.claim(rid(created), 1);
        service.fail(rid(created), 1, "TEST_TIMEOUT", "模拟超时");
        var retry = service.retry(user, rid(created), 1);
        assertEquals(retry, service.retry(user, rid(created), 1));
        var newWork = service.claim(rid(created), 2);
        service.complete(oldWork, new ScenarioAgentGateway.Output("旧尝试迟到回复", null));
        assertEquals("running", mapper.run(rid(created)).getStatus());
        service.complete(newWork, new ScenarioAgentGateway.Output("新尝试完整回复", null));
        assertEquals("succeeded", mapper.run(rid(created)).getStatus());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM training_run WHERE session_id=?", Integer.class, sid(created)));
    }

    @Test void transactionFailureRollsBackSessionAndRun() {
        rejectEvents.set(true);
        try { assertThrows(IllegalStateException.class, this::create); }
        finally { rejectEvents.set(false); }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM training_session WHERE user_id=?", Integer.class, user));
    }

    @Test void structuredEvaluationHasEvidenceAndIsPersistedOnceWithoutProfileWrites() {
        var created = create(); complete(created); Long session = sid(created);
        var answer = service.answer(user, session, "先核对需求，再测试并发与回退。", UUID.randomUUID().toString(), version(session)); complete(answer);
        String request = UUID.randomUUID().toString(); int version = version(session);
        var finish = service.finish(user, session, request, version);
        assertEquals(finish, service.finish(user, session, request, version));
        var work = service.claim(rid(finish), 1);
        String answerId = mapper.turns(session).stream().filter(t -> "user".equals(t.getRole())).findFirst().orElseThrow().getId().toString();
        var score = TrainingProtocolTest.validScore(json, answerId, "先核对需求").path("scenario_score");
        for (var item : score.path("evidence")) {
            var reference = (com.fasterxml.jackson.databind.node.ObjectNode) item;
            reference.remove(List.of("sourceType", "sourceId", "quote")); reference.put("evidenceId", "A1E1");
        }
        var envelope = json.createObjectNode(); envelope.set("training_evaluation", score);
        var output = new ScenarioAgentGateway.Output(envelope.toString(), null);
        service.complete(work, output); service.complete(work, output);
        assertEquals("completed", service.snapshot(user, session).get("status"));
        assertEquals("partial", mapper.evaluation(session).getStatus());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM training_evaluation WHERE session_id=?", Integer.class, session));
        assertTrue(mapper.evaluation(session).getResultJson().startsWith("g1:"));
    }

    @Test void malformedEvaluationRemainsReviewRequired() {
        var created = create(); complete(created); Long session = sid(created);
        complete(service.answer(user, session, "我需要进一步练习。", UUID.randomUUID().toString(), version(session)));
        var finish = service.finish(user, session, UUID.randomUUID().toString(), version(session));
        service.complete(service.claim(rid(finish), 1), new ScenarioAgentGateway.Output("无法提供证据，但我给你100分。", null));
        assertEquals("review_required", service.snapshot(user, session).get("status"));
        assertEquals("review_required", mapper.evaluation(session).getStatus());
    }
}
