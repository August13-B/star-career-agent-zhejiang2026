package org.example.web.service;

import org.example.web.entity.Result;

import reactor.core.publisher.Flux;

/**
 * AI对话服务接口
 * 处理AI对话相关的业务逻辑，包括对话历史获取、AI请求构建、结果保存等
 */
public interface AIConversationService {
    
    /**
     * 获取特定对话的详细历史（按对话ID）
     * @param conversationId 对话ID
     * @return 包含对话详细历史（所有消息）的结果
     */
    Result<?> getConversationHistory(Long conversationId);
    
    /**
     * 发送用户消息并获取AI回复
     * @param userId 用户ID
     * @param userMessage 用户消息内容
     * @param conversationType 对话类型：1-职业规划咨询，2-模拟面试，3-技能提升指导，4-其他
     * @param conversationId 对话ID（可选，如果提供则发送到指定对话，否则系统自动选择或创建对话）
     * @return 包含AI回复的结果
     */
    Result<?> sendMessage(Long userId, String userMessage, Integer conversationType, Long conversationId);
    
    /**
     * 发送用户消息并获取AI回复
     * @param userId 用户ID
     * @param userMessage 用户消息内容
     * @param conversationType 对话类型：1-职业规划咨询，2-模拟面试，3 技能提升指导，4-其他
     * @param conversationId 对话ID（可选，如果提供则发送到指定对话，否则系统自动选择或创建对话）
     * @param temperature 温度参数，控制AI回复的随机性，范围0.0-2.0，默认1.0
     * @return 包含AI回复的结果
     */
    Result<?> sendMessage(Long userId, String userMessage, Integer conversationType, Long conversationId, Double temperature);
    
    /**
     * 流式发送用户消息并获取AI回复
     * @param userId 用户ID
     * @param userMessage 用户消息内容
     * @param conversationType 对话类型：1-职业规划咨询，2-模拟面试，3-技能提升指导，4-其他
     * @param conversationId 对话ID（可选，如果提供则发送到指定对话，否则系统自动选择或创建对话）
     * @return 流式响应（SSE事件流）
     */
    Flux<String> sendMessageStream(Long userId, String userMessage, Integer conversationType, Long conversationId);
    
    /**
     * 流式发送用户消息并获取AI回复
     * @param userId 用户ID
     * @param userMessage 用户消息内容
     * @param conversationType 对话类型：1-职业规划咨询，2-模拟面试，3-技能提升指导，4-其他
     * @param conversationId 对话ID（可选，如果提供则发送到指定对话，否则系统自动选择或创建对话）
     * @param temperature 温度参数，控制AI回复的随机性，范围0.0-2.0，默认1.0
     * @return 流式响应（SSE事件流）
     */
    Flux<String> sendMessageStream(Long userId, String userMessage, Integer conversationType, Long conversationId, Double temperature);
    
    /**
     * 发送带图片的用户消息并获取AI回复
     * @param userId 用户ID
     * @param userMessage 用户消息内容
     * @param conversationType 对话类型：1-职业规划咨询，2-模拟面试，3-技能提升指导，4-其他
     * @param conversationId 对话ID（可选，如果提供则发送到指定对话，否则系统自动选择或创建对话）
     * @param temperature 温度参数，控制AI回复的随机性，范围0.0-2.0，默认1.0
     * @param imageUrl 图片URL
     * @return 包含AI回复的结果
     */
    Result<?> sendMessageWithImage(Long userId, String userMessage, Integer conversationType, Long conversationId, Double temperature, String imageUrl);
    
    /**
     * 流式发送带图片的用户消息并获取AI回复
     * @param userId 用户ID
     * @param userMessage 用户消息内容
     * @param conversationType 对话类型：1-职业规划咨询，2-模拟面试，3-技能提升指导，4-其他
     * @param conversationId 对话ID（可选，如果提供则发送到指定对话，否则系统自动选择或创建对话）
     * @param temperature 温度参数，控制AI回复的随机性，范围0.0-2.0，默认1.0
     * @param imageUrl 图片URL
     * @return 流式响应（SSE事件流）
     */
    Flux<String> sendMessageWithImageStream(Long userId, String userMessage, Integer conversationType, Long conversationId, Double temperature, String imageUrl);
    
    /**
     * 获取对话详情
     * @param conversationId 对话ID
     * @return 包含对话详情的结果
     */
    Result<?> getConversationDetail(Long conversationId);
    
    /**
     * 创建新的对话
     * @param userId 用户ID
     * @param conversationType 对话类型
     * @param title 对话标题
     * @return 包含新对话信息的结果
     */
    Result<?> createConversation(Long userId, Integer conversationType, String title);
    
    /**
     * 结束对话
     * @param conversationId 对话ID
     * @return 操作结果
     */
    Result<?> endConversation(Long conversationId);
    
    /**
     * 获取用户的所有对话列表
     * @param userId 用户ID
     * @return 包含对话列表的结果
     */
    Result<?> getUserConversations(Long userId);

    /**
     * 更新对话标题
     * @param userId 用户ID
     * @param conversationId 对话ID
     * @param newTitle 新标题
     * @return 操作结果
     */
    Result<?> updateConversationTitle(Long userId, Long conversationId, String newTitle);
}
