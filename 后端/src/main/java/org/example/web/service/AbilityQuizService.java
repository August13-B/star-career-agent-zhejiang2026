package org.example.web.service;

import java.util.Map;

/**
 * 六维能力初步测评（题库在 `resources/data/ability-questions.json`，不入库）
 *
 * <p>流程：第 1 步填基本情况（结构化）→ 第 2 步按维度随机抽题（约 10 题）→ 后端评分 →
 * 写 `student_ability`（硬实力文本）+ `student_ability_score`（10 维分 + total，score_type=1）。
 * 之后「AI 能力测评」参考该初步分并覆盖。
 */
public interface AbilityQuizService {

    /** 抽题：每维随机 1~2 道（合计约 10 题），只返回题干与打乱后的选项（不含分值） */
    Map<String, Object> drawQuiz();

    /**
     * 提交作答并评分落库。
     *
     * @param userId 当前用户ID
     * @param body   含 {@code basic}（基本情况结构化）与 {@code answers}（[{id,k}]）
     * @return 10 维分数 + total + 评语
     */
    Map<String, Object> submit(Long userId, Map<String, Object> body);

    /**
     * 只保存「基本情况」（硬实力四项：学历/实习/专业技能/证书）——个人中心不再做软实力测评。
     *
     * <p>按规则表换算硬实力四维，**保留已有软实力六维**（无则默认 60），重算总分；
     * 原始选项写入 {@code student_ability.basic_options} 以便复算与回显。
     */
    java.util.Map<String, Object> saveBasic(Long userId, java.util.Map<String, Object> basic);
}
