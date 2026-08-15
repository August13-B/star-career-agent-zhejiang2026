package wwy.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import wwy.example.springboot.entity.JobSkillRequirement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobSkillRequirementMapper extends BaseMapper<JobSkillRequirement> {
    // 可添加自定义查询方法
}