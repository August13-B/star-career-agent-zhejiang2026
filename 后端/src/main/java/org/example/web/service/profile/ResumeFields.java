package org.example.web.service.profile;

import java.util.List;
import java.util.Map;

/**
 * 简历解析出的「五件套」结构化结果（AI 输出 → Java 对象）。
 *
 * @param education         最高学历（原文未写则空串）
 * @param major             专业
 * @param skill             技能列表
 * @param certificate       证书列表
 * @param workExperience    实习/工作经历（每段一条）
 * @param projectExperience 项目经历（每个一条）
 * @param scores            硬实力四项建议分：education / internship / professional / certificate
 * @param reasons           各建议分的依据（引用简历事实，用于留痕与前端展示）
 */
public record ResumeFields(
        String education,
        String major,
        List<String> skill,
        List<String> certificate,
        List<String> workExperience,
        List<String> projectExperience,
        Map<String, Integer> scores,
        Map<String, String> reasons
) {
    public ResumeFields {
        skill = skill == null ? List.of() : List.copyOf(skill);
        certificate = certificate == null ? List.of() : List.copyOf(certificate);
        workExperience = workExperience == null ? List.of() : List.copyOf(workExperience);
        projectExperience = projectExperience == null ? List.of() : List.copyOf(projectExperience);
        scores = scores == null ? Map.of() : Map.copyOf(scores);
        reasons = reasons == null ? Map.of() : Map.copyOf(reasons);
    }
}
