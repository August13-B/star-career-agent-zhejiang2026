package wwy.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import wwy.example.springboot.entity.JobHardRequirement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobHardRequirementMapper extends BaseMapper<JobHardRequirement> {
    // 可添加自定义方法
}