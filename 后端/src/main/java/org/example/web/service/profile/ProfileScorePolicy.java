package org.example.web.service.profile;

/**
 * 画像更新幅度策略（唯一口径，供所有写入方复用）。
 *
 * <p>产品口径（B）：
 * <ul>
 *   <li><b>首次评估</b>（当前画像还没有被任何评估更新过）允许相对基线 {@value #BASELINE}
 *       大幅调整：±{@value #FIRST_RANGE} → 落在 {@code 25~95}；</li>
 *   <li><b>再次评估</b>：单维单次变化 ≤{@value #AGAIN_MAX_DELTA}，且<b>降低必须带理由</b>
 *       （无理由时保持原值，不算失败，只是不采纳这次下调）；</li>
 *   <li><b>简历解析加分</b>：只加不减，单项 ≤+{@value #RESUME_MAX_BONUS}，
 *       且只在首次评估或该维度此前为空时生效（避免反复传简历刷分）。</li>
 * </ul>
 *
 * <p>说明：本类只做"允许写多少"的裁剪，不做落库；落库由各写入方在同一事务内完成，
 * 以保持既有的行级锁 + 乐观并发（基线 id / 画像 version）保护不变。
 */
public final class ProfileScorePolicy {

    /** 画像基线（首次评估的锚点）。 */
    public static final int BASELINE = 60;
    /** 首次评估允许相对基线上下浮动的幅度。 */
    public static final int FIRST_RANGE = 35;
    /** 再次评估单维单次允许的变化幅度。 */
    public static final int AGAIN_MAX_DELTA = 10;
    /** 简历解析单项加分上限（只加不减）。 */
    public static final int RESUME_MAX_BONUS = 15;

    /** 变更来源常量（写入 student_ability_score.change_source）。 */
    public static final String SOURCE_BASELINE = "baseline";
    public static final String SOURCE_RESUME = "resume";
    public static final String SOURCE_ASSESSMENT = "assessment";
    public static final String SOURCE_TRAINING = "interview_training";
    public static final String SOURCE_REPORT = "report";

    private ProfileScorePolicy() {
    }

    /**
     * 是否属于「首次评估」：当前画像的 change_source 为空或仍是 baseline
     * （即从未被简历解析 / 测评 / 训练 / 多智能体报告更新过）。
     */
    public static boolean isFirstTime(String currentSource) {
        return currentSource == null || currentSource.isBlank() || SOURCE_BASELINE.equals(currentSource);
    }

    /**
     * 计算某维度最终可写入的分值。
     *
     * @param first    是否首次评估
     * @param before   当前分值（0~100）
     * @param proposed 评估给出的建议分值
     * @param reason   变化理由（再次评估时**降低**必填，否则不采纳下调）
     * @return 允许写入的分值
     */
    public static int clamp(boolean first, int before, int proposed, String reason) {
        int from = bound(before);
        int target = bound(proposed);
        if (first) {
            return Math.max(BASELINE - FIRST_RANGE, Math.min(BASELINE + FIRST_RANGE, target));
        }
        int delta = target - from;
        if (delta < 0 && (reason == null || reason.isBlank())) {
            return from;   // 降低必须有理由
        }
        return bound(from + Math.max(-AGAIN_MAX_DELTA, Math.min(AGAIN_MAX_DELTA, delta)));
    }

    /**
     * 简历解析加分：只加不减、单项 ≤+{@value #RESUME_MAX_BONUS}；
     * 仅在首次评估或该维度此前为空（{@code dimensionEmpty}）时生效。
     *
     * @param first          是否首次评估
     * @param dimensionEmpty 该维度对应的资料此前为空（简历新补上了内容）
     * @param before         当前分值
     * @param suggested      依简历建议的分值
     */
    public static int resumeBonus(boolean first, boolean dimensionEmpty, int before, int suggested) {
        int from = bound(before);
        if (!first && !dimensionEmpty) {
            return from;
        }
        return bound(Math.max(from, Math.min(bound(suggested), from + RESUME_MAX_BONUS)));
    }

    /** 总分口径（与现有写入方一致）：硬实力四项均值×30% + 软实力六维均值×70%。 */
    public static double total(int hardAverage, int softAverage) {
        return Math.round((hardAverage * 0.3 + softAverage * 0.7) * 10) / 10.0;
    }

    public static int bound(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
