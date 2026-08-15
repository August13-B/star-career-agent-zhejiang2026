package wwy.example.springboot.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiJobAnalysisResult {
    private String educationRequirement;
    private String internshipRequirement;
    private List<String> professionalSkills;
    private List<String> certificateRequirement;
    private String innovationAbility;
    private String learningAbility;
    private String pressureResistance;
    private String communicationAbility;
    private String problemSolving;
    private String teamworkAbility;
    private List<PromotionJob> promotions;
    private List<TransferJob> transfers;

    @Data
    public static class PromotionJob {
        private String desc;
        private String skillDiff;
        private String experience;
        private Integer learningCycle;
    }

    @Data
    public static class TransferJob {
        private String desc;
        private String skillDiff;
        private String education;
        private String experience;
        private Integer learningCycle;
        private Integer difficulty;
    }
}