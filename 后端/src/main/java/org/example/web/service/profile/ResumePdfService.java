package org.example.web.service.profile;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * 简历 PDF 文本提取（Apache PDFBox 3.x，Apache-2.0；同依赖已用于职业报告 PDF 导出）。
 *
 * <p>只负责"文件 → 纯文本"，不做任何业务判断。**图片型（扫描件）PDF 没有文字层**，
 * 提取结果为空时应明确报错，而不是把空文本送给模型。
 */
@Component
public class ResumePdfService {

    /** 送入模型的文本上限（正常简历远小于此，防止异常文件拖垮 prompt）。 */
    static final int MAX_CHARS = 20000;
    /** 允许的最大页数（超出视为异常文件）。 */
    static final int MAX_PAGES = 20;
    /** 有效文字的最小长度：低于此值几乎必然是扫描件。 */
    static final int MIN_CHARS = 40;

    public String extractText(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ResumeParseException("文件内容为空，请重新选择简历");
        }
        String text;
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.getNumberOfPages() > MAX_PAGES) {
                throw new ResumeParseException("PDF 页数过多（超过 " + MAX_PAGES + " 页），请上传单份简历");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            text = stripper.getText(document);
        } catch (ResumeParseException e) {
            throw e;
        } catch (IOException e) {
            throw new ResumeParseException("PDF 读取失败，请确认文件未加密、未损坏后重试");
        }
        String normalized = normalize(text);
        if (normalized.length() < MIN_CHARS) {
            throw new ResumeParseException("这份 PDF 里没有可提取的文字（可能是扫描件或纯图片），"
                    + "请上传文字版 PDF，或直接在个人中心手动填写");
        }
        return normalized.length() > MAX_CHARS ? normalized.substring(0, MAX_CHARS) : normalized;
    }

    /** 归一化：统一换行、压掉多余空白与不可见字符，保留段落结构（供模型阅读）。 */
    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        text = text.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]", "");
        text = text.replaceAll("[\\t\\u00A0\\u3000 ]+", " ");
        text = text.replaceAll("(?m)^[ ]+", "");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.strip();
    }
}
