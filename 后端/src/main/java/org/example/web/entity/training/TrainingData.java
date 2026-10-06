package org.example.web.entity.training;

import java.time.LocalDateTime;
import lombok.Data;

/** Persistence records. Content fields contain ciphertext; only owned response DTOs are decrypted. */
public final class TrainingData {
    private TrainingData() {}

    @Data public static class Config {
        private Long sessionId;
        private String templateSnapshot;
        private String difficulty;
        private Boolean useForProfile;
        private Long baselineScoreId;
        private Integer baselineProfileVersion;
        private String artifactDraft;
        private Integer artifactDraftVersion;
        private Long selectedArtifactId;
    }
    @Data public static class Artifact {
        private Long id;
        private Long sessionId;
        private Integer revision;
        private String clientRequestId;
        private String inputHash;
        private String contentJson;
        private LocalDateTime createTime;
    }
    @Data public static class Application {
        private Long sessionId;
        private String status;
        private String policyVersion;
        private Integer attempt;
        private String beforeScores;
        private String afterScores;
        private Integer profileVersion;
        private Long scoreHistoryId;
        private String message;
        private LocalDateTime updateTime;
    }

    @Data
    public static class Session {
        private Long id;
        private Long userId;
        private String templateId;
        private String status;
        private Integer answeredCount;
        private Integer version;
        private String draft;
        private Integer draftVersion;
        private String clientRequestId;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data
    public static class Turn {
        private Long id;
        private Long sessionId;
        private Long runId;
        private String role;
        private Integer ordinal;
        private String content;
        private String status;
        private LocalDateTime createTime;
    }

    @Data
    public static class Run {
        private Long id;
        private Long sessionId;
        private String operation;
        private String status;
        private Integer attempt;
        private String clientRequestId;
        private String inputHash;
        private String requestJson;
        private Long responseMessageId;
        private String errorCode;
        private String errorMessage;
        private String rawResult;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data
    public static class Evaluation {
        private Long id;
        private Long sessionId;
        private Long runId;
        private String status;
        private String resultJson;
        private String message;
        private LocalDateTime createTime;
    }
}
