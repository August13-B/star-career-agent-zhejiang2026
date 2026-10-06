package org.example.web.service.training;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/** Stable references to exact submitted text; the model selects references instead of retyping IDs/quotes. */
public final class TrainingEvidenceCatalog {
    private TrainingEvidenceCatalog() {}
    public record Source(String sourceType, String sourceId, String field, String content) {}
    public record Excerpt(String sourceType, String sourceId, String field, String quote) {}

    public static Map<String, Excerpt> from(Map<String, String> answers) {
        return fromSources(answers.entrySet().stream().map(e -> new Source("turn", e.getKey(), "", e.getValue())).toList());
    }

    public static Map<String, Excerpt> fromSources(List<Source> sources) {
        Map<String, Excerpt> result = new LinkedHashMap<>();
        int answerNumber = 0;
        for (var answer : sources) {
            answerNumber++;
            int excerptNumber = 0;
            for (String sentence : answer.content().split("(?<=[。！？；\\n])")) {
                for (int start = 0; start < sentence.length();) {
                    int end = Math.min(start + 350, sentence.length());
                    if (end < sentence.length() && Character.isHighSurrogate(sentence.charAt(end - 1))) end--;
                    String quote = sentence.substring(start, end).strip();
                    if (!quote.isBlank()) result.put("A" + answerNumber + "E" + (++excerptNumber), new Excerpt(answer.sourceType(), answer.sourceId(), answer.field(), quote));
                    start = end;
                }
            }
        }
        return result;
    }
}
