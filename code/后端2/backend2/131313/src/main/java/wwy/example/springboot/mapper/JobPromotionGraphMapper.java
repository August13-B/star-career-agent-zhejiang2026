package wwy.example.springboot.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import wwy.example.springboot.entity.JobPromotionGraph;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobPromotionGraphMapper extends BaseMapper<JobPromotionGraph> {
    // 可以添加自定义SQL方法
}