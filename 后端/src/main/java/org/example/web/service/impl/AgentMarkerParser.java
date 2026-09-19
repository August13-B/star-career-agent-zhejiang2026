package org.example.web.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多智能体段标记增量解析器
 *
 * <p>平台输出形如（同一流式正文中，逐字到达）：
 * <pre>
 * &lt;&lt;&lt;AGENT:profile_analysis&gt;&gt;&gt;
 * ### 一、能力画像分析
 * ...
 * &lt;&lt;&lt;END:profile_analysis&gt;&gt;&gt;
 * </pre>
 *
 * <p>由于 delta 可能是单字符，解析器需保留尾部若干字符以识别跨 chunk 的标记。
 * 另外：**标记前的文本视为噪声**（实测平台会输出英文开场白），除非整段都没有标记（兜底）。
 */
public class AgentMarkerParser {

    public static final String START = "<<<AGENT:";
    public static final String END = "<<<END:";
    private static final String CLOSE = ">>>";

    /** 无标记时的兜底智能体 key */
    public static final String FALLBACK_AGENT = "report_composition";

    /** 尾部保留长度（要够容纳最长标记：<<<END:report_composition>>>） */
    private static final int HOLD = 48;

    /** 标记之前的文本（噪声，或整段无标记时的兜底内容） */
    private final StringBuilder preText = new StringBuilder();
    /** 当前智能体片段缓冲 */
    private final StringBuilder pending = new StringBuilder();

    private String currentAgent = null;
    private boolean anyMarker = false;
    private final Map<String, StringBuilder> sections = new LinkedHashMap<>();

    /** 下发的增量片段 */
    public record Chunk(String agent, String text) {
    }

    /** 输入一个增量，返回可下发的片段（可能为空列表） */
    public List<Chunk> feed(String delta) {
        if (delta == null || delta.isEmpty()) {
            return List.of();
        }
        if (currentAgent == null && !anyMarker) {
            preText.append(delta);
            // 标记前文本不回吐：只有出现标记后才开始切分（避免噪声外泄）
        } else if (currentAgent == null) {
            // 已出现过标记：等待下一个标记，其间文本丢弃
        } else {
            pending.append(delta);
        }
        return drain(false);
    }

    /** 流结束：冲刷剩余内容 */
    public List<Chunk> finish() {
        List<Chunk> out = drain(true);
        if (currentAgent != null && pending.length() > 0) {
            out.add(commit(currentAgent, pending.toString()));
            pending.setLength(0);
            currentAgent = null;
        }
        if (!anyMarker && preText.length() > 0) {
            out.add(commit(FALLBACK_AGENT, preText.toString()));
            preText.setLength(0);
        }
        return out;
    }

    /** 尝试解析缓冲中的标记与文本 */
    private List<Chunk> drain(boolean finishing) {
        List<Chunk> out = new ArrayList<>();
        boolean progress = true;
        while (progress) {
            progress = false;

            if (currentAgent == null) {
                // 在 preText 中寻找起始标记
                int i = preText.indexOf(START);
                if (i >= 0) {
                    int end = preText.indexOf(CLOSE, i);
                    if (end > 0) {
                        // 标记后的内容可能与本标记同处一个 delta → 转入片段缓冲
                        String after = preText.substring(end + CLOSE.length());
                        currentAgent = textBetween(i, end);
                        anyMarker = true;
                        preText.setLength(0);
                        pending.setLength(0);
                        if (!after.isEmpty()) {
                            pending.append(after);
                        }
                        progress = true;
                    } else if (finishing) {
                        break;
                    }
                }
            } else {
                String endMark = END + currentAgent + CLOSE;
                int i = pending.indexOf(endMark);
                if (i >= 0) {
                    if (i > 0) {
                        out.add(commit(currentAgent, pending.substring(0, i)));
                    }
                    pending.delete(0, i + endMark.length());
                    currentAgent = null;
                    progress = true;
                } else {
                    // 下发安全部分（保留尾部，防止标记被截断）
                    int safe = pending.length() - Math.max(HOLD, endMark.length());
                    if (safe > 0) {
                        out.add(commit(currentAgent, pending.substring(0, safe)));
                        pending.delete(0, safe);
                    }
                    if (finishing) {
                        break;
                    }
                }
            }
        }
        return out;
    }

    private String textBetween(int startIdx, int endIdx) {
        return preText.substring(startIdx + START.length(), endIdx);
    }

    private Chunk commit(String agent, String text) {
        sections.computeIfAbsent(agent, k -> new StringBuilder()).append(text);
        return new Chunk(agent, text);
    }

    /** 是否出现过标记（用于兜底判定与告警） */
    public boolean hasMarkers() {
        return anyMarker;
    }

    /** 已归集的各智能体完整内容（key → 文本，保持出现顺序） */
    public Map<String, String> sections() {
        Map<String, String> m = new LinkedHashMap<>();
        sections.forEach((k, v) -> m.put(k, v.toString()));
        return m;
    }
}
