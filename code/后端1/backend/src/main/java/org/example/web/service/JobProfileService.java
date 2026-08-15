package org.example.web.service;

import org.example.web.entity.JobInfo;
import org.example.web.entity.Result;

/**
 * 岗位画像服务接口
 * 定义岗位画像相关的业务方法
 * 注意：当前版本只实现添加岗位信息方法，其他方法已删除
 * 
 * @author 系统生成
 * @version 1.0
 */
public interface JobProfileService {
    
    /**
     * 添加岗位信息
     * 接收JobInfo对象，调用Mapper层方法插入数据库
     * 
     * @param jobInfo 岗位信息实体
     * @return 添加成功返回true，失败返回false
     */
    boolean addJobInfo(JobInfo jobInfo);

    Result createJobProfile(String user_id);
}
