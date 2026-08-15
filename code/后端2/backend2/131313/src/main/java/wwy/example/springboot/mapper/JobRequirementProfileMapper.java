package wwy.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import wwy.example.springboot.entity.JobRequirementProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobRequirementProfileMapper extends BaseMapper<JobRequirementProfile> {
    // 可添加自定义查询方法
}