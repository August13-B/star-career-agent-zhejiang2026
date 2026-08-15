package wwy.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import wwy.example.springboot.entity.JobTransferGraph;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobTransferGraphMapper extends BaseMapper<JobTransferGraph> {
}