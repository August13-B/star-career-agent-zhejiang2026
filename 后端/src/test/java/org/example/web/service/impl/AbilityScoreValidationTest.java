package org.example.web.service.impl;

import org.example.web.entity.StudentAbilityScore;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class AbilityScoreValidationTest {
    private StudentAbilityScore complete() {
        var score = new StudentAbilityScore();
        score.setEducationScore(60); score.setInternshipScore(60);
        score.setProfessionalScore(60); score.setCertificateScore(60);
        score.setInnovationScore(60); score.setLearningScore(60);
        score.setPressureScore(60); score.setCommunicationScore(60);
        score.setProblemSolvingScore(60); score.setTeamworkScore(60);
        score.setTotalScore(BigDecimal.valueOf(60)); score.setScoreComment("基于已提供资料的参考测评");
        return score;
    }
    @Test void acceptsCompleteScore() { assertDoesNotThrow(() -> AIAnalysisServiceImpl.validateScore(complete())); }
    @Test void rejectsMissingDimension() {
        var score = complete(); score.setLearningScore(null);
        assertThrows(IllegalArgumentException.class, () -> AIAnalysisServiceImpl.validateScore(score));
    }
    @Test void rejectsOutOfRange() {
        var score = complete(); score.setProfessionalScore(101);
        assertThrows(IllegalArgumentException.class, () -> AIAnalysisServiceImpl.validateScore(score));
    }
    @Test void rejectsMissingComment() {
        var score = complete(); score.setScoreComment(" ");
        assertThrows(IllegalArgumentException.class, () -> AIAnalysisServiceImpl.validateScore(score));
    }
}
