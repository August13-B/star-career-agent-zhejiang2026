package org.example.web.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.web.entity.CareerReport;
import org.example.web.mapper.CareerReportMapper;
import org.example.web.service.TboxAgentService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportGraphTest {
    final CareerReportMapper reports=mock(CareerReportMapper.class);
    final TboxAgentService ai=mock(TboxAgentService.class);
    final ObjectMapper json=new ObjectMapper();
    final ReportGraphService service=new ReportGraphService(reports,ai,json);
    CareerReport report() {
        var r=new CareerReport();r.setId(9L);r.setUserId(42L);r.setStatus(2);
        var root=json.createObjectNode();var agents=root.putArray("agents");
        for(String key:List.of("profile_analysis","career_exploration","goal_setting","path_planning","action_planning","report_composition"))
            agents.addObject().put("key",key).put("content","建议前端开发，补齐Vue工程能力");
        r.setReportContent(root.toString());return r;
    }
    @Test void rejectsOtherUsersAndIncompleteAssessments() {
        when(reports.selectById(9L)).thenReturn(report());
        assertThrows(IllegalArgumentException.class,()->service.get(43L,9L));
        var partial=report();partial.setReportContent("{\"agents\":[]}");when(reports.selectById(9L)).thenReturn(partial);
        assertThrows(IllegalArgumentException.class,()->service.generate(42L,9L));verifyNoInteractions(ai);
    }
    @Test void persistsAndRestoresGraphWithoutCallingAiAgain() throws Exception {
        var r=report();when(reports.selectById(9L)).thenReturn(r);
        when(ai.chatSync(eq(42L),isNull(),anyString())).thenReturn("{\"center\":\"前端开发\",\"branches\":[{\"name\":\"前端开发\",\"kind\":\"target\",\"evidence\":\"建议前端开发\",\"skills\":[\"Vue\"],\"actions\":[\"项目练习\"]}]}");
        when(reports.saveGraphIfUnchanged(eq(9L),eq(42L),anyString(),anyString())).thenAnswer(call->{r.setReportContent(call.getArgument(3));return 1;});
        var graph=service.generate(42L,9L);
        assertEquals(1,graph.path("branches").size());assertEquals(graph,service.get(42L,9L));assertEquals(graph,service.generate(42L,9L));
        verify(ai,times(1)).chatSync(eq(42L),isNull(),anyString());
    }
    @Test void rejectsUnsupportedEvidenceAndEmptyBranches() throws Exception {
        var source=json.readTree(report().getReportContent()).path("agents");
        assertThrows(IllegalArgumentException.class,()->ReportGraphService.validate(json.readTree("{\"center\":\"前端\",\"branches\":[]}"),source));
        assertThrows(IllegalArgumentException.class,()->ReportGraphService.validate(json.readTree("{\"center\":\"前端\",\"branches\":[{\"name\":\"医生\",\"kind\":\"target\",\"evidence\":\"凭空生成的推荐\",\"skills\":[],\"actions\":[]}]}"),source));
    }
}
