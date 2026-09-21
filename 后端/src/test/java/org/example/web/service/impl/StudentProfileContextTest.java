package org.example.web.service.impl;

import org.example.web.entity.*;
import org.example.web.mapper.*;
import org.example.web.tool.RSA_256;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wwy.example.springboot.entity.*;
import wwy.example.springboot.service.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentProfileContextTest {
    private final StudentProfileMapper profiles = mock(StudentProfileMapper.class);
    private final StudentAbilityMapper abilities = mock(StudentAbilityMapper.class);
    private final StudentAbilityScoreMapper scores = mock(StudentAbilityScoreMapper.class);
    private final MatchRecordMapper matches = mock(MatchRecordMapper.class);
    private final RSA_256 rsa = mock(RSA_256.class);
    private final JobRequirementProfileService jobs = mock(JobRequirementProfileService.class);
    private final JobHardRequirementService hard = mock(JobHardRequirementService.class);
    private final JobSkillRequirementService skills = mock(JobSkillRequirementService.class);
    private final JobSoftRequirementService soft = mock(JobSoftRequirementService.class);
    private StudentProfileContextService service;

    @BeforeEach
    void setUp() {
        service = new StudentProfileContextService(profiles, abilities, scores, matches, rsa, jobs, hard, skills, soft);
    }

    @Test
    void latestProfileAndCompletedMatchIncludeActualJobRequirements() {
        var old = new StudentProfile(); old.setGrade("大一"); old.setUpdateTime(LocalDateTime.of(2025, 1, 1, 0, 0));
        var current = new StudentProfile(); current.setGrade("大三"); current.setSkill("Vue 项目经验"); current.setUpdateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(profiles.selectByUserId(1L)).thenReturn(List.of(old, current));
        var score = new StudentAbilityScore(); score.setProfessionalScore(78);
        when(scores.selectByUserId(1L)).thenReturn(List.of(score));
        var pending = new MatchRecord(); pending.setMatchStatus(1); pending.setJobId(99L);
        var match = new MatchRecord(); match.setMatchStatus(2); match.setJobId(2L); match.setMatchResult(2); match.setSkillScore(new BigDecimal("65"));
        when(matches.selectByUserId(1L)).thenReturn(List.of(pending, match));
        var job = new JobRequirementProfile(); job.setId(2L); job.setPositionName("前端工程师"); job.setLevel(1);
        when(jobs.findById(2L)).thenReturn(job);
        var requirement = new JobSkillRequirement(); requirement.setProfessionalSkill("TypeScript、Vue、单元测试");
        when(skills.findByJobId(2L)).thenReturn(requirement);
        var hardRequirement = new JobHardRequirement(); hardRequirement.setEducationRequirement("本科");
        when(hard.findByJobId(2L)).thenReturn(hardRequirement);
        String context = service.buildChat(1L);
        assertTrue(context.contains("年级：大三"));
        assertFalse(context.contains("年级：大一"));
        assertTrue(context.contains("专业技能：78"));
        assertTrue(context.contains("前端工程师"));
        assertTrue(context.contains("TypeScript、Vue、单元测试"));
        assertTrue(context.contains("学历要求：本科"));
        assertTrue(context.contains("不要重复询问已有信息"));
        verify(jobs, never()).findById(99L);
        verify(profiles).selectByUserId(1L);
    }

    @Test
    void careerIntentionLooksUpBoundedReferenceCandidatesWithoutMatch() {
        var profile = new StudentProfile(); profile.setCareerIntentions("前端");
        when(profiles.selectByUserId(1L)).thenReturn(List.of(profile));
        var candidate = new JobRequirementProfile(); candidate.setId(3L); candidate.setPositionName("Web 前端");
        var page = new Page<JobRequirementProfile>(); page.setRecords(List.of(candidate));
        when(jobs.pageQuery(1, 2, "前端", null, null)).thenReturn(page);
        String context = service.buildChat(1L);
        assertTrue(context.contains("Web 前端"));
        assertTrue(context.contains("参考候选，未经用户确认"));
        verify(jobs).pageQuery(1, 2, "前端", null, null);
    }

    @Test
    void missingProfileDoesNotInventFactsOrEnumerateAllJobs() {
        String context = service.buildChat(1L);
        assertTrue(context.contains("未填写"));
        assertTrue(context.contains("不得编造"));
        assertTrue(context.contains("当前用户明确表达的目标优先"));
        verifyNoInteractions(jobs, hard, skills, soft);
    }

    @Test
    void profileIsRefreshedEveryTurnAndDecryptedBeforeUse() {
        var profile = new StudentProfile(); profile.setSkill("ciphertext");
        when(profiles.selectByUserId(1L)).thenReturn(List.of(profile));
        when(rsa.rsaDecrypt("ciphertext")).thenReturn("HTML").thenReturn("HTML 与 Vue");
        assertTrue(service.buildChat(1L).contains("技能：HTML\n"));
        assertTrue(service.buildChat(1L).contains("技能：HTML 与 Vue"));
        verify(profiles, times(2)).selectByUserId(1L);
    }

    @Test
    void missingJobDetailsDoNotDiscardAlreadyLoadedPersonalProfile() {
        var profile = new StudentProfile(); profile.setMajor("计算机"); profile.setCareerIntentions("前端");
        when(profiles.selectByUserId(1L)).thenReturn(List.of(profile));
        when(jobs.pageQuery(1, 2, "前端", null, null)).thenThrow(new IllegalStateException("岗位数据暂不可用"));
        assertTrue(service.buildChat(1L).contains("计算机"));
    }
}
