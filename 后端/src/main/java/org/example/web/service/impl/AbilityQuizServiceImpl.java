package org.example.web.service.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.example.web.entity.StudentAbility;
import org.example.web.entity.StudentAbilityScore;
import org.example.web.service.AbilityQuizService;
import org.example.web.service.StudentAbilityScoreService;
import org.example.web.service.StudentAbilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 六维能力初步测评实现
 *
 * <p>题库：{@code classpath:data/ability-questions.json}（本地仓库文件，不入库）。
 * 评分：6 个软维度按抽中题目归一化到 0~100；4 个硬维度按基本情况结构化选项查表；
 * 总分 = 硬均分×30% + 软均分×70%。
 */
@Slf4j
@Service
public class AbilityQuizServiceImpl implements AbilityQuizService {

    @Autowired
    private StudentAbilityService studentAbilityService;

    @Autowired
    private StudentAbilityScoreService studentAbilityScoreService;

    @Autowired
    private ObjectMapper objectMapper;

    private volatile JsonNode bank;

    private final Random rnd = new Random();

    private JsonNode bank() {
        if (bank == null) {
            synchronized (this) {
                if (bank == null) {
                    try (java.io.InputStream in = new ClassPathResource("data/ability-questions.json").getInputStream()) {
                        String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                        bank = objectMapper.readTree(json);
                    } catch (Exception e) {
                        throw new IllegalStateException("能力测评题库加载失败: " + e.getMessage(), e);
                    }
                }
            }
        }
        return bank;
    }

