package org.example.web.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    @Test void regeneratesAndOverwritesWhenForced() throws Exception {
        var r=report();when(reports.selectById(9L)).thenReturn(r);
        when(ai.chatSync(eq(42L),isNull(),anyString()))
                .thenReturn("{\"center\":\"前端开发\",\"branches\":[{\"name\":\"前端开发\",\"kind\":\"target\",\"evidence\":\"建议前端开发\",\"skills\":[\"Vue\"],\"actions\":[\"项目练习\"]}]}")
                .thenReturn("{\"center\":\"前端开发\",\"branches\":[{\"name\":\"进阶为高级前端\",\"kind\":\"promotion\",\"evidence\":\"补齐Vue工程能力\",\"skills\":[\"工程化\"],\"actions\":[\"重构练习\"]}]}");
        when(reports.saveGraphIfUnchanged(eq(9L),eq(42L),anyString(),anyString()))
                .thenAnswer(call->{r.setReportContent(call.getArgument(3));return 1;});

        var first=service.generate(42L,9L);
        assertEquals("前端开发",first.path("branches").get(0).path("name").asText());
        // 不带 force：命中已保存星图，不再调 AI
        assertEquals(first,service.generate(42L,9L));
        // 带 force（前端「重新生成星图」）：重新提取并覆盖已保存星图
        var second=service.generate(42L,9L,true);
        assertEquals("进阶为高级前端",second.path("branches").get(0).path("name").asText());
        assertEquals(second,service.get(42L,9L));
        verify(ai,times(2)).chatSync(eq(42L),isNull(),anyString());
    }

    @Test void sanitizesSkillsAndActionsAndRejectsEmptyOnes() throws Exception {
        var source=json.readTree(report().getReportContent()).path("agents");
        // 超 3 条 + 空白项 → 截断为 3 条、剔除空串，并去掉首尾空白
        var graph=json.readTree("{\"center\":\"前端开发\",\"branches\":[{\"name\":\" 前端开发 \",\"kind\":\"target\",\"evidence\":\" 建议前端开发 \",\"reason\":\" 理由 \",\"skills\":[\"Vue\",\" \",\"工程化\",\"  测试\",\"第四条\"],\"actions\":[]}]}");
        ReportGraphService.validate(graph,source);
        var branch=graph.path("branches").get(0);
        assertEquals("前端开发",branch.path("name").asText());
        assertEquals("建议前端开发",branch.path("evidence").asText());
        assertEquals(3,branch.path("skills").size());
        assertEquals("工程化",branch.path("skills").get(1).asText());
        assertEquals(0,branch.path("actions").size());

        // skills 与 actions 同时为空 → 拒绝
        var empty=json.readTree("{\"center\":\"前端开发\",\"branches\":[{\"name\":\"前端开发\",\"kind\":\"target\",\"evidence\":\"建议前端开发\",\"skills\":[],\"actions\":[]}]}");
        assertThrows(IllegalArgumentException.class,()->ReportGraphService.validate(empty,source));
        // skills 不是数组 → 拒绝
        var bad=json.readTree("{\"center\":\"前端开发\",\"branches\":[{\"name\":\"前端开发\",\"kind\":\"target\",\"evidence\":\"建议前端开发\",\"skills\":\"Vue\",\"actions\":[\"练习\"]}]}");
        assertThrows(IllegalArgumentException.class,()->ReportGraphService.validate(bad,source));
    }

    @Test void toleratesPlatformQuoteRewritingInEvidence() throws Exception {
        var r=report();
        var content=(ObjectNode)json.readTree(r.getReportContent());
        // 报告原文带 ASCII 双引号
        content.path("agents").forEach(a->((ObjectNode)a).put("content","报告建议\"前端开发\"方向，需补齐Vue工程能力"));
        var source=content.path("agents");
        // 严格逐字引用 → 通过
        var strict=json.readTree("{\"center\":\"前端开发\",\"branches\":[{\"name\":\"前端开发\",\"kind\":\"target\",\"evidence\":\"报告建议\\\"前端开发\\\"方向\",\"skills\":[\"Vue\"],\"actions\":[]}]}");
        ReportGraphService.validate(strict,source);
        // 平台侧把裸引号换成「」→ 不再逐字相同，但去引号后一致 → 仍通过
        var rewritten=json.readTree("{\"center\":\"前端开发\",\"branches\":[{\"name\":\"前端开发\",\"kind\":\"target\",\"evidence\":\"报告建议「前端开发」方向\",\"skills\":[\"Vue\"],\"actions\":[]}]}");
        ReportGraphService.validate(rewritten,source);
        // 与原文无关的引用依然被拒（不能因容错变成模糊匹配）
        var fabricated=json.readTree("{\"center\":\"前端开发\",\"branches\":[{\"name\":\"后端开发\",\"kind\":\"target\",\"evidence\":\"报告建议「后端开发」方向\",\"skills\":[\"Java\"],\"actions\":[]}]}");
        assertThrows(IllegalArgumentException.class,()->ReportGraphService.validate(fabricated,source));
    }
}
