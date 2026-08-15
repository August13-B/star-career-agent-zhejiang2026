package wwy.example.springboot.dto;

import lombok.Data;
import java.util.List;

@Data
public class GraphVO {
    private CenterNode center;
    private List<PromotionNode> promotions;
    private List<TransferNode> transfers;

    @Data
    public static class CenterNode {
        private String name;
        private String category;
    }

    @Data
    public static class PromotionNode {
        private String name;
        private String skillDiff;
        private String experience;
        private Integer learningCycle;
    }

    @Data
    public static class TransferNode {
        private String name;
        private String skillDiff;
        private String education;
        private String experience;
        private Integer learningCycle;
        private Integer difficulty;
    }
}