    @Override
    public Map<String, Object> drawQuiz() {
        JsonNode root = bank();
        Map<String, List<JsonNode>> byDim = new LinkedHashMap<>();
        for (JsonNode dim : root.path("dimensions")) {
            byDim.put(dim.path("key").asText(), new ArrayList<>());
        }
        for (JsonNode q : root.path("questions")) {
            byDim.computeIfAbsent(q.path("dim").asText(), k -> new ArrayList<>()).add(q);
        }

        // 每维随机 1~2 道（1/3 概率 1 道，2/3 概率 2 道 → 期望约 10 题）
        List<JsonNode> selected = new ArrayList<>();
        for (List<JsonNode> pool0 : byDim.values()) {
            List<JsonNode> pool = new ArrayList<>(pool0);
            Collections.shuffle(pool, rnd);
            int take = rnd.nextInt(3) == 0 ? 1 : 2;
            for (int i = 0; i < Math.min(take, pool.size()); i++) {
                selected.add(pool.get(i));
            }
        }
        Collections.shuffle(selected, rnd);

        List<Map<String, Object>> questions = new ArrayList<>();
        for (JsonNode q : selected) {
            List<Integer> idx = new ArrayList<>();
            for (int i = 0; i < q.path("options").size(); i++) {
                idx.add(i);
            }
            Collections.shuffle(idx, rnd);
            List<Map<String, Object>> opts = new ArrayList<>();
            for (int i : idx) {
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("k", i);   // 原始下标（不含分值），提交时回传
                o.put("t", q.path("options").get(i).path("t").asText(""));
                opts.add(o);
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.path("id").asText());
            item.put("dim", q.path("dim").asText());
            item.put("text", q.path("text").asText());
            item.put("options", opts);
            questions.add(item);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", questions.size());
        out.put("dimensions", objectMapper.convertValue(root.path("dimensions"), List.class));
        out.put("questions", questions);
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submit(Long userId, Map<String, Object> body) {
        if (userId == null) {
            throw new IllegalArgumentException("缺少用户ID");
        }
        JsonNode root = bank();
        Map<String, JsonNode> byId = new HashMap<>();
        for (JsonNode q : root.path("questions")) {
            byId.put(q.path("id").asText(), q);
        }

        // 1) 软维度：累加得分/满分
        Map<String, int[]> soft = new LinkedHashMap<>();
        for (JsonNode dim : root.path("dimensions")) {
            soft.put(dim.path("key").asText(), new int[]{0, 0});
        }
        Object answersRaw = body == null ? null : body.get("answers");
        if (answersRaw instanceof List<?> list) {
            for (Object a : list) {
                if (!(a instanceof Map<?, ?> m)) {
                    continue;
                }
                JsonNode q = byId.get(String.valueOf(m.get("id")));
                if (q == null) {
                    continue;
                }
                int k;
                try {
                    k = Integer.parseInt(String.valueOf(m.get("k")));
                } catch (Exception e) {
                    continue;
                }
                JsonNode opts = q.path("options");
                if (k < 0 || k >= opts.size()) {
                    continue;
                }
                int[] acc = soft.get(q.path("dim").asText());
                if (acc != null) {
                    acc[0] += opts.get(k).path("s").asInt(0);
                    acc[1] += 4;
                }
            }
        }

        // 2) 硬维度：结构化基本情况查表
        Map<String, Object> basic = (body != null && body.get("basic") instanceof Map<?, ?> bm)
                ? objectMapper.convertValue(bm, Map.class) : new HashMap<>();
        JsonNode rules = root.path("hardScoreRules");
        int educationScore = rule(rules.path("education"), str(basic.get("education")), 60);
        int internshipScore = rule(rules.path("internshipMonths"), str(basic.get("internshipMonths")), 60);
        int professionalScore = rule(rules.path("skillLevel"), str(basic.get("skillLevel")), 60);
        int certificateScore = rule(rules.path("certCount"), str(basic.get("certCount")), 60);

        // 3) 归一化 + 总分（硬 30% / 软 70%）
        Map<String, Integer> softScores = new LinkedHashMap<>();
        for (Map.Entry<String, int[]> e : soft.entrySet()) {
            int[] v = e.getValue();
            softScores.put(e.getKey(), v[1] == 0 ? 60 : (int) Math.round(v[0] * 100.0 / v[1]));
        }
        int hardAvg = (educationScore + internshipScore + professionalScore + certificateScore) / 4;
        int softSum = 0;
        for (int v : softScores.values()) {
            softSum += v;
        }
        int softAvg = softScores.isEmpty() ? hardAvg : softSum / softScores.size();
        int total = (int) Math.round(hardAvg * 0.3 + softAvg * 0.7);

        // 4) 写 student_ability（第 1 步硬实力文本，服务内部 RSA 加密）
        StudentAbility ability = upsertHardText(userId, basic);

        // 5) 覆盖 student_ability_score（旧 score_type=1 逻辑删除）
        List<StudentAbilityScore> olds = studentAbilityScoreService.selectByUserId(userId);
        for (StudentAbilityScore old : olds) {
            if (old.getScoreType() != null && old.getScoreType() == 1) {
                studentAbilityScoreService.deleteById(old.getId());
            }
        }
        String comment = buildComment(softScores, educationScore, internshipScore,
                professionalScore, certificateScore, total);
        StudentAbilityScore sc = new StudentAbilityScore();
        sc.setUserId(userId);
        sc.setAbilityId(ability == null ? null : ability.getId());
        sc.setEducationScore(educationScore);
        sc.setInternshipScore(internshipScore);
        sc.setProfessionalScore(professionalScore);
        sc.setCertificateScore(certificateScore);
        sc.setCommunicationScore(softScores.getOrDefault("communication", 60));
        sc.setTeamworkScore(softScores.getOrDefault("teamwork", 60));
        sc.setProblemSolvingScore(softScores.getOrDefault("problem_solving", 60));
        sc.setInnovationScore(softScores.getOrDefault("innovation", 60));
        sc.setLearningScore(softScores.getOrDefault("learning", 60));
        sc.setPressureScore(softScores.getOrDefault("pressure", 60));
        sc.setTotalScore(BigDecimal.valueOf(total));
        sc.setScoreType(1);            // 1-系统自动评分（初步问卷）
        sc.setScoreComment(comment);
        studentAbilityScoreService.insert(sc);

        // 6) 返回（结构化分数给前端做雷达/展示；不含题库内部信息）
        Map<String, Object> scores = new LinkedHashMap<>();
        scores.put("education", educationScore);
        scores.put("internship", internshipScore);
        scores.put("professional", professionalScore);
        scores.put("certificate", certificateScore);
        scores.put("communication", softScores.getOrDefault("communication", 60));
        scores.put("teamwork", softScores.getOrDefault("teamwork", 60));
        scores.put("problem_solving", softScores.getOrDefault("problem_solving", 60));
        scores.put("innovation", softScores.getOrDefault("innovation", 60));
        scores.put("learning", softScores.getOrDefault("learning", 60));
        scores.put("pressure", softScores.getOrDefault("pressure", 60));
        scores.put("total", total);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scores", scores);
        out.put("dimensions", dimensionList(scores));
        out.put("comment", comment);
        log.info("初步能力测评完成: userId={}, total={}", userId, total);
        return out;
    }

    /** 写/更新 student_ability 的 4 个硬实力文本字段（内部 RSA 加密） */
    private StudentAbility upsertHardText(Long userId, Map<String, Object> basic) {
        try {
            List<StudentAbility> existing = studentAbilityService.selectByUserId(userId);
            StudentAbility ab = existing.isEmpty() ? new StudentAbility() : existing.get(0);
            ab.setUserId(userId);
            ab.setEducationRequirement("学历：" + label("education", str(basic.get("education")))
                    + "；专业：" + defaultStr(basic.get("major"), "未填写"));
            ab.setProfessionalSkill("技能：" + defaultStr(basic.get("skillDesc"), "未填写")
                    + "；自评：" + label("skillLevel", str(basic.get("skillLevel"))));
            ab.setInternshipAbility("实习/项目：" + defaultStr(basic.get("internshipDesc"), "未填写")
                    + "；时长：" + label("internshipMonths", str(basic.get("internshipMonths"))));
            ab.setCertificateRequirement("证书：" + defaultStr(basic.get("certDesc"), "未填写")
                    + "；数量：" + label("certCount", str(basic.get("certCount"))));
            if (existing.isEmpty()) {
                List<StudentAbility> r = studentAbilityService.insert(ab);
                return (r != null && !r.isEmpty()) ? r.get(0) : ab;
            }
            List<StudentAbility> r = studentAbilityService.update(ab);
            return (r != null && !r.isEmpty()) ? r.get(0) : ab;
        } catch (Exception e) {
            log.warn("写 student_ability 失败（不阻断评分）: {}", e.getMessage());
            // 可能是"更新后与原先相同"——重新取回现有记录以拿到 abilityId
            try {
                List<StudentAbility> again = studentAbilityService.selectByUserId(userId);
                return again.isEmpty() ? null : again.get(0);
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    private List<Map<String, Object>> dimensionList(Map<String, Object> scores) {
        Object[][] defs = {
                {"education", "学历背景"}, {"internship", "实习经历"}, {"professional", "专业技能"},
                {"certificate", "证书资质"}, {"communication", "沟通能力"}, {"teamwork", "团队协作"},
                {"problem_solving", "问题解决"}, {"innovation", "创新能力"}, {"learning", "学习能力"},
                {"pressure", "抗压能力"}
        };
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] d : defs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", d[0]);
            m.put("name", d[1]);
            m.put("score", scores.getOrDefault(d[0], 60));
            list.add(m);
        }
        return list;
    }

    private String buildComment(Map<String, Integer> softScores, int edu, int intern, int prof,
                                int cert, int total) {
        Map<String, Object> scoreMap = new LinkedHashMap<>();
        scoreMap.put("education", edu);
        scoreMap.put("internship", intern);
        scoreMap.put("professional", prof);
        scoreMap.put("certificate", cert);
        scoreMap.put("communication", softScores.getOrDefault("communication", 60));
        scoreMap.put("teamwork", softScores.getOrDefault("teamwork", 60));
        scoreMap.put("problem_solving", softScores.getOrDefault("problem_solving", 60));
        scoreMap.put("innovation", softScores.getOrDefault("innovation", 60));
        scoreMap.put("learning", softScores.getOrDefault("learning", 60));
        scoreMap.put("pressure", softScores.getOrDefault("pressure", 60));
        List<Map<String, Object>> all = dimensionList(scoreMap);
        all.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
        String top = all.get(0).get("name") + "（" + all.get(0).get("score") + "）、"
                + all.get(1).get("name") + "（" + all.get(1).get("score") + "）";
        String low = all.get(all.size() - 2).get("name") + "（" + all.get(all.size() - 2).get("score") + "）、"
                + all.get(all.size() - 1).get("name") + "（" + all.get(all.size() - 1).get("score") + "）";
        return "综合得分 " + total + "。相对优势：" + top + "；待提升：" + low
                + "。建议优先补强待提升项，结合目标岗位要求制定 3 个月的针对性提升计划。";
    }

    private int rule(JsonNode ruleNode, String key, int def) {
        return ruleNode.path(key).asInt(def);
    }

    private String label(String type, String key) {
        if (key == null || key.isBlank() || "null".equals(key)) {
            return "未填写";
        }
        Map<String, String> edu = Map.of("high_school", "高中/中专", "college", "专科",
                "bachelor", "本科", "master", "硕士", "phd", "博士");
        Map<String, String> skill = Map.of("beginner", "入门", "basic", "了解",
                "proficient", "熟练", "expert", "精通");
        Map<String, String> months = Map.of("0", "无", "1_3", "1-3 个月",
                "4_6", "4-6 个月", "7_12", "7-12 个月", "12_plus", "12 个月以上");
        Map<String, String> cert = Map.of("0", "暂无", "1", "1 项", "2", "2 项", "3_plus", "3 项及以上");
        return switch (type) {
            case "education" -> edu.getOrDefault(key, key);
            case "skillLevel" -> skill.getOrDefault(key, key);
            case "internshipMonths" -> months.getOrDefault(key, key);
            case "certCount" -> cert.getOrDefault(key, key);
            default -> key;
        };
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private String defaultStr(Object o, String def) {
        String s = str(o);
        return s.isEmpty() ? def : s;
    }
}
