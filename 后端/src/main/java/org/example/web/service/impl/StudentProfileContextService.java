package org.example.web.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.web.entity.MatchRecord;
import org.example.web.entity.StudentAbility;
import org.example.web.entity.StudentAbilityScore;
import org.example.web.entity.StudentProfile;
import org.example.web.mapper.MatchRecordMapper;
import org.example.web.mapper.StudentAbilityMapper;
import org.example.web.mapper.StudentAbilityScoreMapper;
import org.example.web.mapper.StudentProfileMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 报告输入上下文构建：把「用户账号下的画像」拼成结构化文本
 *
 * <p>覆盖：学生基本信息、10 维能力评分、能力描述文本、最近一次人岗匹配结果，
 * 以及用户在页面上的临时覆盖项（目标岗位 / 补充说明）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentProfileContextService {

    private final StudentProfileMapper studentProfileMapper;
    private final StudentAbilityMapper studentAbilityMapper;
    private final StudentAbilityScoreMapper studentAbilityScoreMapper;
    private final MatchRecordMapper matchRecordMapper;
    private final org.example.web.tool.RSA_256 rsa256;

    /**
     * 画像敏感字段解密
     * <p>写入用 {@code rsaEncrypt}（RSA），故优先 {@code rsaDecrypt}；
     * 兼容早期用 AES({@code encryptForDB}) 写入的数据；均失败则回退原值。
     */
    private String dec(String v) {
        if (v == null || v.isBlank()) {
            return v;
        }
        String t = v.trim();
        try {
            String plain = rsa256.rsaDecrypt(t);
            if (plain != null && !plain.isBlank()) {
                return plain;
            }
        } catch (Exception ignore) {
            // 非 RSA 密文，继续尝试 AES
        }
        try {
            String plain = rsa256.decryptFromDB(t);
            if (plain != null && !plain.isBlank()) {
                return plain;
            }
        } catch (Exception ignore) {
            // 非密文，保持原值
        }
        return v;
    }

    /**
     * @param userId         用户ID
     * @param overrideTarget 前端覆盖的目标岗位（可为空）
     * @param extraNote      前端补充说明（可为空）
     */
    public String build(Long userId, String overrideTarget, String extraNote) {
        StringBuilder sb = new StringBuilder();
        sb.append("【用户账号下的画像数据】\n");

        try {
            StudentProfile p = firstOf(studentProfileMapper.selectByUserId(userId));
            if (p != null) {
                sb.append("[基本信息]\n");
                append(sb, "姓名", dec(p.getUserName()));
                append(sb, "学历", dec(p.getEducation()));
                append(sb, "学院/专业", join(dec(p.getCollege()), dec(p.getMajor())));
                append(sb, "年级", dec(p.getGrade()));
                append(sb, "毕业时间", p.getGraduationDate() == null ? null : p.getGraduationDate().toLocalDate().toString());
                append(sb, "职业意向", dec(p.getCareerIntentions()));
                append(sb, "职位意向详情", dec(p.getJobIntentionDetail()));
                append(sb, "目标城市", dec(p.getTargetCity()));
                append(sb, "期望薪资", dec(p.getExpectedSalary()));
                append(sb, "行业偏好", dec(p.getIndustryPreference()));
                if (p.getWorkTypePreference() != null) {
                    append(sb, "工作类型偏好", String.valueOf(p.getWorkTypePreference()));
                }
                if (p.getMaxLearningCycle() != null) {
                    append(sb, "可接受学习周期(月)", String.valueOf(p.getMaxLearningCycle()));
                }
                append(sb, "技能", dec(p.getSkill()));
                append(sb, "证书", dec(p.getCertificate()));
                append(sb, "项目经验", dec(p.getProjectExperience()));
                append(sb, "工作/实习经历", dec(p.getWorkExperience()));
                sb.append("\n");
            } else {
                sb.append("[基本信息] 未填写（请在报告中按常见情况做假设）\n\n");
            }

            StudentAbilityScore score = firstOf(studentAbilityScoreMapper.selectByUserId(userId));
            if (score != null) {
                sb.append("[10 维能力评分（0-100）]\n");
                sb.append("- 学历背景：").append(n(score.getEducationScore())).append("\n");
                sb.append("- 实习经历：").append(n(score.getInternshipScore())).append("\n");
                sb.append("- 专业技能：").append(n(score.getProfessionalScore())).append("\n");
                sb.append("- 证书资质：").append(n(score.getCertificateScore())).append("\n");
                sb.append("- 创新能力：").append(n(score.getInnovationScore())).append("\n");
                sb.append("- 学习能力：").append(n(score.getLearningScore())).append("\n");
                sb.append("- 抗压能力：").append(n(score.getPressureScore())).append("\n");
                sb.append("- 沟通能力：").append(n(score.getCommunicationScore())).append("\n");
                sb.append("- 问题解决：").append(n(score.getProblemSolvingScore())).append("\n");
                sb.append("- 团队协作：").append(n(score.getTeamworkScore())).append("\n");
                sb.append("- 综合得分：").append(score.getTotalScore() == null ? "—" : score.getTotalScore()).append("\n\n");
            } else {
                sb.append("[10 维能力评分] 暂无数据\n\n");
            }

            StudentAbility ability = firstOf(studentAbilityMapper.selectByUserId(userId));
            if (ability != null) {
                sb.append("[能力描述文本]\n");
                append(sb, "专业技能", dec(ability.getProfessionalSkill()));
                append(sb, "实习能力", dec(ability.getInternshipAbility()));
                append(sb, "证书要求", dec(ability.getCertificateRequirement()));
                append(sb, "创新能力", dec(ability.getInnovationAbility()));
                append(sb, "学习能力", dec(ability.getLearningAbility()));
                append(sb, "抗压能力", dec(ability.getPressureResistance()));
                append(sb, "沟通能力", dec(ability.getCommunicationAbility()));
                append(sb, "问题解决", dec(ability.getProblemSolving()));
                append(sb, "团队协作", dec(ability.getTeamworkAbility()));
                sb.append("\n");
            }

            List<MatchRecord> matches = matchRecordMapper.selectByUserId(userId);
            if (matches != null && !matches.isEmpty()) {
                MatchRecord m = matches.get(0);
                sb.append("[最近一次人岗匹配]\n");
                if (m.getJobId() != null) {
                    append(sb, "目标岗位ID", String.valueOf(m.getJobId()));
                }
                if (m.getTotalScore() != null) {
                    append(sb, "匹配总分", String.valueOf(m.getTotalScore()));
                }
                if (m.getMatchResult() != null) {
                    append(sb, "匹配结论(枚举值)", String.valueOf(m.getMatchResult()));
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            log.warn("构建画像上下文失败（继续生成，仅缺少部分画像）: {}", e.getMessage());
        }

        // 用户本次的覆盖项（优先级最高）
        if (overrideTarget != null && !overrideTarget.isBlank()) {
            sb.append("[本次目标岗位（用户指定，优先于画像中的职业意向）]\n").append(overrideTarget.strip()).append("\n\n");
        }
        if (extraNote != null && !extraNote.isBlank()) {
            sb.append("[用户补充说明]\n").append(extraNote.strip()).append("\n");
        }
        return sb.toString();
    }

    private <T> T firstOf(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private void append(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append("- ").append(label).append("：").append(value.strip()).append("\n");
        }
    }

    private String join(String a, String b) {
        String x = a == null ? "" : a.strip();
        String y = b == null ? "" : b.strip();
        String r = (x + " " + y).strip();
        return r.isEmpty() ? null : r;
    }

    private String n(Object v) {
        return v == null ? "—" : String.valueOf(v);
    }
}
