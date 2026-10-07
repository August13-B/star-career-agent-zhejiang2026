package org.example.web.service.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 读取「最近一次画像变更」，供两处消费：
 * <ul>
 *   <li>各操作页完成后的「画像已更新」提示（{@code GET /api/profile/change/latest}）；</li>
 *   <li>个人中心「我的核心能力模型」显示最近更新时间 / 来源 / 各维度变化。</li>
 * </ul>
 * 数据来自当前画像行（{@code score_type=1}）的 {@code change_source} / {@code change_detail}（见迁移 015）。
 */
@Service
@RequiredArgsConstructor
public class ProfileChangeService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public Map<String, Object> latest(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT change_source, change_detail, total_score, update_time FROM student_ability_score "
                        + "WHERE user_id=? AND score_type=1 AND is_deleted=0 "
                        + "ORDER BY update_time DESC, id DESC LIMIT 1", userId);
        if (rows.isEmpty()) {
            result.put("available", false);
            result.put("message", "尚无能力画像，请先完善基本情况");
            return result;
        }
        Map<String, Object> row = rows.get(0);
        String source = (String) row.get("change_source");
        result.put("available", true);
        result.put("source", source);
        result.put("sourceLabel", label(source));
        result.put("total", row.get("total_score"));
        result.put("updatedAt", row.get("update_time"));
        result.put("deltas", Map.of());
        String detail = (String) row.get("change_detail");
        if (detail != null && !detail.isBlank()) {
            try {
                JsonNode node = json.readTree(detail);
                result.put("firstTime", node.path("firstTime").asBoolean(false));
                result.put("reason", node.path("reason").asText(""));
                if (node.path("deltas").isObject()) {
                    result.put("deltas", json.convertValue(node.path("deltas"), Map.class));
                }
            } catch (Exception ignored) {
                // 明细损坏不影响主流程：只少一个"变化"展示
            }
        }
        return result;
    }

    /** 来源中文名（前端直接展示，避免各处重复映射）。 */
    public static String label(String source) {
        if (source == null || source.isBlank()) {
            return "初始画像";
        }
        return switch (source) {
            case ProfileScorePolicy.SOURCE_RESUME -> "简历解析";
            case ProfileScorePolicy.SOURCE_ASSESSMENT -> "能力补充测评";
            case ProfileScorePolicy.SOURCE_TRAINING -> "职场训练";
            case ProfileScorePolicy.SOURCE_REPORT -> "多智能体联合测评";
            case ProfileScorePolicy.SOURCE_BASELINE -> "初始画像";
            default -> source;
        };
    }
}
