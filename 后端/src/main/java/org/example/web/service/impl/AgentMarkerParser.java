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
 * &lt;&lt;&lt;AGENT:career_exploration&gt;&gt;&gt;
 * ...
 * </pre>
 *
 * <p>由于 delta 可能是单字符，解析器需保留尾部若干字符以识别跨 chunk 的标记。
 * 另外：**标记前的文本视为噪声**（实测平台会输出英文开场白），除非整段都没有标记（兜底）。
 *
 * <p>容错：
 * <ul>
 *   <li>缺少 {@code <<<END:x>>>} 时，下一个 {@code <<<AGENT:} 视为上一段结束</li>
 *   <li>同一 delta 内包含「结束标记 + 下一段起始标记」时，剩余内容会转入下一段解析</li>
 * </ul>
 */
public class AgentMarkerParser {

    public static final String START = "<<<AGENT:";
    public static final String END = "<<<END:";
    private static final String CLOSE = ">>>";

    /** 无标记时的兜底智能体 key */
    public static final String FALLBACK_AGENT = "report_composition";

    /** 尾部保留长度（要够容纳最长标记：<<<END:report_composition>>>） */
    private static final int HOLD = 48;

    /** 待解析文本：片段之前的噪声，或两个片段之间的过渡文本（不会下发） */
    private final StringBuilder preText = new StringBuilder();
    /** 当前智能体片段缓冲（会按安全长度增量下发） */
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
        if (currentAgent == null) {
            // 未处于某个片段内：一律进 preText，等待（下一个）起始标记。
            // 注意：不能在出现首个标记后就丢弃——否则第 1 段之后的 <<<AGENT:>>> 永远扫不到，
            //      表现就是「报告只有第一段」。
            preText.append(delta);
        } else {
            pending.append(delta);
        }
        return drain(false);
    }

    /** 流结束：冲刷剩余内容 */
    public List<Chunk> finish() {
        List<Chunk> out = drain(true);
        if (currentAgent != null && pending.length() > 0) {
            // 末尾段缺少 <<<END>>>：整段归入当前智能体
            out.add(commit(currentAgent, pending.toString()));
            pending.setLength(0);
            currentAgent = null;
        }
        if (!anyMarker && preText.length() > 0) {
            // 全程无标记：整段兜底归入 report_composition
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
                        String key = preText.substring(i + START.length(), end);
                        String after = preText.substring(end + CLOSE.length());
                        currentAgent = key;
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
                } else if (anyMarker && preText.length() > HOLD) {
                    // 两段之间的噪声：只保留尾部（识别跨 delta 的标记），避免缓冲无限增长
                    preText.delete(0, preText.length() - HOLD);
                }
            } else {
                String endMark = END + currentAgent + CLOSE;
                int iEnd = pending.indexOf(endMark);
                int iStart = pending.indexOf(START);
                if (iEnd >= 0 && (iStart < 0 || iEnd <= iStart)) {
                    // 正常结束：下发 END 标记之前的内容
                    if (iEnd > 0) {
                        out.add(commit(currentAgent, pending.substring(0, iEnd)));
                    }
                    // END 之后可能紧跟下一段的起始标记，转入 preText 继续解析
                    String rest = pending.substring(iEnd + endMark.length());
                    pending.setLength(0);
                    if (!rest.isEmpty()) {
                        preText.append(rest);
                    }
                    currentAgent = null;
                    progress = true;
                } else if (iStart >= 0) {
                    // 容错：缺少 END 标记时，下一个起始标记视为本段结束
                    if (iStart > 0) {
                        out.add(commit(currentAgent, pending.substring(0, iStart)));
                    }
                    preText.append(pending.substring(iStart));
                    pending.setLength(0);
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
