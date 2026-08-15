package wwy.example.springboot.dto;

import lombok.Data;
import java.util.List;

@Data
public class JobCompareResult {
    private Long newJobId;
    private String analysis;
    private List<MatchedJob> matchedJobs;

    @Data
    public static class MatchedJob {
        private Long profileId;
        private String positionName;
        private Double similarity;
    }
}