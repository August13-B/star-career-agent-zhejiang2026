package wwy.example.springboot.service;

public interface JobAIAnalysisService {

    /**
     * 分析指定岗位并保存分析结果到各子表
     * @param jobInfoId job_info 表的主键ID
     */
    void analyzeAndSave(Long jobInfoId);

    /**
     * 按「岗位画像」补全十维要求（学历/实习/技能/证书 + 六项软实力）。
     *
     * <p>与 {@link #analyzeAndSave(Long)} 的区别：入参是 {@code job_requirement_profile.id}，
     * 用画像自身（岗位名/行业/描述）构造分析对象，要求行写回该画像；**不写晋升/转职图谱**
     * （图谱描述真实岗位间关系，画像不是具体职位）。幂等：已补全的画像直接跳过。
     */
    void analyzeProfileAndSave(Long profileId);

    /**
     * 批量补全仍缺要求的岗位画像（训练出题/差距对照与匹配计算都依赖这批数据）。
     *
     * @param limit 本次最多处理多少个（串行调用 AI，建议分批执行）
     * @return 汇总：{@code total / processed / skipped / failed / remaining}
     */
    java.util.Map<String, Object> analyzeMissingProfiles(int limit);
}