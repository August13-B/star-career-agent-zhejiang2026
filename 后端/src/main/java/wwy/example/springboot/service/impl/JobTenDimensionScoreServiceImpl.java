package wwy.example.springboot.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wwy.example.springboot.dto.JobTenDimensionScoreDTO;
import wwy.example.springboot.entity.*;
import wwy.example.springboot.service.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobTenDimensionScoreServiceImpl implements JobTenDimensionScoreService {

    private final JobRequirementProfileService profileService;
    private final JobHardRequirementService hardRequirementService;
    private final JobSkillRequirementService skillRequirementService;
    private final JobSoftRequirementService softRequirementService;

    @Override
    public JobTenDimensionScoreDTO calculateScores(Long profileId) {
        JobRequirementProfile profile = profileService.findById(profileId);
        if (profile == null) {
            throw new RuntimeException("岗位画像不存在，id=" + profileId);
        }

        JobHardRequirement hard = hardRequirementService.findByJobId(profileId);
        JobSkillRequirement skill = skillRequirementService.findByJobId(profileId);
        JobSoftRequirement soft = softRequirementService.findByJobId(profileId);

        JobTenDimensionScoreDTO dto = new JobTenDimensionScoreDTO();
        dto.setJobId(profileId);
        dto.setJobName(profile.getPositionName());

        // 计算10个维度分数
        dto.setEducationScore(calcEducationScore(hard));
        dto.setInternshipScore(calcInternshipScore(hard));
        dto.setProfessionalSkillScore(calcProfessionalSkillScore(skill));
        dto.setCertificateScore(calcCertificateScore(skill));
        dto.setInnovationScore(calcSoftScore(soft != null ? soft.getInnovationAbility() : null));
        dto.setLearningScore(calcSoftScore(soft != null ? soft.getLearningAbility() : null));
        dto.setPressureScore(calcSoftScore(soft != null ? soft.getPressureResistance() : null));
        dto.setCommunicationScore(calcSoftScore(soft != null ? soft.getCommunicationAbility() : null));
        dto.setProblemSolvingScore(calcSoftScore(soft != null ? soft.getProblemSolving() : null));
        dto.setTeamworkScore(calcSoftScore(soft != null ? soft.getTeamworkAbility() : null));

        // 计算总分（10个维度平均）
        BigDecimal sum = dto.getEducationScore()
                .add(dto.getInternshipScore())
                .add(dto.getProfessionalSkillScore())
                .add(dto.getCertificateScore())
                .add(dto.getInnovationScore())
                .add(dto.getLearningScore())
                .add(dto.getPressureScore())
                .add(dto.getCommunicationScore())
                .add(dto.getProblemSolvingScore())
                .add(dto.getTeamworkScore());
        BigDecimal total = sum.divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
        dto.setTotalScore(total);

        return dto;
    }

    /**
     * 学历打分
     */
    private BigDecimal calcEducationScore(JobHardRequirement hard) {
        if (hard == null || hard.getEducationRequirement() == null) {
            return BigDecimal.valueOf(60);
        }
        String edu = hard.getEducationRequirement();
        if (edu.contains("博士")) return BigDecimal.valueOf(100);
        if (edu.contains("硕士")) return BigDecimal.valueOf(90);
        if (edu.contains("本科")) return BigDecimal.valueOf(70);
        if (edu.contains("大专")) return BigDecimal.valueOf(50);
        return BigDecimal.valueOf(40);
    }

    /**
     * 实习经历打分
     */
    private BigDecimal calcInternshipScore(JobHardRequirement hard) {
        if (hard == null || hard.getInternshipRequirement() == null) {
            return BigDecimal.valueOf(50);
        }
        String text = hard.getInternshipRequirement();
        if (text.contains("3年") || text.contains("三年")) return BigDecimal.valueOf(90);
        if (text.contains("2年") || text.contains("两年")) return BigDecimal.valueOf(80);
        if (text.contains("1年") || text.contains("一年")) return BigDecimal.valueOf(70);
        if (text.contains("优先") || text.contains("有实习")) return BigDecimal.valueOf(60);
        return BigDecimal.valueOf(50);
    }

    /**
     * 专业技能打分（基于技能数量）
     */
    private BigDecimal calcProfessionalSkillScore(JobSkillRequirement skill) {
        if (skill == null || skill.getProfessionalSkill() == null) {
            return BigDecimal.valueOf(50);
        }
        int count = 0;
        String skillsJson = skill.getProfessionalSkill();
        try {
            JSONArray arr = JSONUtil.parseArray(skillsJson);
            count = arr.size();
        } catch (Exception e) {
            count = skillsJson.split(",").length;
        }
        if (count >= 8) return BigDecimal.valueOf(95);
        if (count >= 6) return BigDecimal.valueOf(85);
        if (count >= 4) return BigDecimal.valueOf(75);
        if (count >= 2) return BigDecimal.valueOf(65);
        if (count >= 1) return BigDecimal.valueOf(55);
        return BigDecimal.valueOf(50);
    }

    /**
     * 证书要求打分
     */
    private BigDecimal calcCertificateScore(JobSkillRequirement skill) {
        if (skill == null || skill.getCertificateRequirement() == null) {
            return BigDecimal.valueOf(50);
        }
        int count = 0;
        String certJson = skill.getCertificateRequirement();
        try {
            JSONArray arr = JSONUtil.parseArray(certJson);
            count = arr.size();
        } catch (Exception e) {
            count = certJson.split(",").length;
        }
        if (count >= 5) return BigDecimal.valueOf(95);
        if (count >= 3) return BigDecimal.valueOf(80);
        if (count >= 1) return BigDecimal.valueOf(65);
        return BigDecimal.valueOf(50);
    }

    /**
     * 软实力通用打分（有描述 = 75分，无描述 = 60分）
     */
    private BigDecimal calcSoftScore(String text) {
        if (text == null || text.isEmpty()) {
            return BigDecimal.valueOf(60);
        }
        // 根据描述长度/关键词可进一步细化
        if (text.length() > 20) {
            return BigDecimal.valueOf(80);
        }
        return BigDecimal.valueOf(75);
    }
}