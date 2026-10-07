package org.example.web.service.profile;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 简历解析：文本提取（PDFBox 往返）+ 资料合并规则（不覆盖/去重追加）。 */
class ResumeParseTest {

    private final ResumePdfService pdf = new ResumePdfService();

    @Test void extractsTextFromRealPdf() throws Exception {
        byte[] bytes = pdfWithText("Resume of Alex Chen\nSkills: Java, Vue, MySQL\nCert: CET-6\n"
                + "Intern: ByteDance backend intern 2025\nProject: campus job platform");
        String text = pdf.extractText(bytes);
        assertTrue(text.contains("Alex Chen"), text);
        assertTrue(text.contains("ByteDance"), text);
    }

    @Test void rejectsPdfWithoutTextLayer() throws Exception {
        // 只画一个矩形、不写文字 → 模拟扫描件（无文字层）
        byte[] bytes = pdfWithoutText();
        ResumeParseException e = assertThrows(ResumeParseException.class, () -> pdf.extractText(bytes));
        assertTrue(e.getMessage().contains("扫描件"), e.getMessage());
    }

    @Test void rejectsBrokenPdf() {
        assertThrows(ResumeParseException.class, () -> pdf.extractText(new byte[]{'%', 'P', 'D', 'F', 'x'}));
        assertThrows(ResumeParseException.class, () -> pdf.extractText(new byte[0]));
    }

    @Test void normalizeKeepsParagraphsAndDropsNoise() {
        String raw = "姓名：张三\r\n\r\n\r\n  技能：Java \u0000 \t Vue  \r\n\r\n项目：A";
        String text = ResumePdfService.normalize(raw);
        assertTrue(text.contains("技能：Java Vue"), text);
        assertTrue(text.contains("\n\n项目：A"), text);
        assertFalse(text.contains("\u0000"));
        assertFalse(text.contains("\r"));
    }

    @Test void mergeNeverOverwritesFilledFields() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("education", "本科");
        profile.put("major", "");
        Map<String, Object> updates = new LinkedHashMap<>();
        ResumeOutcomeService.fillIfBlank(updates, profile, "education", "硕士");
        ResumeOutcomeService.fillIfBlank(updates, profile, "major", "计算机科学与技术");
        assertEquals(Map.of("major", "计算机科学与技术"), updates);
    }

    @Test void mergeAppendsListsWithoutDuplicates() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("skill", "Java、Vue");
        profile.put("certificate", null);
        Map<String, Object> updates = new LinkedHashMap<>();
        ResumeOutcomeService.appendList(updates, profile, "skill", List.of("Vue", "MySQL", "  ", "Java"));
        ResumeOutcomeService.appendList(updates, profile, "certificate", List.of("CET-6"));
        assertEquals("Java、Vue、MySQL", updates.get("skill"));
        assertEquals("CET-6", updates.get("certificate"));
    }

    @Test void mergeAppendsParagraphsOnlyWhenNew() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("work_experience", "字节跳动 后端实习 2025");
        Map<String, Object> updates = new LinkedHashMap<>();
        ResumeOutcomeService.appendParagraph(updates, profile, "work_experience",
                List.of("字节跳动 后端实习 2025", "腾讯 前端实习 2024"));
        assertEquals("字节跳动 后端实习 2025\n腾讯 前端实习 2024", updates.get("work_experience"));

        Map<String, Object> same = new LinkedHashMap<>();
        ResumeOutcomeService.appendParagraph(same, profile, "work_experience", List.of("字节跳动 后端实习 2025"));
        assertTrue(same.isEmpty(), "完全重复时不产生更新");
    }

    @Test void splitsExistingItemsByCommonSeparators() {
        assertEquals(List.of("Java", "Vue", "MySQL"), ResumeOutcomeService.split("Java、Vue,MySQL"));
        assertEquals(List.of("A", "B"), ResumeOutcomeService.split("A；B"));
        assertEquals(List.of(), ResumeOutcomeService.split("   "));
    }

    // ── 测试用 PDF 构造 ────────────────────────────────────────────────

    private static byte[] pdfWithText(String content) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                stream.newLineAtOffset(40, 760);
                for (String line : content.split("\n")) {
                    stream.showText(line);
                    stream.newLineAtOffset(0, -16);
                }
                stream.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] pdfWithoutText() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.addRect(40, 700, 200, 60);
                stream.fill();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
