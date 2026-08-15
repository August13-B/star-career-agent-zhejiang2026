package org.example.web.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.example.web.entity.JobInfo;
import org.example.web.entity.JobRequirementProfile;
import org.example.web.entity.Result;
import org.example.web.mapper.JobProfileMapper;
import org.example.web.service.AIService;
import org.example.web.service.JobProfileService;
import org.example.web.tool.AIResponseParser;
import org.example.web.tool.InPutGiveAI;
import org.example.web.tool.SnowIdCreater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 岗位画像服务实现类
 * 实现JobProfileService接口，提供岗位画像的业务逻辑
 * 注意：当前版本只实现添加岗位信息方法，其他方法已删除
 * 
 * @author 系统生成
 * @version 1.0
 */
@Service
public class JobProfileServiceImpl implements JobProfileService {

    @Autowired
    private JobProfileMapper jobProfileMapper;

    @Autowired
    private AIService aiService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 添加岗位信息
     * 接收JobInfo对象，设置ID、时间等字段，调用Mapper插入数据库
     * 
     * @param jobInfo 岗位信息实体
     * @return 添加成功返回true，失败返回false
     */
    @Override
    public boolean addJobInfo(JobInfo jobInfo) {
        try {
            // 使用雪花算法生成ID，类别为18（可自定义，确保与其他业务不冲突）
            jobInfo.setId(SnowIdCreater.generateId(13));
            
            // 设置创建时间和更新时间
            jobInfo.setCreateTime(LocalDateTime.now());
            jobInfo.setUpdateTime(LocalDateTime.now());
            
            // 设置删除标识为0（未删除）
            jobInfo.setIsDeleted(0);
            
            // 调用Mapper插入数据
            int result = jobProfileMapper.insertJobInfo(jobInfo);
            
            // 返回操作结果
            return result > 0;
        } catch (Exception e) {
            // 记录日志或抛出异常，这里简单返回false
            System.err.println("添加岗位信息时发生错误: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Result createJobProfile(String user_id) {
        List<Map<String,Object>> jobInfoList = new ArrayList<>();
        jobInfoList = jobProfileMapper.selectJobInfo();
        Integer jobInfoLen = jobInfoList.size();
        Integer round = jobInfoLen/100+1;
        
        List<Map<String,Object>> jobProfile = null;
        jobProfile = jobProfileMapper.selectJobProfile();
        
        for(int i=41;i<round;i++){
            List<Map<String,Object>> jobInfoBatch = new ArrayList<>();
            int start = i*100;
            int end = Math.min(start + 100, jobInfoLen);
            
            for (int j = start; j < end; j++) {
                Map<String,Object> map = new HashMap<>(jobInfoList.get(j));
                map.remove("company_detail");
                map.remove("job_source_url");
                map.remove("create_time");
                map.remove("update_time");
                map.remove("is_deleted");
                jobInfoBatch.add(map);
            }
            
            Map<String,Object> data = InPutGiveAI.ai_input_create_job_profile(
                    user_id,
                    "# Role\n" +
                            "资深人力资源数据标准化专家，精通万级招聘数据极致聚合去重、岗位标准化建模与职级体系搭建\n" +
                            "\n" +
                            "# Task\n" +
                            "分批接收职业数据，完成极致聚合标准化，将海量零散数据收敛为带职级前缀的标准岗位画像，通过职级实现同岗位纵向分层。核心要求：极致去重，严禁按行业/技术栈/项目类型等非核心属性拆分岗位，不得输出「其他岗位」。\n" +
                            "\n" +
                            "# 核心规则 强制执行 一票否决\n" +
                            "## 0. JSON完整性铁则（最高优先级）\n" +
                            "必须输出**100%完整、闭合的纯JSON**，所有括号、引号、逗号必须成对出现，严禁中途截断、严禁输出半拉JSON、严禁在JSON中间或末尾添加任何非JSON内容；如果生成内容过长，必须确保在完整闭合JSON后再结束输出。" +
                            "## 1. 岗位名称标准化（最高优先级，按顺序执行）\n" +
                            "- 无效信息全剔除：彻底删除所有修饰词（急聘/高薪/全栈/资深/初级/中级/高级等）、括号/符号内所有补充内容、行业/地域/语言/技术栈等非核心限定词，仅保留岗位核心主体\n" +
                            "- 强制固定映射（含对应关键词，必须严格输出指定标准基础名，无任何例外）：\n" +
                            "  含Java→Java开发工程师；含测试/QA/QC→软件测试工程师；含实施→实施工程师；含C++/C/C++→C++开发工程师；含前端→前端开发工程师；含科研/博士后/研究员→科研人员；含Python/PHP/Go/嵌入式/后端/安卓/Android/IOS/鸿蒙→后端开发工程师；含产品/PM→产品经理；含UI/UE/UX/设计→UI/UX设计师；含运维/DevOps/SRE→运维工程师；含安全/渗透/攻防/等保→网络安全工程师；含技术支持/技术服务→技术支持工程师；含人事/HR/人力资源→人力资源专员；含财务/会计/出纳→财务专员；含销售/BD/客户经理→销售专员；含运营→运营专员；含行政/文员/前台→行政专员；含法务/合规→法务专员；含采购/物流/供应链→采购供应链专员；含文案/品牌/市场→品牌市场专员\n" +
                            "- 兜底规则：未命中上述规则，含开发/研发/工程师→研发工程师；其余统一标准化为「XX专员/XX工程师」基础名，**禁止输出「其他岗位」**\n" +
                            "- 职级前缀强制绑定：基础岗位名确定后，必须根据level判定结果，在基础名前加对应职级前缀，形成最终position_name；**level字段仅输出对应数字，不添加任何文字描述**，绑定规则如下：\n" +
                            "  level=1 → 前缀「入门」，示例：入门Java开发工程师\n" +
                            "  level=2 → 前缀「中级」，示例：中级Java开发工程师\n" +
                            "  level=3 → 前缀「高级」，示例：高级Java开发工程师\n" +
                            "\n" +
                            "## 2. 极致聚合去重铁则（核心红线，严禁违反）\n" +
                            "最终带职级前缀的position_name完全一致，无论行业/技术栈/项目等非职级差异，必须100%合并为同一个画像，严禁拆分；同基础岗位不同职级（前缀不同）可作为独立纵向分层画像，同position_name严禁重复输出。\n" +
                            "\n" +
                            "## 3. 行业标准化规则（仅补充信息，不影响岗位聚合）\n" +
                            "industry字段仅为岗位画像的补充信息，**绝对不允许因行业差异拆分同一个position_name的岗位**，只能填写以下给出的几个大类或者全行业通用，优先匹配JD中出现的最高频行业大类，无明确行业信息则填「全行业通用」，标准化行业分类如下：\n" +
                            "- IT\n" +
                            "- 金融\n" +
                            "- 制造业\n" +
                            "- 医疗健康\n" +
                            "- 房地产\n" +
                            "- 零售\n" +
                            "- 教育\n" +
                            "- 文传\n" +
                            "- 交通\n" +
                            "- 能源\n" +
                            "- 政府\n" +
                            "- 服务\n" +
                            "- 全行业通用\n" +
                            "\n" +
                            "## 4. 职级level判定规则（与前缀1:1绑定，仅输出数字）\n" +
                            "- level=1（入门）：JD要求应届生/1年以内经验、无硬性高学历/证书要求、仅需完成基础执行类工作，无独立项目负责要求\n" +
                            "- level=2（中级）：JD要求1-3年相关经验、本科及以上常规学历要求、可独立完成岗位核心工作、具备岗位必备专业能力\n" +
                            "- level=3（高级）：JD要求3年以上资深经验、硬性高学历/核心证书要求、负责核心项目/团队管理/技术攻坚、具备专家级业务/技术能力\n" +
                            "\n" +
                            "## 5. 权重打分规则（三项分值总和必须=100，不得有偏差）\n" +
                            "- hard_weight：学历/证书/从业年限等硬性门槛要求，准入要求越高分值越高\n" +
                            "- skill_weight：专业技术/编码/专属业务能力要求，技术/专业属性越强分值越高\n" +
                            "- soft_weight：沟通协调/客户对接/出差/抗压/业绩要求，商务/管理属性越强分值越高\n" +
                            "\n" +
                            "## 6. 输出强制校验铁则（必须100%满足，否则输出无效）\n" +
                            "1.  id映射：当批所有输入数据的原始ID，必须100%完整映射到对应带职级前缀的最终position_name，无遗漏、无缺失\n" +
                            "2.  画像匹配：**id映射中出现的所有唯一position_name，必须100%在job_profile中生成对应的唯一画像，岗位名称必须完全一致，无遗漏、无多余**\n" +
                            "3.  去重要求：job_profile仅输出去重后的唯一岗位画像，同position_name严禁重复输出，严禁因行业/技术栈差异拆分同一个position_name的岗位\n" +
                            "4.  行业规范：industry必须严格使用上述给出的标准化行业大类，不得自定义细分行业、不得输出非列表内的行业名称\n" +
                            "5.  格式要求：必须输出纯JSON格式，无任何多余文字、注释、格式错误\n" +
                            "\n" +
                            "# 输出格式 纯JSON 无任何多余内容\n" +
                            "{\n" +
                            "  \"id\": {\n" +
                            "    \"原始ID1\": \"带前缀的最终position_name1\",\n" +
                            "    \"原始ID2\": \"带前缀的最终position_name2\"\n" +
                            "  },\n" +
                            "  \"job_profile\": [\n" +
                            "    {\n" +
                            "      \"position_name\": \"带前缀的最终岗位全称\",\n" +
                            "      \"category\": \"研发/测试/技术服务/产品设计/职能支持/业务运营/科研\",\n" +
                            "      \"industry\": \"上述标准化行业大类中的一项\",\n" +
                            "      \"description\": \"岗位核心通用职责\",\n" +
                            "      \"level\": 1/2/3,\n" +
                            "      \"hard_weight\": 分值,\n" +
                            "      \"skill_weight\": 分值,\n" +
                            "      \"soft_weight\": 分值\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}",
                    jobProfile,
                    jobInfoBatch,
                    0.2F
            );
            
            Result ai_response = aiService.sendPostRequest(data,"/api/llm/chat/generate/raw");
            
            // 处理AI响应，插入画像并更新job_id
            processAIResponse(ai_response);
            System.out.println(ai_response);
            System.out.println("第 " + (i+1) + " 批数据处理完成，共 " + (end - start) + " 条记录");
        }
        
        return Result.success("岗位画像生成完成，共处理 " + jobInfoLen + " 条岗位信息");
    }
    
    /**
     * 处理AI响应，插入新的岗位画像并更新job_info的job_id
     * 
     * @param aiResponse AI响应结果
     */
    @SuppressWarnings("unchecked")
    private void processAIResponse(Result aiResponse) {
        try {
            if (aiResponse == null || aiResponse.getCode() != 200 || aiResponse.getData() == null) {
                System.err.println("AI响应无效");
                return;
            }
            
            // 解析响应数据
            Map<String, Object> dataMap = objectMapper.convertValue(aiResponse.getData(), 
                    new TypeReference<Map<String, Object>>() {});
            
            Object responseObj = dataMap.get("response");
            if (responseObj == null) {
                System.err.println("响应中没有response字段");
                return;
            }
            
            Map<String, Object> responseMap;
            if (responseObj instanceof String) {
                responseMap = objectMapper.readValue((String) responseObj, 
                        new TypeReference<Map<String, Object>>() {});
            } else if (responseObj instanceof Map) {
                responseMap = (Map<String, Object>) responseObj;
            } else {
                System.err.println("response字段格式不正确");
                return;
            }
            
            // 获取id映射
            Object idMapObj = responseMap.get("id");
            if (!(idMapObj instanceof Map)) {
                System.err.println("id字段格式不正确");
                return;
            }
            Map<String, String> idMap = (Map<String, String>) idMapObj;
            
            // 获取岗位画像列表
            Object jobProfileListObj = responseMap.get("job_profile");
            if (!(jobProfileListObj instanceof List)) {
                System.err.println("job_profile字段格式不正确");
                return;
            }
            List<Map<String, Object>> jobProfileList = (List<Map<String, Object>>) jobProfileListObj;
            
            // 1. 收集id映射中的所有唯一岗位名称
            Set<String> uniquePositionNamesInIdMap = new HashSet<>();
            for (String name : idMap.values()) {
                uniquePositionNamesInIdMap.add(name.trim());
            }
            
            // 2. 收集job_profile列表中的岗位名称
            Set<String> positionNamesInJobProfile = new HashSet<>();
            for (Map<String, Object> profileMap : jobProfileList) {
                String positionName = (String) profileMap.get("position_name");
                if (positionName != null) {
                    positionNamesInJobProfile.add(positionName.trim());
                }
            }
            
            // 存储岗位名称到画像ID的映射（提前声明，以便在缺失岗位名称处理中使用）
            Map<String, Long> positionNameToProfileId = new HashMap<>();
            
            // 3. 找出缺失的岗位名称（在id映射中出现但不在job_profile列表中）
            Set<String> missingPositionNames = new HashSet<>(uniquePositionNamesInIdMap);
            missingPositionNames.removeAll(positionNamesInJobProfile);
            
            if (!missingPositionNames.isEmpty()) {
                System.out.println("发现缺失的岗位画像，将优先检查数据库是否存在: " + missingPositionNames);
                
                // 先检查数据库是否已存在这些岗位画像
                for (String missingName : missingPositionNames) {
                    JobRequirementProfile existing = jobProfileMapper.selectRequirementByPositionName(missingName);
                    if (existing != null) {
                        // 数据库中存在，直接使用现有画像，不需要补充到jobProfileList
                        System.out.println("数据库中存在缺失的岗位画像，直接使用: " + missingName + ", ID: " + existing.getId());
                        // 将数据库中找到的ID提前存入positionNameToProfileId映射，避免后续重复查询
                        positionNameToProfileId.put(missingName, existing.getId());
                        // 继续处理下一个缺失名称
                        continue;
                    }
                    
                    // 数据库不存在，尝试基于同基础岗位的其他职级画像创建
                    String[] parts = extractLevelAndBaseName(missingName);
                    if (parts == null) {
                        System.err.println("无法解析岗位名称: " + missingName);
                        continue;
                    }
                    String levelPrefix = parts[0]; // "入门"、"中级"、"高级"
                    String baseName = parts[1]; // 基础岗位名称
                    
                    // 在已有的job_profile列表中查找同基础岗位的其他职级画像
                    Map<String, Object> baseProfile = null;
                    for (Map<String, Object> profileMap : jobProfileList) {
                        String positionName = (String) profileMap.get("position_name");
                        if (positionName != null) {
                            positionName = positionName.trim();
                            String[] profileParts = extractLevelAndBaseName(positionName);
                            if (profileParts != null && profileParts[1].equals(baseName)) {
                                baseProfile = profileMap;
                                break;
                            }
                        }
                    }
                    
                    if (baseProfile != null) {
                        // 基于现有画像创建缺失职级的画像
                        Map<String, Object> newProfileMap = new HashMap<>(baseProfile);
                        newProfileMap.put("position_name", missingName);
                        // 根据职级前缀设置level
                        int level = 1;
                        if (levelPrefix.equals("中级")) {
                            level = 2;
                        } else if (levelPrefix.equals("高级")) {
                            level = 3;
                        }
                        newProfileMap.put("level", level);
                        // 调整权重（可以根据职级微调，这里简单保持相同）
                        jobProfileList.add(newProfileMap);
                        System.out.println("已补充缺失的岗位画像: " + missingName + " (基于 " + baseName + ")");
                    } else {
                        System.err.println("无法为缺失的岗位名称找到基础画像，且数据库不存在: " + missingName);
                    }
                }
            }
            
            // 第一步：插入新的岗位画像（使用已声明的positionNameToProfileId映射）
            for (Map<String, Object> profileMap : jobProfileList) {
                String positionName = (String) profileMap.get("position_name");
                if (positionName == null) {
                    continue;
                }
                
                // 标准化岗位名称（去除首尾空格）
                positionName = positionName.trim();
                
                // 检查是否已存在相同岗位名称的画像
                JobRequirementProfile existing = jobProfileMapper.selectRequirementByPositionName(positionName);
                if (existing != null) {
                    // 已存在，使用现有ID
                    positionNameToProfileId.put(positionName, existing.getId());
                    System.out.println("使用现有岗位画像: " + positionName + ", ID: " + existing.getId());
                    continue;
                }
                
                // 创建新的岗位画像实体
                JobRequirementProfile newProfile = new JobRequirementProfile();
                newProfile.setId(SnowIdCreater.generateId(8)); // 雪花ID类别8
                newProfile.setPositionName(positionName);
                newProfile.setCategory((String) profileMap.get("category"));
                newProfile.setIndustry((String) profileMap.get("industry"));
                newProfile.setDescription((String) profileMap.get("description"));
                
                // 处理level字段（可能是Integer或String）
                Object levelObj = profileMap.get("level");
                if (levelObj != null) {
                    if (levelObj instanceof Integer) {
                        newProfile.setLevel((Integer) levelObj);
                    } else if (levelObj instanceof String) {
                        try {
                            newProfile.setLevel(Integer.parseInt((String) levelObj));
                        } catch (NumberFormatException e) {
                            newProfile.setLevel(1); // 默认值
                        }
                    }
                } else {
                    newProfile.setLevel(1); // 默认值
                }
                
                // 处理权重字段
                Object hardWeightObj = profileMap.get("hard_weight");
                if (hardWeightObj != null) {
                    if (hardWeightObj instanceof Number) {
                        newProfile.setHardWeight(BigDecimal.valueOf(((Number) hardWeightObj).doubleValue()));
                    } else if (hardWeightObj instanceof String) {
                        try {
                            newProfile.setHardWeight(new BigDecimal((String) hardWeightObj));
                        } catch (Exception e) {
                            newProfile.setHardWeight(BigDecimal.valueOf(30.00)); // 默认值
                        }
                    }
                } else {
                    newProfile.setHardWeight(BigDecimal.valueOf(30.00)); // 默认值
                }
                
                Object skillWeightObj = profileMap.get("skill_weight");
                if (skillWeightObj != null) {
                    if (skillWeightObj instanceof Number) {
                        newProfile.setSkillWeight(BigDecimal.valueOf(((Number) skillWeightObj).doubleValue()));
                    } else if (skillWeightObj instanceof String) {
                        try {
                            newProfile.setSkillWeight(new BigDecimal((String) skillWeightObj));
                        } catch (Exception e) {
                            newProfile.setSkillWeight(BigDecimal.valueOf(40.00)); // 默认值
                        }
                    }
                } else {
                    newProfile.setSkillWeight(BigDecimal.valueOf(40.00)); // 默认值
                }
                
                Object softWeightObj = profileMap.get("soft_weight");
                if (softWeightObj != null) {
                    if (softWeightObj instanceof Number) {
                        newProfile.setSoftWeight(BigDecimal.valueOf(((Number) softWeightObj).doubleValue()));
                    } else if (softWeightObj instanceof String) {
                        try {
                            newProfile.setSoftWeight(new BigDecimal((String) softWeightObj));
                        } catch (Exception e) {
                            newProfile.setSoftWeight(BigDecimal.valueOf(30.00)); // 默认值
                        }
                    }
                } else {
                    newProfile.setSoftWeight(BigDecimal.valueOf(30.00)); // 默认值
                }
                
                // 设置时间戳
                newProfile.setCreateTime(LocalDateTime.now());
                newProfile.setUpdateTime(LocalDateTime.now());
                newProfile.setIsDeleted(0);
                
                // 插入数据库
                int result = jobProfileMapper.insertRequirement(newProfile);
                if (result > 0) {
                    positionNameToProfileId.put(positionName, newProfile.getId());
                    System.out.println("成功插入岗位画像: " + positionName + ", ID: " + newProfile.getId());
                } else {
                    System.err.println("插入岗位画像失败: " + positionName);
                }
            }
            
            // 第二步：更新job_info表中的job_id
            int updatedCount = 0;
            int missingProfileCount = 0;
            
            for (Map.Entry<String, String> entry : idMap.entrySet()) {
                String jobInfoIdStr = entry.getKey();
                String positionName = entry.getValue();
                
                // 标准化岗位名称
                positionName = positionName.trim();
                
                Long jobInfoId;
                try {
                    jobInfoId = Long.parseLong(jobInfoIdStr);
                } catch (NumberFormatException e) {
                    System.err.println("无效的job_info ID格式: " + jobInfoIdStr);
                    continue;
                }
                
                // 查找岗位画像ID（多级容错）
                Long profileId = findProfileId(positionName, positionNameToProfileId, jobProfileList);
                
                if (profileId == null) {
                    // 尝试创建默认画像
                    profileId = createDefaultProfile(positionName);
                    if (profileId != null) {
                        positionNameToProfileId.put(positionName, profileId);
                        System.out.println("为job_info记录 " + jobInfoId + " 创建默认岗位画像: " + positionName + ", ID: " + profileId);
                    } else {
                        System.err.println("找不到岗位名称对应的画像ID: " + positionName + " (job_info ID: " + jobInfoId + ")");
                        missingProfileCount++;
                        continue; // 创建失败，跳过
                    }
                }
                
                int result = jobProfileMapper.updateJobIdById(jobInfoId, profileId);
                if (result > 0) {
                    updatedCount++;
                } else {
                    System.err.println("更新job_info的job_id失败，job_info ID: " + jobInfoId + ", profile ID: " + profileId);
                }
            }
            
            System.out.println("成功更新 " + updatedCount + " 条job_info记录的job_id");
            if (missingProfileCount > 0) {
                System.err.println("警告: 有 " + missingProfileCount + " 个岗位名称找不到对应的画像");
            }
            
            // 调试信息：打印所有id映射和对应的画像
            System.out.println("=== 调试信息: id映射统计 ===");
            System.out.println("id映射总数: " + idMap.size());
            System.out.println("job_profile列表数: " + jobProfileList.size());
            System.out.println("positionNameToProfileId映射数: " + positionNameToProfileId.size());
            
        } catch (Exception e) {
            System.err.println("处理AI响应时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 查找岗位画像ID（多级容错）
     * 1. 首先从positionNameToProfileId映射中查找
     * 2. 然后尝试从数据库中查找（精确匹配）
     * 3. 最后尝试从job_profile列表中查找相似名称
     * 
     * @param positionName 岗位名称
     * @param positionNameToProfileId 岗位名称到ID的映射
     * @param jobProfileList AI返回的岗位画像列表
     * @return 画像ID，找不到返回null
     */
    private Long findProfileId(String positionName, Map<String, Long> positionNameToProfileId, 
                               List<Map<String, Object>> jobProfileList) {
        // 1. 从映射中查找
        if (positionNameToProfileId.containsKey(positionName)) {
            return positionNameToProfileId.get(positionName);
        }
        
        // 2. 从数据库中查找（精确匹配）
        JobRequirementProfile existing = jobProfileMapper.selectRequirementByPositionName(positionName);
        if (existing != null) {
            positionNameToProfileId.put(positionName, existing.getId());
            return existing.getId();
        }
        
        // 3. 从job_profile列表中查找（模糊匹配，检查是否有相似的名称）
        for (Map<String, Object> profileMap : jobProfileList) {
            String profilePositionName = (String) profileMap.get("position_name");
            if (profilePositionName != null) {
                profilePositionName = profilePositionName.trim();
                
                // 检查是否相同（忽略大小写）
                if (profilePositionName.equalsIgnoreCase(positionName)) {
                    // 尝试从数据库中查找这个标准化的名称
                    existing = jobProfileMapper.selectRequirementByPositionName(profilePositionName);
                    if (existing != null) {
                        positionNameToProfileId.put(positionName, existing.getId()); // 缓存原始名称
                        return existing.getId();
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 创建默认岗位画像
     * 
     * @param positionName 岗位名称
     * @return 画像ID，创建失败返回null
     */
    private Long createDefaultProfile(String positionName) {
        try {
            // 根据岗位名称推断分类和行业
            String category = inferCategory(positionName);
            String industry = "IT/互联网/全行业通用";
            String description = "标准化的" + positionName + "岗位";
            
            JobRequirementProfile defaultProfile = new JobRequirementProfile();
            defaultProfile.setId(SnowIdCreater.generateId(8));
            defaultProfile.setPositionName(positionName);
            defaultProfile.setCategory(category);
            defaultProfile.setIndustry(industry);
            defaultProfile.setDescription(description);
            defaultProfile.setLevel(1); // 默认为入门级别
            
            // 设置默认权重
            defaultProfile.setHardWeight(BigDecimal.valueOf(30.00));
            defaultProfile.setSkillWeight(BigDecimal.valueOf(40.00));
            defaultProfile.setSoftWeight(BigDecimal.valueOf(30.00));
            
            // 设置时间戳
            defaultProfile.setCreateTime(LocalDateTime.now());
            defaultProfile.setUpdateTime(LocalDateTime.now());
            defaultProfile.setIsDeleted(0);
            
            // 插入数据库
            int result = jobProfileMapper.insertRequirement(defaultProfile);
            if (result > 0) {
                System.out.println("创建默认岗位画像成功: " + positionName + ", ID: " + defaultProfile.getId());
                return defaultProfile.getId();
            } else {
                System.err.println("创建默认岗位画像失败: " + positionName);
                return null;
            }
        } catch (Exception e) {
            System.err.println("创建默认岗位画像时发生错误: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据岗位名称推断分类
     * 
     * @param positionName 岗位名称
     * @return 推断的分类
     */
    private String inferCategory(String positionName) {
        if (positionName == null) {
            return "职能支持";
        }
        
        String lowerName = positionName.toLowerCase();
        
        if (lowerName.contains("开发") || lowerName.contains("研发") || lowerName.contains("前端") || 
            lowerName.contains("后端") || lowerName.contains("java") || lowerName.contains("c++") || 
            lowerName.contains("python") || lowerName.contains("php") || lowerName.contains("go") || 
            lowerName.contains("嵌入式") || lowerName.contains("安卓") || lowerName.contains("android") || 
            lowerName.contains("ios") || lowerName.contains("鸿蒙")) {
            return "研发";
        } else if (lowerName.contains("测试") || lowerName.contains("qa") || lowerName.contains("qc")) {
            return "测试";
        } else if (lowerName.contains("实施") || lowerName.contains("技术支持") || lowerName.contains("技术服务")) {
            return "技术服务";
        } else if (lowerName.contains("产品") || lowerName.contains("pm")) {
            return "产品设计";
        } else if (lowerName.contains("ui") || lowerName.contains("ue") || lowerName.contains("ux") || lowerName.contains("设计")) {
            return "产品设计";
        } else if (lowerName.contains("运维") || lowerName.contains("devops") || lowerName.contains("sre")) {
            return "技术服务";
        } else if (lowerName.contains("安全") || lowerName.contains("渗透") || lowerName.contains("攻防") || lowerName.contains("等保")) {
            return "技术服务";
        } else if (lowerName.contains("科研") || lowerName.contains("博士后") || lowerName.contains("研究员")) {
            return "科研";
        } else {
            return "职能支持";
        }
    }
    
    /**
     * 从岗位名称中提取职级前缀和基础名称
     * 例如："入门Java开发工程师" -> ["入门", "Java开发工程师"]
     * 
     * @param positionName 岗位名称
     * @return 数组，[0]为职级前缀（入门/中级/高级），[1]为基础名称，如果不匹配返回null
     */
    private String[] extractLevelAndBaseName(String positionName) {
        if (positionName == null || positionName.isEmpty()) {
            return null;
        }
        
        // 定义职级前缀
        String[] levelPrefixes = {"入门", "中级", "高级"};
        
        for (String prefix : levelPrefixes) {
            if (positionName.startsWith(prefix)) {
                String baseName = positionName.substring(prefix.length());
                if (!baseName.isEmpty()) {
                    return new String[]{prefix, baseName};
                }
            }
        }
        
        return null;
    }
}
