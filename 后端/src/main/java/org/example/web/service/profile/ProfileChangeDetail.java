package org.example.web.service.profile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 画像变更明细的统一构造（写入 {@code student_ability_score.change_detail}）。
 *
 * <p>结构：
 * <pre>
 * {
 *   "firstTime": false,
 *   "source": "assessment",
 *   "reason": "能力补充测评更新（软实力六维）",
 *   "deltas": { "communication": { "before": 62, "after": 70 } }
 * }
 * </pre>
 *
 * <p>所有画像写入方（简历解析 / 能力补充测评 / 职场训练 / 多智能体报告）共用本类，
 * 保证前端「画像已更新」提示与个人中心「最近变化」展示的字段一致。
 */
public final class ProfileChangeDetail {

    private ProfileChangeDetail() {
    }

    public static Map<String, Object> of(boolean firstTime, String source, String reason,
                                         Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("firstTime", firstTime);
        detail.put("source", source);
        detail.put("reason", reason);
        detail.put("deltas", deltas(before, after));
        return detail;
    }

    /** 仅保留发生变化的维度：{dimension: {before, after}}。 */
    public static Map<String, Object> deltas(Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> deltas = new LinkedHashMap<>();
        after.forEach((dimension, value) -> {
            if ("total".equals(dimension) || !before.containsKey(dimension)) {
                return;
            }
            int from = number(before.get(dimension));
            int to = number(value);
            if (from != to) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("before", from);
                item.put("after", to);
                deltas.put(dimension, item);
            }
        });
        return deltas;
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : -1;
    }
}
