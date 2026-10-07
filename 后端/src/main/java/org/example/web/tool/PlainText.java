package org.example.web.tool;

/**
 * 纯文本清理工具。
 *
 * <p>背景：平台（百宝箱）下发的正文里，换行有时是**转义后的字面量** —— 前端收到的是
 * 两个字符 `\` + `n`，于是聊天窗口里直接显示「……行情。\n\n### 一、……」而不是换行，
 * Markdown 结构也就全乱了（标题、列表挤在一行）。
 *
 * <p>本类把这类转义还原成真实字符：`\n` / `\r\n` / `\r` → 换行，`\t` → 制表符，
 * `\"` → 双引号，`\\` → 反斜杠。**其它未知转义原样保留**，因此 C# 的 `\d`、正则 `\w`、
 * Windows 路径 `C:\Users` 都不会被破坏。
 *
 * <p>平台偶尔会下发**两层**转义（`\\n`），所以这里按轮反复还原直到稳定（上限 {@value #MAX_PASSES} 轮）。
 *
 * <p>只在**面向用户的文本出口**使用（如聊天流的分块）。**不要**用于 JSON 解析前的内容 ——
 * JSON 字符串里的 `\n` 是合法转义，提前还原会让 JSON 变成非法格式。
 */
public final class PlainText {

    private static final int MAX_PASSES = 3;

    private PlainText() {
    }

    /** 反复还原直到稳定；无转义或还原失败时返回原文。 */
    public static String unescapeEscapes(String text) {
        if (text == null || text.indexOf('\\') < 0) {
            return text;
        }
        String current = text;
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            String next = unescapeOnce(current);
            if (next.equals(current)) {
                return next;
            }
            current = next;
        }
        return current;
    }

    /** 单轮还原。 */
    static String unescapeOnce(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch != '\\' || i + 1 >= text.length()) {
                out.append(ch);
                continue;
            }
            char next = text.charAt(++i);
            switch (next) {
                case 'n', 'r' -> {
                    // \r\n 视作一个换行，避免多出一个空行
                    if (next == 'r' && i + 2 < text.length()
                            && text.charAt(i + 1) == '\\' && text.charAt(i + 2) == 'n') {
                        i += 2;
                    }
                    out.append('\n');
                }
                case 't' -> out.append('\t');
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                default -> out.append('\\').append(next);
            }
        }
        return out.toString();
    }
}
