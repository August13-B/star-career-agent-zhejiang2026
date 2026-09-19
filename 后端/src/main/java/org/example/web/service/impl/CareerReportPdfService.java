package org.example.web.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 职业报告 PDF 导出（Apache PDFBox 3.x，Apache-2.0）
 *
 * <p>中文字体自动探测：Windows(微软雅黑/宋体) → Linux(Noto CJK)；
 * 均未找到时退回内置拉丁字体（中文将无法显示，会在日志告警）。
 */
@Slf4j
@Service
public class CareerReportPdfService {

    private static final float MARGIN = 50f;
    private static final float TITLE_SIZE = 18f;
    private static final float HEAD_SIZE = 13f;
    private static final float BODY_SIZE = 10.5f;
    private static final float LEADING = 17f;
    /** 每行可容纳的"半角宽度单位"（全角算 2，半角算 1） */
    private static final int LINE_UNITS = 88;

    /** 优先 TTF（PDFBox 对 TTC 需额外 API，故先取单体字体） */
    private static final String[] CJK_FONT_CANDIDATES = {
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simkai.ttf",
            "C:/Windows/Fonts/simfang.ttf",
            "C:/Windows/Fonts/msyh.ttf",
            "C:/Windows/Fonts/simsun.ttf",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttf",
            "/usr/share/fonts/truetype/arphic/uming.ttf",
            "/System/Library/Fonts/Supplemental/Songti.ttc"
    };

    /**
     * 渲染报告 PDF
     *
     * @param reportName 报告名（标题）
     * @param agents     有序片段：[{name, content}]
     */
    public byte[] render(String reportName, List<Map<String, String>> agents) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDFont font = loadCjkFont(doc);
            Writer writer = new Writer(doc, font);

            writer.writeTitle(reportName == null ? "职业规划报告" : reportName);
            if (agents != null) {
                for (Map<String, String> a : agents) {
                    writer.writeHeading(a.getOrDefault("name", "报告章节"));
                    writer.writeBody(a.getOrDefault("content", ""));
                }
            }
            writer.close();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("生成报告 PDF 失败", e);
            throw new IllegalStateException("生成报告 PDF 失败: " + e.getMessage(), e);
        }
    }

    private PDFont loadCjkFont(PDDocument doc) {
        for (String path : CJK_FONT_CANDIDATES) {
            File f = new File(path);
            if (!f.exists()) {
                continue;
            }
            try {
                if (path.toLowerCase().endsWith(".ttc")) {
                    continue;   // TTC 暂不支持，跳过
                }
                return PDType0Font.load(doc, f);
            } catch (Exception e) {
                log.warn("加载中文字体失败 {}: {}", path, e.getMessage());
            }
        }
        log.warn("未找到中文字体，PDF 中文可能无法显示（请安装微软雅黑/宋体或 Noto CJK）");
        try {
            return new org.apache.pdfbox.pdmodel.font.PDType1Font(
                    org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
        } catch (Exception e) {
            throw new IllegalStateException("初始化 PDF 字体失败", e);
        }
    }

    /** 简单的流式排版器：自动换行 + 自动分页 */
    private static final class Writer {
        private final PDDocument doc;
        private final PDFont font;
        private PDPage page;
        private PDPageContentStream cs;
        private float y;

        Writer(PDDocument doc, PDFont font) throws Exception {
            this.doc = doc;
            this.font = font;
            newPage();
        }

        private void newPage() throws Exception {
            if (cs != null) {
                cs.endText();
                cs.close();
            }
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            cs.beginText();
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        private void ensure(float need) throws Exception {
            if (y - need < MARGIN) {
                newPage();
            }
        }

        private void line(String text, float size) throws Exception {
            ensure(LEADING);
            cs.setFont(font, size);
            cs.newLineAtOffset(0, -LEADING);
            cs.showText(text == null ? "" : text);
            y -= LEADING;
        }

        void writeTitle(String text) throws Exception {
            ensure(LEADING * 1.5f);
            cs.setFont(font, TITLE_SIZE);
            cs.newLineAtOffset(0, -LEADING * 1.5f);
            cs.showText(text);
            y -= LEADING * 1.5f;
            blank(0.5f);
        }

        void writeHeading(String text) throws Exception {
            blank(0.4f);
            line("【" + text + "】", HEAD_SIZE);
        }

        void writeBody(String markdown) throws Exception {
            if (markdown == null || markdown.isBlank()) {
                return;
            }
            for (String raw : markdown.split("\n")) {
                String line = clean(raw);
                if (line.isEmpty()) {
                    blank(0.5f);
                    continue;
                }
                for (String seg : wrap(line, LINE_UNITS)) {
                    line(seg, BODY_SIZE);
                }
            }
        }

        private void blank(float factor) throws Exception {
            ensure(LEADING * factor);
            cs.setFont(font, BODY_SIZE);
            cs.newLineAtOffset(0, -LEADING * factor);
            y -= LEADING * factor;
        }

        /** 去掉 Markdown 记号，保留可读文本 */
        private String clean(String s) {
            String t = s.strip();
            t = t.replaceAll("^#{1,6}\\s*", "");
            t = t.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
            t = t.replaceAll("^[-*]\\s+", "· ");
            t = t.replaceAll("^\\d+\\.\\s+", "");
            t = t.replace("`", "");
            t = t.replaceAll("\\[(.+?)]\\((.+?)\\)", "$1");
            return t.strip();
        }

        /** 按"半角宽度单位"折行（中文算 2，其余算 1） */
        private java.util.List<String> wrap(String text, int units) {
            java.util.List<String> out = new java.util.ArrayList<>();
            StringBuilder cur = new StringBuilder();
            int w = 0;
            for (char c : text.toCharArray()) {
                int cw = (c >= 0x2E80) ? 2 : 1;
                if (w + cw > units) {
                    out.add(cur.toString());
                    cur.setLength(0);
                    w = 0;
                }
                cur.append(c);
                w += cw;
            }
            if (cur.length() > 0) {
                out.add(cur.toString());
            }
            return out;
        }

        void close() throws Exception {
            if (cs != null) {
                cs.endText();
                cs.close();
                cs = null;
            }
        }
    }
}
