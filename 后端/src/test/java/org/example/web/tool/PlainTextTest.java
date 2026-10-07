package org.example.web.tool;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 转义换行还原：一层 / 两层都要还原，且不能破坏路径、正则、C# 等未知转义。 */
class PlainTextTest {

    @Test void unescapesSingleLayer() {
        assertEquals("openai\n\n### 一、说明", PlainText.unescapeEscapes("openai\\n\\n### 一、说明"));
        assertEquals("a\nb", PlainText.unescapeEscapes("a\\nb"));
    }

    @Test void unescapesDoubleLayer() {
        // 平台偶尔下发两层转义：\\n（源码写法 \\\\n）也要还原成真实换行
        assertEquals("行情。\n\n### 一、你的评分说明什么", PlainText.unescapeEscapes("行情。\\\\n\\\\n### 一、你的评分说明什么"));
    }

    @Test void treatsCrlfAsOneNewline() {
        assertEquals("a\nb", PlainText.unescapeEscapes("a\\r\\nb"));
        assertEquals("列1\t列2", PlainText.unescapeEscapes("列1\\t列2"));
        assertEquals("引用 \"x\"", PlainText.unescapeEscapes("引用 \\\"x\\\""));
    }

    @Test void keepsUnknownEscapesAndPlainBackslashes() {
        assertEquals("C:\\Users\\me", PlainText.unescapeEscapes("C:\\Users\\me"));
        assertEquals("正则 \\d+ 与 \\w", PlainText.unescapeEscapes("正则 \\d+ 与 \\w"));
        assertEquals("没有转义", PlainText.unescapeEscapes("没有转义"));
    }

    @Test void nullSafe() {
        assertNull(PlainText.unescapeEscapes(null));
        assertEquals("", PlainText.unescapeEscapes(""));
    }
}
