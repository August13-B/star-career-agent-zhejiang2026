package org.example.web.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.web.entity.AiConversation;
import org.example.web.entity.AiMessage;
import org.example.web.entity.Result;
import org.example.web.entity.StudentAbility;
import org.example.web.entity.StudentProfile;
import org.example.web.entity.User;
import org.example.web.mapper.AiConversationMapper;
import org.example.web.mapper.StudentAbilityMapper;
import org.example.web.mapper.StudentProfileMapper;
import org.example.web.mapper.UserMapper;
import org.example.web.service.AIConversationService;
import org.example.web.service.AIService;
import org.example.web.tool.InPutGiveAI;
import org.example.web.tool.JsonUtils;
import org.example.web.tool.RSA_256;
import org.example.web.tool.SnowIdCreater;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;

/**
 * AI对话服务实现类
 */
@Service
@SuppressWarnings("unchecked")
public class AIConversationServiceImpl implements AIConversationService {

    @Autowired
    private AiConversationMapper aiConversationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AIService aiService;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JsonUtils jsonUtils;

    @Autowired
    private StudentProfileMapper studentProfileMapper;
    
    @Autowired
    private StudentAbilityMapper studentAbilityMapper;
    
    @Autowired
    private RSA_256 rsa256;

    @Override
    public Result<?> getConversationHistory(Long conversationId) {
        try {
            // 根据对话ID查询对话信息
            AiConversation conversation = aiConversationMapper.selectConversationById(conversationId);
            if (conversation == null) {
                return Result.error("对话不存在");
            }
            
            // 检查对话是否已删除
            if (conversation.getIsDeleted() != null && conversation.getIsDeleted() == 1) {
                return Result.error("对话已被删除");
            }
            
            // 获取该对话的所有消息
            List<AiMessage> messages = aiConversationMapper.selectMessagesByConversationId(conversationId);
            
            // 返回消息列表
            List<Map<String, Object>> messageList = new ArrayList<>();
            for (AiMessage message : messages) {
                ObjectMapper mapper = new ObjectMapper();
                String content = message.getContent();
                Map<String, Object> jsonMap = new HashMap<>();
                try {
                    // 尝试作为 JSON 解析
                    Map<String, Object> json = mapper.readValue(content, Map.class);
                    Object responseObj = json.get("response");
                    if (responseObj != null) {
                        // 如果response是Map类型，可能包含嵌套的response字段
                        if (responseObj instanceof Map) {
                            Map<String, Object> responseMap = (Map<String, Object>) responseObj;
                            // 检查是否包含嵌套的response字段
                            if (responseMap.containsKey("response")) {
                                jsonMap.put("response", responseMap.get("response"));
                            } else {
                                // 如果没有嵌套response，使用整个responseMap
                                jsonMap.put("response", responseMap);
                            }
                        } else {
                            // response是字符串或其他类型
                            jsonMap.put("response", responseObj);
                        }
                    } else {
                        // 没有response字段，将整个json作为response
                        jsonMap.put("response", json);
                    }
                } catch (Exception e) {
                    // 不是 JSON，作为普通文本处理
                    jsonMap.put("response", content);
                }

                Map<String, Object> context = jsonUtils.strToObj(message.getContextInfo().toString(), Map.class);
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("messageId", message.getId());
                msgMap.put("type", message.getMessageType());
                msgMap.put("content", jsonMap);
                msgMap.put("contextInfo", context);
                msgMap.put("createTime", message.getCreateTime());
                messageList.add(msgMap);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("conversationId", conversationId);
            result.put("title", conversation.getTitle());
            result.put("type", conversation.getConversationType());
            result.put("status", conversation.getStatus());
            result.put("createTime", conversation.getCreateTime());
            result.put("updateTime", conversation.getUpdateTime());
            result.put("messages", messageList);
            
            return Result.success("获取对话历史成功", result);
        } catch (Exception e) {
            return Result.error("获取对话历史失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<?> sendMessage(Long userId, String userMessage, Integer conversationType, Long conversationId) {
        return sendMessage(userId, userMessage, conversationType, conversationId, 1.0);
    }
    
    @Override
    @Transactional
    public Result<?> sendMessage(Long userId, String userMessage, Integer conversationType, Long conversationId, Double temperature) {
        try {
            // 1. 获取用户信息，获取nickname作为role
            User user = userMapper.findById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            String userNickname = user.getNickname();
            if (userNickname == null || userNickname.trim().isEmpty()) {
                userNickname = "用户"; // 默认值
            }
            
            // 2. 获取对话：如果提供了conversationId，则使用指定的对话；否则自动获取或创建
            AiConversation conversation;
            if (conversationId != null && conversationId > 0) {
                // 使用指定的对话ID
                conversation = aiConversationMapper.selectConversationById(conversationId);
                if (conversation == null) {
                    return Result.error("指定的对话不存在");
                }
                // 检查对话是否属于当前用户
                if (!conversation.getUserId().equals(userId)) {
                    return Result.error("无权访问此对话");
                }
                // 检查对话是否已删除
                if (conversation.getIsDeleted() != null && conversation.getIsDeleted() == 1) {
                    return Result.error("对话已被删除");
                }
                // 更新对话状态为进行中
                conversation.setStatus(1);
                conversation.setUpdateTime(LocalDateTime.now());
                // 更新数据库中的状态
                aiConversationMapper.updateConversationStatus(conversation.getId(), 1);
                System.out.println("使用指定对话ID: " + conversationId);
            } else {
                // 自动获取或创建对话
                conversation = getOrCreateConversation(userId, conversationType, userMessage);
                if (conversation == null) {
                    return Result.error("创建或获取对话失败");
                }
            }
            
            // 3. 获取该对话的历史消息（用于构建上下文）
            List<AiMessage> historyMessages = aiConversationMapper.selectMessagesByConversationId(conversation.getId());
            
            // 4. 构建contextInfo为JSON格式，如果失败则设为空JSON对象
            String contextInfo = "{}";
            try {
                Map<String, Object> contextMap = new HashMap<>();
                contextMap.put("userMessage", userMessage);
                contextMap.put("timestamp", LocalDateTime.now().toString());
                contextInfo = objectMapper.writeValueAsString(contextMap);
            } catch (Exception e) {
                // 如果JSON序列化失败，使用空JSON对象
                System.err.println("构建contextInfo JSON失败: " + e.getMessage());
                contextInfo = "{}";
            }
            
            // 5. 格式化历史对话，为AI提供上下文
            String conversationHistory = formatConversationHistory(historyMessages);
            
            // 6. 获取学生信息（每次对话都获取）
            String studentInfo = getFormattedStudentInfo(userId);
            
            // 7. 构建完整的AI请求（按照用户要求的新格式）
            // 使用用户提供的temperature参数，如果为空则使用1.0
            Float temp = temperature != null ? temperature.floatValue() : 1.0F;
            String systemPrompt = "这是一个用户与AI自由对话的接口，除非用户要求，则不用进行打分，主要是解答用户对职业和自身的问题,如果是用户提到与岗位相关信息，自身画像相关的问题则都可以回答，回答不能假大空，返回值的json结构只有一个参数response:''禁止加入其他任何参数，里面记录模型返回的话语";
            if (!studentInfo.isEmpty()) {
                systemPrompt = systemPrompt + "\n\n" + studentInfo;
            }
            Map<String,Object> aiRequest = InPutGiveAI.ai_input_with_history(String.valueOf(userId),
                    systemPrompt,
                    "以下是历史对话记录，请基于历史对话继续回答：\n\n" + conversationHistory,
                    userMessage,
                    temp
            );
            // 7. 检查是否已存在相同的用户消息（防止重复保存）
            AiMessage existingUserMsg = null;
            LocalDateTime tenSecondsAgo = LocalDateTime.now().minusSeconds(10);
            for (AiMessage msg : historyMessages) {
                if (msg.getMessageType() == 1 && 
                    msg.getContent().equals(userMessage) &&
                    msg.getCreateTime().isAfter(tenSecondsAgo)) {
                    existingUserMsg = msg;
                    break;
                }
            }
            
            AiMessage userMsg;
            int sequence;
            if (existingUserMsg != null) {
                // 使用已存在的用户消息
                userMsg = existingUserMsg;
                sequence = userMsg.getSequence();
                System.out.println("检测到重复用户消息，使用已有消息ID: " + userMsg.getId());
            } else {
                // 创建新的用户消息
                userMsg = new AiMessage();
                userMsg.setId(Long.valueOf(SnowIdCreater.generateId(23))); // 使用类别23表示AI消息
                userMsg.setConversationId(conversation.getId());
                userMsg.setMessageType(1); // 1-用户输入
                userMsg.setContentType(1); // 1-文本
                userMsg.setContent(userMessage);
                userMsg.setContextInfo(contextInfo);
                
                // 计算sequence：历史消息数量 + 1
                sequence = historyMessages.size() + 1;
                userMsg.setSequence(sequence);
                userMsg.setCreateTime(LocalDateTime.now());
                
                aiConversationMapper.insertMessage(userMsg);
            }
            
            // 8. 更新对话状态为生成中，并更新更新时间
            conversation.setStatus(1); // 生成中
            conversation.setUpdateTime(LocalDateTime.now());
            // 更新数据库中的状态
            aiConversationMapper.updateConversationStatus(conversation.getId(), 1);
            
            // 9. 发送请求到AI服务（同步）使用新接口
            String message = (String) aiRequest.get("content");
            Result<?> aiResponse = aiService.chat(message, temperature);
            
            // 10. 处理AI响应，保存AI消息
            Map<String, Object> aiResponseResult = processAIResponseSync(conversation.getId(), aiResponse, sequence + 1, contextInfo);
            AiMessage aiMsg = (AiMessage) aiResponseResult.get("aiMessage");
            Map<String, Object> rawResponse = (Map<String, Object>) aiResponseResult.get("rawResponse");
            
            // 11. aiMessage字段使用完整的AI响应JSON对象
            // rawResponse已经是Map，包含AI的完整响应
            Object aiMessageField = rawResponse;
            
            // 构建返回结果
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("conversationId", conversation.getId());
            resultData.put("userMessage", userMessage);
            resultData.put("userMessageId", userMsg.getId());
            // aiMessage字段使用完整的AI响应JSON对象
            resultData.put("aiMessage", aiMessageField);
            resultData.put("aiMessageId", aiMsg.getId());
            resultData.put("createTime", aiMsg.getCreateTime());
            System.out.println("AI原始响应: " + rawResponse);
            return Result.success("消息发送成功", resultData);
        } catch (Exception e) {
            return Result.error("发送消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理AI响应（异步版本）
     */
    private void processAIResponse(Long conversationId, Result<?> aiResponse, int sequence, String contextInfo) {
        try {
            if (aiResponse == null) {
                throw new RuntimeException("AI响应为空");
            }
            
            // 解析AI响应，获取回复内容
            String aiContent = extractAIContent(aiResponse);

            Map<String, Object> response_content = jsonUtils.strToObj(aiContent, Map.class);
            String  content = response_content.get("response").toString();
            Map<String, Object> AiContent = jsonUtils.strToObj(content, Map.class);
            System.out.println(AiContent);
            // 保存AI消息到数据库
            AiMessage aiMsg = new AiMessage();
            aiMsg.setId(SnowIdCreater.generateId(23));
            aiMsg.setConversationId(conversationId);
            aiMsg.setMessageType(2); // 2-AI回复
            aiMsg.setContentType(1); // 1-文本
            aiMsg.setContent(aiContent);
            aiMsg.setContextInfo(contextInfo);
            aiMsg.setSequence(sequence);
            aiMsg.setCreateTime(LocalDateTime.now());
            
            aiConversationMapper.insertMessage(aiMsg);
            
            // 更新对话状态为已完成（状态2）
            aiConversationMapper.updateConversationStatus(conversationId, 2);
            System.out.println("AI回复已保存，对话ID: " + conversationId + "，状态已更新为已完成");
            
        } catch (Exception e) {
            System.err.println("处理AI响应失败: " + e.getMessage());
        }
    }
    
    /**
     * 提取AI响应中的原始response对象，确保返回一个Map，包含response字段，且response字段的值为纯文本字符串
     * 如果AI返回的是字符串，则包装为{"response": "字符串"}
     * 如果AI返回的是Map但没有response字段，则尝试提取文本内容放在response中
     * 始终返回Map<String, Object>，确保aiMessage字段为JSON对象，且response字段为纯文本
     */
    private Map<String, Object> extractAIResponseRaw(Result<?> aiResponse) {
        Map<String, Object> resultMap = new HashMap<>();

        if (aiResponse == null || aiResponse.getData() == null) {
            resultMap.put("response", "AI生成回复失败");
            return resultMap;
        }

        Object data = aiResponse.getData();

        // 如果data是Map类型
        if (data instanceof Map) {
            Map<?, ?> dataMap = (Map<?, ?>) data;
            // 检查是否有response字段
            if (dataMap.containsKey("response")) {
                Object response = dataMap.get("response");
                
                // 如果response是字符串，使用ensurePlainText提取纯文本，处理嵌套JSON
                if (response instanceof String) {
                    String responseStr = (String) response;
                    String pureText = ensurePlainText(responseStr);
                    resultMap.put("response", pureText);
                    // 保留其他字段（可选）
                    for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                        if (!"response".equals(entry.getKey())) {
                            resultMap.put(entry.getKey().toString(), entry.getValue());
                        }
                    }
                    return resultMap;
                }
                
                // 对于非字符串类型的response，使用原有逻辑提取纯文本
                String responseText = extractPureTextFromResponseObject(response);
                resultMap.put("response", responseText);
                // 保留其他字段（可选）
                for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                    if (!"response".equals(entry.getKey())) {
                        resultMap.put(entry.getKey().toString(), entry.getValue());
                    }
                }
                return resultMap;
            } else {
                // 没有response字段，尝试提取文本内容
                String textContent = extractTextFromObject(dataMap);
                if (textContent != null) {
                    resultMap.put("response", textContent);
                } else {
                    // 无法提取文本，将整个data转换为字符串作为response
                    resultMap.put("response", dataMap.toString());
                }
                return resultMap;
            }
        }

        // 如果data是字符串，尝试解析为JSON
        if (data instanceof String) {
            String dataStr = (String) data;
            try {
                // 使用JsonUtils解析，可能更宽松
                Object parsed = jsonUtils.strToObj(dataStr, Object.class);
                if (parsed instanceof Map) {
                    // 递归处理Map
                    return extractAIResponseRaw(Result.success(parsed));
                } else {
                    // 如果不是Map，直接作为字符串包装
                    resultMap.put("response", parsed.toString());
                    return resultMap;
                }
            } catch (Exception e) {
                //System.out.println("字符串不能被解析"+dataStr);
                // 如果不是JSON格式，直接作为字符串包装
                resultMap.put("response", dataStr);
                return resultMap;
            }
        }

        // 其他情况，将data转换为字符串包装
        resultMap.put("response", data.toString());
        return resultMap;
    }

    /**
     * 获取并格式化学生信息（包括个人画像和能力维度）
     * 如果查询不到学生信息，返回空字符串
     * 注意：数据库中的字符串字段可能是密文存储，需要解密
     */
    private String getFormattedStudentInfo(Long userId) {
        // 先测试解密功能
        try {
            String testEncrypted = rsa256.encryptForDB("test");
            String testDecrypted = rsa256.decryptFromDB(testEncrypted);
            System.err.println("测试解密功能 - 加密: " + testEncrypted + ", 解密: " + testDecrypted);
        } catch (Exception e) {
            System.err.println("测试解密功能失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 调试：测试特定用户ID的数据解密
        System.err.println("========== 调试学生信息解密 ==========");
        System.err.println("用户ID: " + userId);
        
        StringBuilder studentInfo = new StringBuilder();
        
        try {
            // 1. 查询学生画像信息
            List<StudentProfile> studentProfileList = studentProfileMapper.selectByUserId(userId);
            StudentProfile studentProfile = null;
            if (studentProfileList != null && !studentProfileList.isEmpty()) {
                studentProfile = studentProfileList.get(0);
            }
            if (studentProfile != null) {
                System.err.println("找到学生画像: " + studentProfile.getId());
                
                // 调试：直接测试解密
                String testUserName = studentProfile.getUserName();
                System.err.println("原始user_name字段: " + testUserName);
                System.err.println("字段长度: " + (testUserName != null ? testUserName.length() : 0));
                System.err.println("isValidBase64: " + isValidBase64(testUserName));
                
                // 尝试直接解密
                if (testUserName != null && !testUserName.trim().isEmpty()) {
                    try {
                        String decryptedName = rsa256.decryptFromDB(testUserName.trim());
                        System.err.println("直接解密user_name结果: " + decryptedName);
                    } catch (Exception e) {
                        System.err.println("直接解密user_name失败: " + e.getMessage());
                    }
                }
                
                studentInfo.append("【学生基本信息】\n");
                // 解密并添加字段
                addDecryptedField(studentInfo, "姓名", studentProfile.getUserName());
                if (studentProfile.getGender() != null) {
                    studentInfo.append("性别: ").append(studentProfile.getGender() == 1 ? "男" : studentProfile.getGender() == 2 ? "女" : "未填写").append("\n");
                }
                addDecryptedField(studentInfo, "院校", studentProfile.getCollege());
                addDecryptedField(studentInfo, "专业", studentProfile.getMajor());
                addDecryptedField(studentInfo, "年级", studentProfile.getGrade());
                addDecryptedField(studentInfo, "学历", studentProfile.getEducation());
                addDecryptedField(studentInfo, "职业意向", studentProfile.getCareerIntentions());
                addDecryptedField(studentInfo, "目标城市", studentProfile.getTargetCity());
                addDecryptedField(studentInfo, "期望薪资", studentProfile.getExpectedSalary());
                addDecryptedField(studentInfo, "行业偏好", studentProfile.getIndustryPreference());
                addDecryptedField(studentInfo, "工作/实习经历", studentProfile.getWorkExperience());
                addDecryptedField(studentInfo, "项目经历", studentProfile.getProjectExperience());
                addDecryptedField(studentInfo, "技能特长", studentProfile.getSkill());
                addDecryptedField(studentInfo, "持有证书", studentProfile.getCertificate());
                studentInfo.append("\n");
            } else {
                System.err.println("未找到学生画像信息");
            }
            
            // 2. 查询学生能力维度信息
            List<StudentAbility> studentAbilityList = studentAbilityMapper.selectByUserId(userId);
            StudentAbility studentAbility = null;
            if (studentAbilityList != null && !studentAbilityList.isEmpty()) {
                studentAbility = studentAbilityList.get(0);
            }
            if (studentAbility != null) {
                System.err.println("找到学生能力维度: " + studentAbility.getId());
                studentInfo.append("【学生能力维度】\n");
                addDecryptedField(studentInfo, "学历背景", studentAbility.getEducationRequirement());
                addDecryptedField(studentInfo, "实习经历", studentAbility.getInternshipAbility());
                addDecryptedField(studentInfo, "专业技能", studentAbility.getProfessionalSkill());
                addDecryptedField(studentInfo, "证书资质", studentAbility.getCertificateRequirement());
                addDecryptedField(studentInfo, "创新能力", studentAbility.getInnovationAbility());
                addDecryptedField(studentInfo, "学习能力", studentAbility.getLearningAbility());
                addDecryptedField(studentInfo, "抗压能力", studentAbility.getPressureResistance());
                addDecryptedField(studentInfo, "沟通能力", studentAbility.getCommunicationAbility());
                addDecryptedField(studentInfo, "问题解决能力", studentAbility.getProblemSolving());
                addDecryptedField(studentInfo, "团队协作能力", studentAbility.getTeamworkAbility());
                studentInfo.append("\n");
            } else {
                System.err.println("未找到学生能力维度信息");
            }
            
            // 3. 如果没有任何信息，返回空字符串
            if (studentInfo.length() == 0) {
                System.err.println("学生信息为空，返回空字符串");
                return "";
            }
            
            // 添加提示语，告诉AI如何使用这些信息
            studentInfo.append("---\n");
            studentInfo.append("注意：以上是用户的个人信息和能力维度，请根据这些信息提供个性化的回答和建议。\n");
            
            System.err.println("最终学生信息字符串: " + studentInfo.toString());
            System.err.println("========== 调试结束 ==========");
            
            return studentInfo.toString();
        } catch (Exception e) {
            System.err.println("获取学生信息失败: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 处理AI响应（同步版本）
     * 返回Map包含aiMessage和rawResponse
     */
    private Map<String, Object> processAIResponseSync(Long conversationId, Result<?> aiResponse, int sequence, String contextInfo) {
        try {
            if (aiResponse == null) {
                throw new RuntimeException("AI响应为空");
            }

            // 提取原始response对象，现在总是返回Map
            Map<String, Object> rawResponse = extractAIResponseRaw(aiResponse);

            // 生成用于存储的内容：将Map序列化为JSON字符串
            String aiContent;
            try {
                aiContent = objectMapper.writeValueAsString(rawResponse);
            } catch (Exception e) {
                aiContent = rawResponse.toString();
            }

            // 保存AI消息到数据库
            AiMessage aiMsg = new AiMessage();
            aiMsg.setId(SnowIdCreater.generateId(23));
            aiMsg.setConversationId(conversationId);
            aiMsg.setMessageType(2); // 2-AI回复
            aiMsg.setContentType(1); // 1-文本
            aiMsg.setContent(aiContent);
            aiMsg.setContextInfo(contextInfo);
            aiMsg.setSequence(sequence);
            aiMsg.setCreateTime(LocalDateTime.now());

            aiConversationMapper.insertMessage(aiMsg);

            // 更新对话状态为已完成（状态2）
            aiConversationMapper.updateConversationStatus(conversationId, 2);
            System.out.println("AI回复已保存，对话ID: " + conversationId + "，状态已更新为已完成（同步）");

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("aiMessage", aiMsg);
            result.put("rawResponse", rawResponse);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("处理AI响应失败: " + e.getMessage());
        }
    }

    /**
     * 从AI响应中提取内容
     */
    private String extractAIContent(Result<?> aiResponse) {
        // 根据实际AI响应结构解析
        // AI返回的Result的data字段可能是包含response字段的Map
        if (aiResponse.getData() != null) {
            Object data = aiResponse.getData();

            // 1. 如果data是Map类型，尝试提取response字段
            if (data instanceof Map) {
                Map<?, ?> dataMap = (Map<?, ?>) data;
                Object response = dataMap.get("response");
                if (response != null) {
                    // 如果response是Map类型，进一步提取文本内容
                    if (response instanceof Map) {
                        Map<?, ?> responseMap = (Map<?, ?>) response;
                        // 尝试从response Map中提取文本内容
                        for (String key : new String[]{"content", "text", "message", "answer", "reply", "generated_text"}) {
                            if (responseMap.containsKey(key)) {
                                Object text = responseMap.get(key);
                                if (text != null) {
                                    return text.toString();
                                }
                            }
                        }
                        // 如果没有找到特定键，尝试查找任何字符串值
                        for (Object value : responseMap.values()) {
                            if (value instanceof String) {
                                String strValue = (String) value;
                                if (!strValue.trim().isEmpty()) {
                                    return strValue;
                                }
                            }
                        }
                        // 如果都找不到，返回整个response对象的字符串表示
                        return responseMap.toString();
                    }
                    // 如果response不是Map，直接转换为字符串
                    return response.toString();
                }
                // 如果Map中没有response字段，但可能直接包含内容
                // 尝试查找其他可能的键
                for (String key : new String[]{"content", "answer", "text", "message"}) {
                    if (dataMap.containsKey(key)) {
                        Object value = dataMap.get(key);
                        if (value != null) {
                            return value.toString();
                        }
                    }
                }
            }

            // 2. 如果data是字符串，尝试解析为JSON对象
            if (data instanceof String) {
                String dataStr = (String) data;
                // 尝试解析JSON
                try {
                    Map<?, ?> jsonMap = objectMapper.readValue(dataStr, Map.class);
                    Object response = jsonMap.get("response");
                    if (response != null) {
                        // 如果response是Map类型，进一步提取文本内容
                        if (response instanceof Map) {
                            Map<?, ?> responseMap = (Map<?, ?>) response;
                            // 尝试从response Map中提取文本内容
                            for (String key : new String[]{"content", "text", "message", "answer", "reply", "generated_text"}) {
                                if (responseMap.containsKey(key)) {
                                    Object text = responseMap.get(key);
                                    if (text != null) {
                                        return text.toString();
                                    }
                                }
                            }
                            // 如果没有找到特定键，尝试查找任何字符串值
                            for (Object value : responseMap.values()) {
                                if (value instanceof String) {
                                    String strValue = (String) value;
                                    if (!strValue.trim().isEmpty()) {
                                        return strValue;
                                    }
                                }
                            }
                            // 如果都找不到，返回整个response对象的字符串表示
                            return responseMap.toString();
                        }
                        // 如果response不是Map，直接转换为字符串
                        return response.toString();
                    }
                    // 如果没有response字段，尝试其他可能的键
                    for (String key : new String[]{"content", "answer", "text", "message"}) {
                        if (jsonMap.containsKey(key)) {
                            Object value = jsonMap.get(key);
                            if (value != null) {
                                return value.toString();
                            }
                        }
                    }
                } catch (Exception e) {
                    // 如果不是JSON格式，直接返回字符串
                    // 尝试提取类似{conversation_type=1, user_id=... response=...}格式中的response
                    String extracted = extractResponseFromString(dataStr);
                    if (extracted != null) {
                        return extracted;
                    }
                    return dataStr;
                }
            }

            // 3. 其他情况返回data的字符串表示（可能包含多余信息）
            return data.toString();
        }
        return "AI生成回复失败";
    }

    /**
     * 从非标准格式字符串中提取response内容
     * 例如：{conversation_type=1, user_id=232703462741114880, conversation_id=3, response=......}
     * 支持多种格式：response=value, response:value, "response":value
     */
    private String extractResponseFromString(String dataStr) {
        if (dataStr == null || dataStr.isEmpty()) {
            return null;
        }

        // 尝试多种匹配模式
        String[] patterns = {
            "response\\s*=\\s*([^,}]+)",           // response=value
            "response\\s*:\\s*([^,}]+)",           // response:value
            "\"response\"\\s*:\\s*\"([^\"]+)\"",   // "response":"value"
            "\"response\"\\s*:\\s*'([^']+)'",      // "response":'value'
            "\"response\"\\s*:\\s*([^,}]+)",       // "response":value
            "'response'\\s*:\\s*\"([^\"]+)\"",     // 'response':"value"
            "'response'\\s*:\\s*'([^']+)'",        // 'response':'value'
            "'response'\\s*:\\s*([^,}]+)"          // 'response':value
        };

        for (String pattern : patterns) {
            try {
                java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher matcher = regex.matcher(dataStr);
                if (matcher.find()) {
                    String response = matcher.group(1).trim();
                    // 如果response被引号包裹，去掉引号
                    if (response.startsWith("\"") && response.endsWith("\"")) {
                        response = response.substring(1, response.length() - 1);
                    }
                    if (response.startsWith("'") && response.endsWith("'")) {
                        response = response.substring(1, response.length() - 1);
                    }
                    return response;
                }
            } catch (Exception e) {
                // 继续尝试下一个模式
            }
        }

        // 如果没有匹配到任何模式，尝试简单的字符串查找
        int responseIndex = dataStr.indexOf("response=");
        if (responseIndex == -1) {
            responseIndex = dataStr.indexOf("response:");
        }
        if (responseIndex == -1) {
            responseIndex = dataStr.indexOf("\"response\"");
        }
        if (responseIndex == -1) {
            responseIndex = dataStr.indexOf("'response'");
        }

        if (responseIndex != -1) {
            // 找到response关键字，尝试提取值
            // 查找值开始位置（跳过=或:以及可能的空格）
            int valueStart = responseIndex;
            for (int i = responseIndex; i < dataStr.length(); i++) {
                char c = dataStr.charAt(i);
                if (c == '=' || c == ':') {
                    valueStart = i + 1;
                    break;
                }
            }

            // 跳过值前的空格
            while (valueStart < dataStr.length() && Character.isWhitespace(dataStr.charAt(valueStart))) {
                valueStart++;
            }

            // 确定值的结束位置
            int valueEnd = valueStart;
            boolean inQuotes = false;
            char quoteChar = 0;

            for (int i = valueStart; i < dataStr.length(); i++) {
                char c = dataStr.charAt(i);

                // 处理引号
                if (c == '"' || c == '\'') {
                    if (!inQuotes) {
                        inQuotes = true;
                        quoteChar = c;
                    } else if (c == quoteChar) {
                        inQuotes = false;
                    }
                }

                // 如果不在引号内，遇到逗号、大括号、换行符则结束
                if (!inQuotes && (c == ',' || c == '}' || c == '\n' || c == '\r')) {
                    valueEnd = i;
                    break;
                }

                valueEnd = i + 1;
            }

            if (valueStart < valueEnd && valueEnd <= dataStr.length()) {
                String response = dataStr.substring(valueStart, valueEnd).trim();
                // 如果response被引号包裹，去掉引号
                if (response.startsWith("\"") && response.endsWith("\"")) {
                    response = response.substring(1, response.length() - 1);
                }
                if (response.startsWith("'") && response.endsWith("'")) {
                    response = response.substring(1, response.length() - 1);
                }
                return response;
            }
        }

        return null;
    }

    /**
     * 从任意对象中递归提取文本内容
     * 支持Map、List、String等类型，递归查找直到找到非空字符串
     */
    private String extractTextFromObject(Object obj) {
        if (obj == null) {
            return null;
        }

        // 如果是字符串，直接返回
        if (obj instanceof String) {
            String str = (String) obj;
            if (!str.trim().isEmpty()) {
                return str;
            }
            return null;
        }

        // 如果是Map，递归查找每个值
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            // 优先查找常见键
            String[] priorityKeys = {"content", "text", "message", "answer", "reply", "generated_text", "response", "result", "output"};
            for (String key : priorityKeys) {
                if (map.containsKey(key)) {
                    String text = extractTextFromObject(map.get(key));
                    if (text != null) {
                        return text;
                    }
                }
            }
            // 如果没有找到优先级键，遍历所有值递归查找
            for (Object value : map.values()) {
                String text = extractTextFromObject(value);
                if (text != null) {
                    return text;
                }
            }
            return null;
        }

        // 如果是List，遍历每个元素递归查找
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (Object item : list) {
                String text = extractTextFromObject(item);
                if (text != null) {
                    return text;
                }
            }
            return null;
        }

        // 其他类型，转换为字符串
        String str = obj.toString();
        if (!str.trim().isEmpty() && !str.startsWith("{") && !str.startsWith("[")) {
            // 如果不是JSON对象或数组，返回字符串
            return str;
        }

        return null;
    }

    /**
     * 获取或创建对话（优化版：如果没有未删除的对话，则恢复最近已删除的对话）
     */
    private AiConversation getOrCreateConversation(Long userId, Integer conversationType, String userMessage) {
        // 查找用户最近的一个未删除的对话（is_deleted=0），优先选择相同类型的对话
        List<AiConversation> conversations = aiConversationMapper.selectConversationsByUserId(userId);
        AiConversation existingConversation = null;
        AiConversation sameTypeConversation = null;

        for (AiConversation conv : conversations) {
            // 跳过已删除的对话（第一轮只处理未删除的）
            if (conv.getIsDeleted() != null && conv.getIsDeleted() == 1) {
                continue;
            }

            // 优先选择相同类型的对话
            if (conv.getConversationType().equals(conversationType)) {
                sameTypeConversation = conv;
                break; // 找到相同类型的未删除对话，直接使用
            }

            // 如果没有相同类型的对话，记录第一个未删除的对话
            if (existingConversation == null) {
                existingConversation = conv;
            }
        }

        // 优先返回相同类型的未删除对话，否则返回其他类型的未删除对话
        AiConversation selectedConversation = sameTypeConversation != null ? sameTypeConversation : existingConversation;

        if (selectedConversation != null) {
            // 更新对话状态为进行中（如果已结束）
            if (selectedConversation.getStatus() != 1) {
                selectedConversation.setStatus(1);
                selectedConversation.setUpdateTime(LocalDateTime.now());
                // 更新数据库中的状态
                aiConversationMapper.updateConversationStatus(selectedConversation.getId(), 1);
            }
            return selectedConversation;
        }

        // 没有未删除的对话，查找已删除的对话（is_deleted=1），优先选择相同类型的对话
        AiConversation deletedSameTypeConversation = null;
        AiConversation deletedConversation = null;

        for (AiConversation conv : conversations) {
            // 只处理已删除的对话
            if (conv.getIsDeleted() == null || conv.getIsDeleted() == 0) {
                continue;
            }

            // 优先选择相同类型的已删除对话
            if (conv.getConversationType().equals(conversationType)) {
                deletedSameTypeConversation = conv;
                break; // 找到相同类型的已删除对话，直接使用
            }

            // 如果没有相同类型的已删除对话，记录第一个已删除的对话
            if (deletedConversation == null) {
                deletedConversation = conv;
            }
        }

        // 优先恢复相同类型的已删除对话，否则恢复其他类型的已删除对话
        AiConversation deletedToRestore = deletedSameTypeConversation != null ? deletedSameTypeConversation : deletedConversation;

        if (deletedToRestore != null) {
            // 恢复已删除的对话
            // 根据用户消息生成标题（截取前20个字符）
            String title = userMessage.length() > 20 ? userMessage.substring(0, 20) + "..." : userMessage;
            aiConversationMapper.restoreConversationById(deletedToRestore.getId(), title);

            // 重新设置对话对象的状态
            deletedToRestore.setIsDeleted(0);
            deletedToRestore.setStatus(1);
            deletedToRestore.setTitle(title);
            deletedToRestore.setUpdateTime(LocalDateTime.now());

            System.out.println("恢复已删除对话，对话ID: " + deletedToRestore.getId() + ", 新标题: " + title);
            return deletedToRestore;
        }

        // 没有未删除的对话，也没有已删除的对话，创建新的对话
        AiConversation newConversation = new AiConversation();
        newConversation.setId(SnowIdCreater.generateId(22)); // 使用类别22表示对话
        newConversation.setUserId(userId);
        newConversation.setConversationType(conversationType);

        // 根据用户消息生成标题（截取前20个字符）
        String title = userMessage.length() > 20 ? userMessage.substring(0, 20) + "..." : userMessage;
        newConversation.setTitle(title);

        newConversation.setStatus(1); // 进行中
        newConversation.setCreateTime(LocalDateTime.now());
        newConversation.setUpdateTime(LocalDateTime.now());
        newConversation.setIsDeleted(0);

        aiConversationMapper.insertConversation(newConversation);
        return newConversation;
    }

    /**
     * 构建消息列表
     */
    private List<Map<String, String>> buildMessageList(List<AiMessage> historyMessages, String userRole, String newMessage) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 添加历史消息
        for (AiMessage msg : historyMessages) {
            Map<String, String> message = new HashMap<>();
            if (msg.getMessageType() == 1) { // 用户消息
                message.put("role", "user");
            } else { // AI消息
                message.put("role", "assistant");
            }
            message.put("content", msg.getContent());
            messages.add(message);
        }

        // 添加最新用户消息
        Map<String, String> newMsg = new HashMap<>();
        newMsg.put("role", "user");
        newMsg.put("content", newMessage);
        messages.add(newMsg);

        return messages;
    }

    /**
     * 将消息转换为历史对话格式
     */
    private List<Map<String, Object>> convertMessagesToHistory(List<AiMessage> messages) {
        List<Map<String, Object>> history = new ArrayList<>();

        for (AiMessage msg : messages) {
            Map<String, Object> historyItem = new HashMap<>();
            historyItem.put("type", msg.getMessageType() == 1 ? "user" : "ai");
            historyItem.put("content", msg.getContent());
            historyItem.put("time", msg.getCreateTime());
            history.add(historyItem);
        }

        return history;
    }

    
    /**
     * 解密字段并添加到字符串构建器
     * 如果字段为空或解密失败，使用原始值
     */
    private void addDecryptedField(StringBuilder sb, String fieldName, String encryptedValue) {
        if (encryptedValue != null && !encryptedValue.trim().isEmpty()) {
            String trimmedValue = encryptedValue.trim();
            try {
                // 首先检查是否是有效的Base64格式（密文通常是Base64编码）
                if (isValidBase64(trimmedValue)) {
                    System.err.println("尝试解密Base64格式字段: " + fieldName + ", 长度: " + trimmedValue.length());
                    // 尝试解密
                    String decrypted = rsa256.decryptFromDB(trimmedValue);
                    System.err.println("解密成功 - 字段名: " + fieldName + ", 密文长度: " + trimmedValue.length() + ", 明文: " + decrypted);
                    sb.append(fieldName).append(": ").append(decrypted).append("\n");
                } else {
                    // 不是有效的Base64，可能是明文或格式错误
                    System.err.println("字段不是有效的Base64格式，直接使用: " + fieldName + ", 值: " + trimmedValue);
                    sb.append(fieldName).append(": ").append(trimmedValue).append("\n");
                }
            } catch (Exception e) {
                // 解密失败，可能已经是明文或格式错误，使用原始值
                System.err.println("解密字段失败 - 字段名: " + fieldName + ", 异常: " + e.getMessage() + ", 值: " + trimmedValue);
                e.printStackTrace();
                sb.append(fieldName).append(": ").append(trimmedValue).append("\n");
            }
        }
    }
    
    /**
     * 检查字符串是否是有效的Base64格式
     */
    private boolean isValidBase64(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        // Base64格式通常包含A-Z, a-z, 0-9, +, /, =，长度通常是4的倍数
        String base64Pattern = "^[A-Za-z0-9+/]+={0,2}$";
        if (!value.matches(base64Pattern)) {
            return false;
        }
        
        // 检查长度是否是4的倍数（考虑填充字符=）
        int length = value.length();
        if (length % 4 != 0) {
            return false;
        }
        
        // 尝试解码验证
        try {
            java.util.Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从流式chunks中提取纯文本内容
     * 假设chunks是JSON格式，包含data字段
     */
    private String extractPureTextFromChunks(List<String> chunks) {
        StringBuilder pureText = new StringBuilder();
        for (String chunk : chunks) {
            try {
                Map<?, ?> chunkMap = objectMapper.readValue(chunk, Map.class);
                if (chunkMap.containsKey("data")) {
                    Object data = chunkMap.get("data");
                    if (data != null) {
                        pureText.append(data.toString());
                    }
                } else {
                    // 如果没有data字段，尝试查找其他可能的文本字段
                    for (Object value : chunkMap.values()) {
                        if (value instanceof String) {
                            pureText.append(value);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // 不是JSON，直接作为文本
                pureText.append(chunk);
            }
        }
        return pureText.toString();
    }

    @Override
    public Result<?> getConversationDetail(Long conversationId) {
        try {
            // 获取对话信息
            // 注意：AiConversationMapper没有根据ID查询单个对话的方法，需要先获取用户所有对话再过滤
            // 这里简化处理，实际应该添加相应方法
            return Result.error("功能待实现");
        } catch (Exception e) {
            return Result.error("获取对话详情失败: " + e.getMessage());
        }
    }

    @Override
    public Result<?> createConversation(Long userId, Integer conversationType, String title) {
        try {
            AiConversation conversation = new AiConversation();
            conversation.setId(SnowIdCreater.generateId(22));
            conversation.setUserId(userId);
            conversation.setConversationType(conversationType);
            conversation.setTitle(title);
            conversation.setStatus(1); // 进行中
            conversation.setCreateTime(LocalDateTime.now());
            conversation.setUpdateTime(LocalDateTime.now());
            conversation.setIsDeleted(0);

            aiConversationMapper.insertConversation(conversation);

            return Result.success("创建对话成功", conversation);
        } catch (Exception e) {
            return Result.error("创建对话失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<?> endConversation(Long conversationId) {
        try {
            // 1. 逻辑删除对话主表记录（设置is_deleted=1）
            aiConversationMapper.deleteConversationById(conversationId);

            // 2. 逻辑删除该对话下的所有消息（设置is_deleted=1）
            aiConversationMapper.deleteMessagesByConversationId(conversationId);

            return Result.success("对话已结束并删除");
        } catch (Exception e) {
            return Result.error("结束对话失败: " + e.getMessage());
        }
    }

    @Override
    public Result<?> getUserConversations(Long userId) {
        try {
            List<AiConversation> conversations = aiConversationMapper.selectConversationsByUserId(userId);
            // 过滤已删除的对话
            List<AiConversation> filteredConversations = new ArrayList<>();
            for (AiConversation conversation : conversations) {
                if (conversation.getIsDeleted() == null || conversation.getIsDeleted() == 0) {
                    filteredConversations.add(conversation);
                }
            }
            return Result.success("获取对话列表成功", filteredConversations);
        } catch (Exception e) {
            return Result.error("获取对话列表失败: " + e.getMessage());
        }
    }

    /**
     * 格式化历史对话为文本，方便AI理解上下文
     * 格式示例：
     * 用户：你好
     * AI：你好，有什么可以帮助您的？
     * 用户：我想了解Spring Boot
     * AI：Spring Boot是一个...
     *
     * @param historyMessages 历史消息列表
     * @return 格式化的历史对话文本
     */
    private String formatConversationHistory(List<AiMessage> historyMessages) {
        if (historyMessages == null || historyMessages.isEmpty()) {
            return "";
        }

        StringBuilder historyBuilder = new StringBuilder();
        // 限制历史对话长度，避免token超限
        int maxHistoryMessages = 10; // 最多保留最近10条历史消息
        int startIndex = Math.max(0, historyMessages.size() - maxHistoryMessages);

        for (int i = startIndex; i < historyMessages.size(); i++) {
            AiMessage msg = historyMessages.get(i);
            String role = msg.getMessageType() == 1 ? "用户" : "AI";
            historyBuilder.append(role)
                         .append("：")
                         .append(msg.getContent())
                         .append("\n");
        }

        return historyBuilder.toString();
    }

    @Override
    public Result<?> updateConversationTitle(Long userId, Long conversationId, String newTitle) {
        try {
            // 参数验证
            if (newTitle == null || newTitle.trim().isEmpty()) {
                return Result.error("标题不能为空");
            }
            if (newTitle.length() > 255) {
                return Result.error("标题长度不能超过255个字符");
            }

            // 更新标题（同时验证用户ID和对话ID匹配）
            int affectedRows = aiConversationMapper.updateTitleByIdAndUserId(conversationId, userId, newTitle.trim());
            if (affectedRows == 0) {
                // 可能原因：对话不存在、对话已删除、对话不属于该用户
                // 可以先检查对话是否存在
                AiConversation conversation = aiConversationMapper.selectConversationById(conversationId);
                if (conversation == null) {
                    return Result.error("对话不存在");
                }
                if (conversation.getIsDeleted() != null && conversation.getIsDeleted() == 1) {
                    return Result.error("对话已被删除");
                }
                if (!conversation.getUserId().equals(userId)) {
                    return Result.error("无权修改此对话标题");
                }
                return Result.error("更新标题失败");
            }
            return Result.success("标题更新成功");
        } catch (Exception e) {
            return Result.error("更新标题失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> sendMessageStream(Long userId, String userMessage, Integer conversationType, Long conversationId) {
        // 调用带默认温度值的方法
        return sendMessageStream(userId, userMessage, conversationType, conversationId, 1.0);
    }

    @Override
    public Flux<String> sendMessageStream(Long userId, String userMessage, Integer conversationType, Long conversationId, Double temperature) {
        // 流式版本：保存用户消息，调用AI流式接口，收集响应并保存AI消息
        try {
            // 1. 获取用户信息
            User user = userMapper.findById(userId);
            if (user == null) {
                return Flux.error(new RuntimeException("用户不存在"));
            }

            // 2. 获取或创建对话
            AiConversation conversation;
            if (conversationId != null && conversationId > 0) {
                conversation = aiConversationMapper.selectConversationById(conversationId);
                if (conversation == null || !conversation.getUserId().equals(userId)) {
                    return Flux.error(new RuntimeException("对话不存在或无权限"));
                }
                if (conversation.getIsDeleted() != null && conversation.getIsDeleted() == 1) {
                    return Flux.error(new RuntimeException("对话已被删除"));
                }
                // 更新状态
                aiConversationMapper.updateConversationStatus(conversation.getId(), 1);
            } else {
                conversation = getOrCreateConversation(userId, conversationType, userMessage);
                if (conversation == null) {
                    return Flux.error(new RuntimeException("创建或获取对话失败"));
                }
            }

            // 3. 获取历史消息
            List<AiMessage> historyMessages = aiConversationMapper.selectMessagesByConversationId(conversation.getId());
            String conversationHistory = formatConversationHistory(historyMessages);

            // 4. 构建contextInfo
            String contextInfoStr;
            try {
                Map<String, Object> contextMap = new HashMap<>();
                contextMap.put("userMessage", userMessage);
                contextMap.put("timestamp", LocalDateTime.now().toString());
                contextInfoStr = objectMapper.writeValueAsString(contextMap);
            } catch (Exception e) {
                System.err.println("构建contextInfo JSON失败: " + e.getMessage());
                contextInfoStr = "{}";
            }
            final String finalContextInfo = contextInfoStr;

            // 5. 检查是否已存在相同的用户消息（防止重复保存）
            AiMessage existingUserMsg = null;
            LocalDateTime tenSecondsAgo = LocalDateTime.now().minusSeconds(10);
            for (AiMessage msg : historyMessages) {
                if (msg.getMessageType() == 1 &&
                    msg.getContent().equals(userMessage) &&
                    msg.getCreateTime().isAfter(tenSecondsAgo)) {
                    existingUserMsg = msg;
                    break;
                }
            }

            AiMessage userMsg;
            int sequence;
            if (existingUserMsg != null) {
                // 使用已存在的用户消息
                userMsg = existingUserMsg;
                sequence = userMsg.getSequence();
                System.out.println("检测到重复用户消息，使用已有消息ID: " + userMsg.getId());
            } else {
                // 创建新的用户消息
                userMsg = new AiMessage();
                userMsg.setId(Long.valueOf(SnowIdCreater.generateId(23)));
                userMsg.setConversationId(conversation.getId());
                userMsg.setMessageType(1); // 1-用户输入
                userMsg.setContentType(1); // 1-文本
                userMsg.setContent(userMessage);
                userMsg.setContextInfo(finalContextInfo);

                // 计算sequence：历史消息数量 + 1
                sequence = historyMessages.size() + 1;
                userMsg.setSequence(sequence);
                userMsg.setCreateTime(LocalDateTime.now());

                aiConversationMapper.insertMessage(userMsg);
            }

            // 6. 更新对话状态为生成中
            conversation.setStatus(1);
            conversation.setUpdateTime(LocalDateTime.now());
            aiConversationMapper.updateConversationStatus(conversation.getId(), 1);

            // 7. 获取学生信息（每次对话都获取）
            String studentInfo = getFormattedStudentInfo(userId);
            
            // 8. 构建最终消息
            String finalMessage;
            if (conversationHistory.isEmpty()) {
                if (!studentInfo.isEmpty()) {
                    finalMessage = studentInfo + "\n\n" + userMessage;
                } else {
                    finalMessage = userMessage;
                }
            } else {
                if (!studentInfo.isEmpty()) {
                    finalMessage = "以下是历史对话记录，请基于历史对话继续回答：\n\n" +
                                   conversationHistory +
                                   "\n\n【学生个人信息】\n" + studentInfo +
                                   "\n\n当前用户问题：" + userMessage;
                } else {
                    finalMessage = "以下是历史对话记录，请基于历史对话继续回答：\n\n" +
                                   conversationHistory +
                                   "\n\n当前用户问题：" + userMessage;
                }
            }

            // 8. 使用用户提供的temperature参数，如果为空则使用1.0
            Double finalTemperature = temperature != null ? temperature : 1.0;

            // 9. 调用AI服务流式接口，收集响应并保存AI消息
            return aiService.chatStream(finalMessage, finalTemperature)
                    .collectList()
                    .flatMapMany(chunks -> {
                        // 处理每个chunk，提取纯文本并重新构建chunk，同时拼接完整响应
                        List<String> processedChunks = new ArrayList<>();
                        StringBuilder fullResponseBuilder = new StringBuilder();
                        for (String chunk : chunks) {
                            try {
                                // 尝试解析chunk为JSON对象
                                Map<?, ?> chunkMap = objectMapper.readValue(chunk, Map.class);
                                if (chunkMap.containsKey("data")) {
                                    Object data = chunkMap.get("data");
                                    if (data != null) {
                                        // 递归提取纯文本
                                        String pureText = extractPureTextFromResponseObject(data);
                                        // 重新构建chunk
                                        Map<String, Object> processedChunkMap = new HashMap<>();
                                        processedChunkMap.put("data", pureText);
                                        String processedChunk = objectMapper.writeValueAsString(processedChunkMap);
                                        processedChunks.add(processedChunk);
                                        // 用于保存AI消息的拼接
                                        fullResponseBuilder.append(pureText);
                                    } else {
                                        processedChunks.add(chunk);
                                        fullResponseBuilder.append(chunk);
                                    }
                                } else {
                                    // 如果没有data字段，使用整个chunk的字符串表示
                                    processedChunks.add(chunk);
                                    fullResponseBuilder.append(chunk);
                                }
                            } catch (Exception e) {
                                // 不是JSON，直接作为文本
                                processedChunks.add(chunk);
                                fullResponseBuilder.append(chunk);
                            }
                        }
                        String fullResponseText = fullResponseBuilder.toString();

                        // 保存AI消息
                        try {
                            // 提取纯文本内容
                            String pureText = extractPureTextFromResponseObject(fullResponseText);
                            // 构建统一的响应JSON
                            Map<String, Object> rawResponse = new HashMap<>();
                            rawResponse.put("response", pureText);

                            String aiContentToStore;
                            try {
                                aiContentToStore = objectMapper.writeValueAsString(rawResponse);
                            } catch (Exception e) {
                                aiContentToStore = rawResponse.toString();
                            }

                            AiMessage aiMsg = new AiMessage();
                            aiMsg.setId(SnowIdCreater.generateId(23));
                            aiMsg.setConversationId(conversation.getId());
                            aiMsg.setMessageType(2); // 2-AI回复
                            aiMsg.setContentType(1); // 1-文本
                            aiMsg.setContent(aiContentToStore);
                            aiMsg.setContextInfo(finalContextInfo);
                            aiMsg.setSequence(sequence + 1);
                            aiMsg.setCreateTime(LocalDateTime.now());

                            aiConversationMapper.insertMessage(aiMsg);

                            // 更新对话状态为已完成
                            aiConversationMapper.updateConversationStatus(conversation.getId(), 2);
                            System.out.println("AI流式回复已保存，对话ID: " + conversation.getId());

                            // 返回处理后的chunks的Flux
                            return Flux.fromIterable(processedChunks);
                        } catch (Exception e) {
                            System.err.println("保存AI流式消息失败: " + e.getMessage());
                            // 返回处理后的chunks，但记录错误
                            return Flux.fromIterable(processedChunks);
                        }
                    })
                    .onErrorResume(e -> {
                        // 发生错误时，返回错误信息
                        return Flux.error(new RuntimeException("流式处理失败: " + e.getMessage()));
                    });
        } catch (Exception e) {
            return Flux.error(new RuntimeException("发送流式消息失败: " + e.getMessage()));
        }
    }

    @Override
    public Result<?> sendMessageWithImage(Long userId, String userMessage, Integer conversationType, Long conversationId, Double temperature, String imageUrl) {
        try {
            // 1. 获取用户信息，获取nickname作为role
            User user = userMapper.findById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 2. 获取对话：如果提供了conversationId，则使用指定的对话；否则自动获取或创建
            AiConversation conversation;
            if (conversationId != null && conversationId > 0) {
                // 使用指定的对话ID
                conversation = aiConversationMapper.selectConversationById(conversationId);
                if (conversation == null) {
                    return Result.error("指定的对话不存在");
                }
                // 检查对话是否属于当前用户
                if (!conversation.getUserId().equals(userId)) {
                    return Result.error("无权访问此对话");
                }
                // 检查对话是否已删除
                if (conversation.getIsDeleted() != null && conversation.getIsDeleted() == 1) {
                    return Result.error("对话已被删除");
                }
                // 更新对话状态为进行中
                conversation.setStatus(1);
                conversation.setUpdateTime(LocalDateTime.now());
                // 更新数据库中的状态
                aiConversationMapper.updateConversationStatus(conversation.getId(), 1);
                System.out.println("使用指定对话ID: " + conversationId);
            } else {
                // 自动获取或创建对话
                conversation = getOrCreateConversation(userId, conversationType, userMessage);
                if (conversation == null) {
                    return Result.error("创建或获取对话失败");
                }
            }

            // 3. 获取该对话的历史消息（用于构建上下文）
            List<AiMessage> historyMessages = aiConversationMapper.selectMessagesByConversationId(conversation.getId());

            // 4. 构建contextInfo为JSON格式，包含imageUrl
            String contextInfoStr;
            try {
                Map<String, Object> contextMap = new HashMap<>();
                contextMap.put("userMessage", userMessage);
                contextMap.put("imageUrl", imageUrl);
                contextMap.put("timestamp", LocalDateTime.now().toString());
                contextInfoStr = objectMapper.writeValueAsString(contextMap);
            } catch (Exception e) {
                System.err.println("构建contextInfo JSON失败: " + e.getMessage());
                contextInfoStr = "{}";
            }
            final String contextInfo = contextInfoStr;

            // 5. 格式化历史对话，为AI提供上下文
            String conversationHistory = formatConversationHistory(historyMessages);

            // 6. 获取学生信息（每次对话都获取）
            String studentInfo = getFormattedStudentInfo(userId);
            
            // 7. 构建最终消息
            String finalMessage;
            if (conversationHistory.isEmpty()) {
                if (!studentInfo.isEmpty()) {
                    finalMessage = studentInfo + "\n\n" + userMessage;
                } else {
                    finalMessage = userMessage;
                }
            } else {
                if (!studentInfo.isEmpty()) {
                    finalMessage = "以下是历史对话记录，请基于历史对话继续回答：\n\n" +
                                   conversationHistory +
                                   "\n\n【学生个人信息】\n" + studentInfo +
                                   "\n\n当前用户问题：" + userMessage;
                } else {
                    finalMessage = "以下是历史对话记录，请基于历史对话继续回答：\n\n" +
                                   conversationHistory +
                                   "\n\n当前用户问题：" + userMessage;
                }
            }

            // 检查是否已存在相同的用户消息（防止重复保存）
            AiMessage existingUserMsg = null;
            LocalDateTime tenSecondsAgo = LocalDateTime.now().minusSeconds(10);
            for (AiMessage msg : historyMessages) {
                if (msg.getMessageType() == 1 &&
                    msg.getContent().equals(userMessage) &&
                    msg.getCreateTime().isAfter(tenSecondsAgo)) {
                    existingUserMsg = msg;
                    break;
                }
            }

            AiMessage userMsg;
            int sequence;
            if (existingUserMsg != null) {
                // 使用已存在的用户消息
                userMsg = existingUserMsg;
                sequence = userMsg.getSequence();
                System.out.println("检测到重复用户消息，使用已有消息ID: " + userMsg.getId());
            } else {
                // 创建新的用户消息
                userMsg = new AiMessage();
                userMsg.setId(Long.valueOf(SnowIdCreater.generateId(23))); // 使用类别23表示AI消息
                userMsg.setConversationId(conversation.getId());
                userMsg.setMessageType(1); // 1-用户输入
                userMsg.setContentType(1); // 1-文本
                userMsg.setContent(userMessage);
                userMsg.setContextInfo(contextInfo);

                // 计算sequence：历史消息数量 + 1
                sequence = historyMessages.size() + 1;
                userMsg.setSequence(sequence);
                userMsg.setCreateTime(LocalDateTime.now());

                aiConversationMapper.insertMessage(userMsg);
            }

            // 更新对话状态为生成中
            conversation.setStatus(1);
            conversation.setUpdateTime(LocalDateTime.now());
            aiConversationMapper.updateConversationStatus(conversation.getId(), 1);

            // 使用用户提供的temperature参数，如果为空则使用0.7（带图片对话通常使用较低温度）
            Double finalTemperature = temperature != null ? temperature : 0.7;
            // 调用AI服务带图片接口
            Result<?> aiResponse = aiService.chatWithImage(finalMessage, finalTemperature, imageUrl);

            // 处理AI响应，保存AI消息
            Map<String, Object> aiResponseResult = processAIResponseSync(conversation.getId(), aiResponse, sequence + 1, contextInfo);
            AiMessage aiMsg = (AiMessage) aiResponseResult.get("aiMessage");
            Map<String, Object> rawResponse = (Map<String, Object>) aiResponseResult.get("rawResponse");

            // 构建返回结果
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("conversationId", conversation.getId());
            resultData.put("userMessage", userMessage);
            resultData.put("userMessageId", userMsg.getId());
            resultData.put("aiMessage", rawResponse);
            resultData.put("aiMessageId", aiMsg.getId());
            resultData.put("createTime", aiMsg.getCreateTime());
            resultData.put("imageUrl", imageUrl);
            System.out.println("AI带图片原始响应: " + rawResponse);
            return Result.success("带图片消息发送成功", resultData);
        } catch (Exception e) {
            return Result.error("发送带图片消息失败: " + e.getMessage());
        }
    }

    @Override
    public Flux<String> sendMessageWithImageStream(Long userId, String userMessage, Integer conversationType, Long conversationId, Double temperature, String imageUrl) {
        try {
            // 1. 获取用户信息
            User user = userMapper.findById(userId);
            if (user == null) {
                return Flux.error(new RuntimeException("用户不存在"));
            }

            // 2. 获取或创建对话
            AiConversation conversation;
            if (conversationId != null && conversationId > 0) {
                conversation = aiConversationMapper.selectConversationById(conversationId);
                if (conversation == null || !conversation.getUserId().equals(userId)) {
                    return Flux.error(new RuntimeException("对话不存在或无权限"));
                }
                if (conversation.getIsDeleted() != null && conversation.getIsDeleted() == 1) {
                    return Flux.error(new RuntimeException("对话已被删除"));
                }
                // 更新状态
                aiConversationMapper.updateConversationStatus(conversation.getId(), 1);
            } else {
                conversation = getOrCreateConversation(userId, conversationType, userMessage);
                if (conversation == null) {
                    return Flux.error(new RuntimeException("创建或获取对话失败"));
                }
            }

            // 3. 获取历史消息
            List<AiMessage> historyMessages = aiConversationMapper.selectMessagesByConversationId(conversation.getId());
            String conversationHistory = formatConversationHistory(historyMessages);

            // 4. 构建contextInfo（包含imageUrl）
            String contextInfoStr;
            try {
                Map<String, Object> contextMap = new HashMap<>();
                contextMap.put("userMessage", userMessage);
                contextMap.put("imageUrl", imageUrl);
                contextMap.put("timestamp", LocalDateTime.now().toString());
                contextInfoStr = objectMapper.writeValueAsString(contextMap);
            } catch (Exception e) {
                System.err.println("构建contextInfo JSON失败: " + e.getMessage());
                contextInfoStr = "{}";
            }
            final String finalContextInfo = contextInfoStr;

            // 5. 检查是否已存在相同的用户消息（防止重复保存）
            AiMessage existingUserMsg = null;
            LocalDateTime tenSecondsAgo = LocalDateTime.now().minusSeconds(10);
            for (AiMessage msg : historyMessages) {
                if (msg.getMessageType() == 1 &&
                    msg.getContent().equals(userMessage) &&
                    msg.getCreateTime().isAfter(tenSecondsAgo)) {
                    existingUserMsg = msg;
                    break;
                }
            }

            AiMessage userMsg;
            int sequence;
            if (existingUserMsg != null) {
                // 使用已存在的用户消息
                userMsg = existingUserMsg;
                sequence = userMsg.getSequence();
                System.out.println("检测到重复用户消息，使用已有消息ID: " + userMsg.getId());
            } else {
                // 创建新的用户消息
                userMsg = new AiMessage();
                userMsg.setId(Long.valueOf(SnowIdCreater.generateId(23)));
                userMsg.setConversationId(conversation.getId());
                userMsg.setMessageType(1); // 1-用户输入
                userMsg.setContentType(1); // 1-文本
                userMsg.setContent(userMessage);
                userMsg.setContextInfo(finalContextInfo);

                // 计算sequence：历史消息数量 + 1
                sequence = historyMessages.size() + 1;
                userMsg.setSequence(sequence);
                userMsg.setCreateTime(LocalDateTime.now());

                aiConversationMapper.insertMessage(userMsg);
            }

            // 6. 获取学生信息（每次对话都获取）
            String studentInfo = getFormattedStudentInfo(userId);
            
            // 7. 更新对话状态为生成中
            conversation.setStatus(1);
            conversation.setUpdateTime(LocalDateTime.now());
            aiConversationMapper.updateConversationStatus(conversation.getId(), 1);

            // 8. 构建最终消息
            String finalMessage;
            if (conversationHistory.isEmpty()) {
                if (!studentInfo.isEmpty()) {
                    finalMessage = studentInfo + "\n\n" + userMessage;
                } else {
                    finalMessage = userMessage;
                }
            } else {
                if (!studentInfo.isEmpty()) {
                    finalMessage = "以下是历史对话记录，请基于历史对话继续回答：\n\n" +
                                   conversationHistory +
                                   "\n\n【学生个人信息】\n" + studentInfo +
                                   "\n\n当前用户问题：" + userMessage;
                } else {
                    finalMessage = "以下是历史对话记录，请基于历史对话继续回答：\n\n" +
                                   conversationHistory +
                                   "\n\n当前用户问题：" + userMessage;
                }
            }

            // 9. 使用用户提供的temperature参数，如果为空则使用0.7（带图片对话通常使用较低温度）
            Double finalTemperature = temperature != null ? temperature : 0.7;

            // 9. 调用AI服务带图片流式接口，收集响应并保存AI消息
            return aiService.chatWithImageStream(finalMessage, finalTemperature, imageUrl)
                    .collectList()
                    .flatMapMany(chunks -> {
                        // 处理每个chunk，提取纯文本并重新构建chunk，同时拼接完整响应
                        List<String> processedChunks = new ArrayList<>();
                        StringBuilder fullResponseBuilder = new StringBuilder();
                        for (String chunk : chunks) {
                            try {
                                // 尝试解析chunk为JSON对象
                                Map<?, ?> chunkMap = objectMapper.readValue(chunk, Map.class);
                                if (chunkMap.containsKey("data")) {
                                    Object data = chunkMap.get("data");
                                    if (data != null) {
                                        // 递归提取纯文本
                                        String pureText = extractPureTextFromResponseObject(data);
                                        // 重新构建chunk
                                        Map<String, Object> processedChunkMap = new HashMap<>();
                                        processedChunkMap.put("data", pureText);
                                        String processedChunk = objectMapper.writeValueAsString(processedChunkMap);
                                        processedChunks.add(processedChunk);
                                        // 用于保存AI消息的拼接
                                        fullResponseBuilder.append(pureText);
                                    } else {
                                        processedChunks.add(chunk);
                                        fullResponseBuilder.append(chunk);
                                    }
                                } else {
                                    // 如果没有data字段，使用整个chunk的字符串表示
                                    processedChunks.add(chunk);
                                    fullResponseBuilder.append(chunk);
                                }
                            } catch (Exception e) {
                                // 不是JSON，直接作为文本
                                processedChunks.add(chunk);
                                fullResponseBuilder.append(chunk);
                            }
                        }
                        String fullResponseText = fullResponseBuilder.toString();

                        // 保存AI消息
                        try {
                            // 提取纯文本内容
                            String pureText = extractPureTextFromResponseObject(fullResponseText);
                            // 构建统一的响应JSON
                            Map<String, Object> rawResponse = new HashMap<>();
                            rawResponse.put("response", pureText);

                            String aiContentToStore;
                            try {
                                aiContentToStore = objectMapper.writeValueAsString(rawResponse);
                            } catch (Exception e) {
                                aiContentToStore = rawResponse.toString();
                            }

                            AiMessage aiMsg = new AiMessage();
                            aiMsg.setId(SnowIdCreater.generateId(23));
                            aiMsg.setConversationId(conversation.getId());
                            aiMsg.setMessageType(2); // 2-AI回复
                            aiMsg.setContentType(1); // 1-文本
                            aiMsg.setContent(aiContentToStore);
                            aiMsg.setContextInfo(finalContextInfo);
                            aiMsg.setSequence(sequence + 1);
                            aiMsg.setCreateTime(LocalDateTime.now());

                            aiConversationMapper.insertMessage(aiMsg);

                            // 更新对话状态为已完成
                            aiConversationMapper.updateConversationStatus(conversation.getId(), 2);
                            System.out.println("AI带图片流式回复已保存，对话ID: " + conversation.getId());

                            // 返回处理后的chunks的Flux
                            return Flux.fromIterable(processedChunks);
                        } catch (Exception e) {
                            System.err.println("保存AI带图片流式消息失败: " + e.getMessage());
                            // 返回处理后的chunks，但记录错误
                            return Flux.fromIterable(processedChunks);
                        }
                    })
                    .onErrorResume(e -> {
                        // 发生错误时，返回错误信息
                        return Flux.error(new RuntimeException("带图片流式处理失败: " + e.getMessage()));
                    });
        } catch (Exception e) {
            return Flux.error(new RuntimeException("发送带图片流式消息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 确保文本为纯文本，如果输入是JSON字符串，提取其中的文本内容
     */
    private String ensurePlainText(String str) {
        if (str == null) {
            return "";
        }
        // 如果字符串是JSON格式，提取纯文本
        if (str.trim().startsWith("{") && str.trim().endsWith("}")) {
            try {
                Map<?, ?> map = objectMapper.readValue(str, Map.class);
                String text = extractTextFromObject(map);
                if (text != null) {
                    return text;
                }
            } catch (Exception e) {
                // 解析失败，返回原字符串
            }
        }
        return str;
    }
    
    /**
     * 从响应对象中提取纯文本
     */
    private String extractPureTextFromResponseObject(Object response) {
        return extractPureTextRecursive(response, 0);
    }
    
    /**
     * 递归提取纯文本
     */
    private String extractPureTextRecursive(Object response, int depth) {
        if (depth > 5) {
            return response != null ? response.toString() : "";
        }

        if (response == null) {
            return "";
        }

        // 如果是字符串
        if (response instanceof String) {
            String str = (String) response;
            // 尝试检查是否是JSON字符串
            if (str.trim().startsWith("{") && str.trim().endsWith("}")) {
                try {
                    Map<?, ?> nestedMap = objectMapper.readValue(str, Map.class);
                    // 递归处理解析后的Map
                    return extractPureTextRecursive(nestedMap, depth + 1);
                } catch (Exception e) {
                    // 不是有效的JSON，直接返回字符串
                    return str;
                }
            }
            return str;
        }

        // 如果是Map类型
        if (response instanceof Map) {
            Map<?, ?> responseMap = (Map<?, ?>) response;
            // 优先查找response字段
            if (responseMap.containsKey("response")) {
                Object nestedResponse = responseMap.get("response");
                return extractPureTextRecursive(nestedResponse, depth + 1);
            } else {
                // 没有response字段，尝试提取文本内容
                String text = extractTextFromObject(responseMap);
                if (text != null) {
                    return text;
                } else {
                    // 无法提取文本，将整个Map转换为字符串
                    try {
                        return objectMapper.writeValueAsString(responseMap);
                    } catch (Exception e) {
                        return responseMap.toString();
                    }
                }
            }
        }

        // 其他类型转换为字符串
        return response.toString();
    }
}
