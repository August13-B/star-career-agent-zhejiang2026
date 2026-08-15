package wwy.example.springboot.dto;

import lombok.Data;
import wwy.example.springboot.entity.*;
import java.util.List;

@Data
public class JobDetailVO {
    private JobRequirementProfile jobRequirementProfile;
    private List<JobInfo> jobInfoList;          // 改为 List，支持多个岗位
    private JobHardRequirement hardRequirement;
    private JobSkillRequirement skillRequirement;
    private JobSoftRequirement softRequirement;
    private JobMarketInfo marketInfo;
    private List<JobPromotionGraph> promotionGraphs;
    private List<JobTransferGraph> transferGraphs;
}