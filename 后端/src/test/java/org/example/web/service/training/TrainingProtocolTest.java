package org.example.web.service.training;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrainingProtocolTest {
    private final ObjectMapper json = new ObjectMapper();
    private TrainingScoreValidator validator;
    @BeforeEach void setup() throws Exception { validator = new TrainingScoreValidator(new TrainingTemplate(json), json); }

    static ObjectNode validScore(ObjectMapper json, String answerId, String quote) {
        ObjectNode root = json.createObjectNode();
        ObjectNode score = root.putObject("scenario_score");
        score.put("scenario", "mock_interview").put("templateVersion", "interview_backend_intern.v1").put("rubricVersion", "interview_rubric.v1");
        score.putObject("dimensions").put("professional", 80).put("communication", 70).put("problem_solving", 90).put("pressure", 60);
        score.put("total", 100).put("comment", "技术方案较具体，需继续改进沟通。");
        score.putArray("suggestions").add("下次用具体的回退条件说明风险判断。");
        var evidence = score.putArray("evidence");
        for (String dimension : new String[]{"professional", "communication", "problem_solving", "pressure"})
            evidence.addObject().put("dimension", dimension).put("sourceType", "turn").put("sourceId", answerId).put("quote", quote);
        return root;
    }

    @Test void recomputesWeightedTotalInsteadOfTrustingModel() {
        var result = validator.validate(validScore(json, "9007199254740993", "先核对需求"), Map.of("9007199254740993", "我会先核对需求，然后验证并发和回退条件。"));
        assertEquals(77, result.path("total").asInt());
    }

    @Test void rejectsWrongVersionsUnknownDimensionsAndOutOfRangeScores() {
        for (String mutation : new String[]{"version", "extra", "range", "string", "missing"}) {
            var root = validScore(json, "1", "验证");
            var score = (ObjectNode) root.path("scenario_score");
            var dimensions = (ObjectNode) score.path("dimensions");
            switch (mutation) {
                case "version" -> score.put("templateVersion", "unknown");
                case "extra" -> dimensions.put("education", 100);
                case "range" -> dimensions.put("pressure", 101);
                case "string" -> dimensions.put("pressure", "80");
                case "missing" -> dimensions.remove("pressure");
            }
            assertThrows(TrainingException.class, () -> validator.validate(root, Map.of("1", "验证")), mutation);
        }
    }

    @Test void rejectsForeignEvidenceAndFabricatedQuotes() {
        assertThrows(TrainingException.class, () -> validator.validate(validScore(json, "other-user", "验证"), Map.of("1", "验证")));
        assertThrows(TrainingException.class, () -> validator.validate(validScore(json, "1", "不存在的原文"), Map.of("1", "验证")));
        var numericId = validScore(json, "1", "验证");
        ((ObjectNode) numericId.path("scenario_score").path("evidence").get(0)).put("sourceId", 1);
        assertThrows(TrainingException.class, () -> validator.validate(numericId, Map.of("1", "验证")));
    }

    @Test void validatesTextEnvelopeWithoutLegacyCardFieldLoss() throws Exception {
        var root = json.createObjectNode();
        root.set("training_evaluation", validScore(json, "1", "验证").path("scenario_score"));
        var events = new TrainingPlatformEvents(json, ignored -> {});
        events.accept(json.writeValueAsString(Map.of("type", "TEXT_MESSAGE_CONTENT", "delta", root.toString())));
        events.accept("{\"type\":\"RUN_FINISHED\"}");
        assertNull(events.result().score());
        assertEquals(77, validator.validate(json.readTree(events.result().text()), Map.of("1", "验证")).path("total").asInt());
        root.set("scenario_score", root.get("training_evaluation"));
        assertThrows(TrainingException.class, () -> validator.validate(root, Map.of("1", "验证")));
    }

    @Test void evidenceReferencesResolveOnlyToOwnedExactText() {
        var answers = new java.util.LinkedHashMap<String, String>();
        answers.put("9007199254740993", "先核对需求。再验证并发。" + "长".repeat(349) + "😀完成。");
        var catalog = TrainingEvidenceCatalog.from(answers);
        assertEquals("先核对需求。", catalog.get("A1E1").quote());
        for (var excerpt : catalog.values()) {
            assertTrue(answers.get(excerpt.sourceId()).contains(excerpt.quote()));
            assertTrue(excerpt.quote().length() <= 350);
            assertFalse(Character.isHighSurrogate(excerpt.quote().charAt(excerpt.quote().length() - 1)));
        }
        var root = validScore(json, "unused", "unused");
        for (var item : root.path("scenario_score").path("evidence")) {
            var object = (ObjectNode) item;
            object.remove(java.util.List.of("sourceType", "sourceId", "quote"));
            object.put("evidenceId", "A1E1");
        }
        var validated = validator.validate(root, answers);
        assertEquals("9007199254740993", validated.path("evidence").get(0).path("sourceId").asText());
        assertEquals("先核对需求。", validated.path("evidence").get(0).path("quote").asText());
        ((ObjectNode) root.path("scenario_score").path("evidence").get(0)).put("evidenceId", "A99E99");
        assertThrows(TrainingException.class, () -> validator.validate(root, answers));
    }

    @Test void preservesRawCardAlongsideIncrementalText() throws Exception {
        var pieces = new ArrayList<String>();
        var events = new TrainingPlatformEvents(json, pieces::add);
        events.accept("{\"type\":\"TEXT_MESSAGE_CONTENT\",\"delta\":\"你好\"}");
        events.accept("{\"type\":\"TEXT_MESSAGE_CONTENT\",\"delta\":\"，请介绍经历\"}");
        var card = json.createObjectNode().put("type", "CUSTOM");
        card.putObject("value").set("data", validScore(json, "1", "验证"));
        events.accept(json.writeValueAsString(card));
        events.accept("{\"type\":\"RUN_FINISHED\"}");
        assertEquals(2, pieces.size());
        assertEquals("你好，请介绍经历", events.result().text());
        assertTrue(events.result().score().has("scenario_score"));
    }

    @Test void runErrorDisconnectAndEmptyCompletionAreNotSuccessfulReplies() {
        var error = new TrainingPlatformEvents(json, ignored -> {});
        assertThrows(TrainingException.class, () -> error.accept("{\"type\":\"RUN_ERROR\",\"message\":\"private upstream details\"}"));
        var interrupted = new TrainingPlatformEvents(json, ignored -> {});
        interrupted.accept("{\"type\":\"TEXT_MESSAGE_CONTENT\",\"delta\":\"partial\"}");
        assertThrows(TrainingException.class, interrupted::result);
        var empty = new TrainingPlatformEvents(json, ignored -> {});
        empty.accept("{\"type\":\"RUN_FINISHED\"}");
        assertThrows(TrainingException.class, empty::result);
    }

    @Test void encryptedTrainingWritesUseDifferentNoncesAndDetectTampering() {
        var cipher = new TrainingContentCipher("training-key-16!");
        String first = cipher.encrypt("回答中的个人经历"), second = cipher.encrypt("回答中的个人经历");
        assertNotEquals(first, second);
        assertEquals("回答中的个人经历", cipher.decrypt(first));
        assertThrows(TrainingException.class, () -> cipher.decrypt(first.substring(0, first.length() - 5) + "AAAAA"));
    }
}
