package wwy.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import wwy.example.springboot.entity.JobSoftRequirement;

@Mapper
public interface JobSoftRequirementMapper extends BaseMapper<JobSoftRequirement> {
}
