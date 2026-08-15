package wwy.example.springboot.service;

public interface JobAIAnalysisService {

    /**
     * 分析指定岗位并保存分析结果到各子表
     * @param jobInfoId job_info 表的主键ID
     */
    void analyzeAndSave(Long jobInfoId);
}