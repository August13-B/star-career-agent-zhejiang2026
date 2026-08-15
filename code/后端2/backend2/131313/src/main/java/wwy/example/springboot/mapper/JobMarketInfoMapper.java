package wwy.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import wwy.example.springboot.entity.JobMarketInfo;

@Mapper
public interface JobMarketInfoMapper extends BaseMapper<JobMarketInfo> {
}
