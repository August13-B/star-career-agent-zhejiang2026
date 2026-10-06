package org.example.web.service.training;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 意向文本 → 搜索关键词：切分、去噪、去重、限长限数（纯函数，不依赖数据库）。 */
class TrainingJobCatalogTest {

    @Test void splitsIntentByChineseAndAsciiPunctuation() {
        assertEquals(List.of("产品运营", "数据分析"), TrainingJobCatalog.keywords("产品运营、数据分析"));
        assertEquals(List.of("前端开发", "Java"), TrainingJobCatalog.keywords("前端开发, Java"));
        assertEquals(List.of("算法工程师", "测试开发"), TrainingJobCatalog.keywords("算法工程师；测试开发"));
        assertEquals(List.of("运维工程师", "网络安全"), TrainingJobCatalog.keywords("运维工程师 / 网络安全"));
    }

    @Test void stripsIntentNoiseAndSurroundingPunctuation() {
        assertEquals(List.of("Java后端开发"), TrainingJobCatalog.keywords("意向：Java后端开发。"));
        assertEquals(List.of("人力资源"), TrainingJobCatalog.keywords("期望：人力资源、"));
    }

    @Test void dropsTooShortTooLongAndDuplicates() {
        assertEquals(List.of("数据分析"), TrainingJobCatalog.keywords("a、测、数据分析、数据分析"));
        assertEquals(List.of(), TrainingJobCatalog.keywords("这是一个超过二十个字符长度的非常长的职业意向名称用来测试上限"));
    }

    @Test void capsKeywordCount() {
        List<String> words = TrainingJobCatalog.keywords("一职业、二职业、三职业、四职业、五职业、六职业、七职业");
        assertEquals(6, words.size(), "最多取 6 个关键词");
        assertEquals("一职业", words.get(0));
    }

    @Test void expandsLongHanWordBySlidingWindows() {
        List<String> probes = TrainingJobCatalog.expand("全栈开发工程师");
        assertEquals("全栈开发工程师", probes.get(0), "整词优先");
        assertTrue(probes.contains("开发工程师"), "应包含 5 字子串");
        assertTrue(probes.contains("工程师"), "应到最后退到 3 字子串");
        assertEquals(probes.size(), probes.stream().distinct().count(), "不应重复");
    }

    @Test void keepsAsciiAndShortWordsIntact() {
        assertEquals(List.of("Java"), TrainingJobCatalog.expand("Java"));
        assertEquals(List.of("数据分析"), TrainingJobCatalog.expand("数据分析"), "不足 5 字不拆");
        assertEquals(List.of(), TrainingJobCatalog.expand(null));
        assertEquals(List.of(), TrainingJobCatalog.expand("  "));
    }

    @Test void blankIntentYieldsNoKeywords() {
        assertEquals(List.of(), TrainingJobCatalog.keywords(null));
        assertEquals(List.of(), TrainingJobCatalog.keywords("   "));
        assertEquals(List.of(), TrainingJobCatalog.keywords("、，；"));
    }
}
