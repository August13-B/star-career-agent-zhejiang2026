package wwy.example.springboot.dto;

import lombok.Data;
import wwy.example.springboot.entity.JobRequirementProfile;

@Data
public class JobProfileSimpleVO {
    private JobRequirementProfile jobRequirementProfile;
    // 不加其他字段
}