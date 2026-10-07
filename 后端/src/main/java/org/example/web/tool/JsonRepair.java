package org.example.web.tool;

/**
 * 轻量 JSON 修复：模型常把评语里的强调词加上英文双引号（例如 {@code ...风险。"缺证据"集中在：...}），
 * 导致整个 JSON 字符串提前闭合、严格解析失败（表现为「评分不是有效的结构化结果」→ 待复核）。
 *
 * <p>策略：仅在**严格解析失败后**作为兜底使用。逐字符扫描，遇到字符串内部的裸引号时判断其后
 * （跳过空白）是否紧跟 `,` `}` `]` `:` 或输入结束——不是则视为裸引号并转义。
 *
 * <p>注意：这是启发式修复，无法覆盖所有歧义（例如裸引号后紧跟逗号）；修复后仍可能解析失败，
 * 调用方需保留原报错与原文日志以便排查。
 */
public final class JsonRepair {
    private JsonRepair() {
    }

    public static String repairUnescapedQuotes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length() + 32);
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!inString) {
                out.append(c);
                if (c == '"') {
                    inString = true;
                }
                continue;
            }
            if (c == '\\') {
                out.append(c);
                if (i + 1 < text.length()) {
                    out.append(text.charAt(++i));
                }
                continue;
            }
            if (c == '"') {
                if (isTerminator(text, i + 1)) {
                    out.append(c);
                    inString = false;
                } else {
                    out.append("\\\"");
                }
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static boolean isTerminator(String text, int from) {
        int i = from;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        if (i >= text.length()) {
            return true;
        }
        char next = text.charAt(i);
        return next == ',' || next == '}' || next == ']' || next == ':';
    }
}
