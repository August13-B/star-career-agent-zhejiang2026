package org.example.web.service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.example.web.entity.CareerReport;
import org.example.web.mapper.CareerReportMapper;
import org.example.web.service.TboxAgentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 多智能体报告 → 画像计分：越权/未完成拒绝、幂等不重复调 AI、无依据维度不打分。 */
class ReportProfileScoreTest {

    final CareerReportMapper reports = mock(CareerReportMapper.class);
    final ReportProfileOutcomeService outcome = mock(ReportProfileOutcomeService.class);
    final TboxAgentService ai = mock(TboxAgentService.class);
    final ObjectMapper json = new ObjectMapper();
    final ReportProfileScoreService service = new ReportProfileScoreService(reports, outcome, ai, json);

    CareerReport report(boolean scored) {
        CareerReport r = new CareerReport();
        r.setId(9L);
        r.setUserId(42L);
        r.setStatus(2);
        r.setIsDeleted(0);
        var root = json.createObjectNode();
        var agents = root.putArray("agents");
        for (String key : List.of("profile_analysis", "career_exploration", "goal_setting",
                "path_planning", "action_planning", "report_composition")) {
            agents.addObject().put("key", key).put("content", "沟通表达清晰，团队协作良好");
        }
        if (scored) {
            root.put("profileScoredAt", "2026-10-08T00:00:00Z");
        }
        r.setReportContent(root.toString());
        return r;
    }

    @Test void rejectsOtherUserAndIncompleteReport() {
        when(reports.selectById(9L)).thenReturn(report(false));
        assertThrows(ResumeParseException.class, () -> service.score(43L, 9L));

        CareerReport partial = report(false);
        partial.setReportContent("{\"agents\":[]}");
        when(reports.selectById(9L)).thenReturn(partial);
        assertThrows(ResumeParseException.class, () -> service.score(42L, 9L));
        verifyNoInteractions(ai);
        verifyNoInteractions(outcome);
    }

    @Test void skipsAiWhenAlreadyScored() {
        when(reports.selectById(9L)).thenReturn(report(true));
        Map<String, Object> result = service.score(42L, 9L);
        assertEquals("already_scored", result.get("status"));
        assertEquals(42L, 42L);
        verifyNoInteractions(ai);
        verifyNoInteractions(outcome);
    }

    @Test void parsesOnlyDimensionsWithEvidence() {
        when(reports.selectById(9L)).thenReturn(report(false));
        when(ai.chatSync(eq(42L), isNull(), anyString())).thenReturn(
                "{\"scores\":{\"communication\":78,\"teamwork\":72,\"learning\":90},"
                        + "\"reasons\":{\"communication\":\"沟通表达清晰\",\"teamwork\":\"团队协作良好\",\"learning\":\"x\"}}");
        when(outcome.apply(eq(42L), eq(9L), anyMap(), anyMap())).thenReturn(Map.of("status", "applied"));

        service.score(42L, 9L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> scores = ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> reasons = ArgumentCaptor.forClass(Map.class);
        verify(outcome).apply(eq(42L), eq(9L), scores.capture(), reasons.capture());
        assertEquals(Set.of("communication", "teamwork"), scores.getValue().keySet());
        assertEquals(78, scores.getValue().get("communication"));
        assertEquals(2, reasons.getValue().size());
    }

    @Test void rejectsWhenAiGivesNothingUsable() {
        when(reports.selectById(9L)).thenReturn(report(false));
        when(ai.chatSync(eq(42L), isNull(), anyString())).thenReturn("抱歉，我无法评估。");
        assertThrows(ResumeParseException.class, () -> service.score(42L, 9L));
        verifyNoInteractions(outcome);
    }
}
