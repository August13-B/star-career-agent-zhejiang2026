package org.example.web.controller;

import java.util.Map;

import org.example.web.entity.Result;
import org.example.web.service.AIConversationService;
import org.example.web.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai-conversation")
public class AiConversationController {
    @Autowired
    private AIConversationService aiConversationService;

    @Autowired
    private AIService aiService;

    /**
     * 获取特定对话的详细历史（按对话ID）
     */
    @GetMapping("/history/{conversationId}")
    @CrossOrigin
    public Result<?> getConversationHistory(@PathVariable Long conversationId) {
        return aiConversationService.getConversationHistory(conversationId);
    }

    /**
     * 发送消息给AI并获取回复（同步）
     */
    @PostMapping("/send")
    @CrossOrigin
    public Result<?> sendMessage(@RequestBody Map<String, Object> request) {
        Long userId = request.get("user_id") instanceof Number
                ? ((Number) request.get("user_id")).longValue()
                : Long.parseLong(request.get("user_id").toString());
        String content = (String) request.get("content");
        Integer conversationType = (Integer) request.get("conversation_type");
        if (conversationType == null) {
            conversationType = 1; // 默认职业规划咨询
        }
        // 处理conversationId参数（可选）
        Long conversationId = null;
        if (request.get("conversation_id") != null) {
            conversationId = request.get("conversation_id") instanceof Number
                    ? ((Number) request.get("conversation_id")).longValue()
                    : Long.parseLong(request.get("conversation_id").toString());
        }
        // 处理temperature参数（可选）
        Double temperature = null;
        if (request.get("temperature") != null) {
            if (request.get("temperature") instanceof Number) {
                temperature = ((Number) request.get("temperature")).doubleValue();
            } else if (request.get("temperature") instanceof String) {
                try {
                    temperature = Double.parseDouble((String) request.get("temperature"));
                } catch (NumberFormatException e) {
                    // 忽略格式错误，使用默认值
                }
            }
        }
        // 如果temperature为null，调用无temperature参数的方法（内部会使用默认值1.0）
        if (temperature == null) {
            return aiConversationService.sendMessage(userId, content, conversationType, conversationId);
        } else {
            return aiConversationService.sendMessage(userId, content, conversationType, conversationId, temperature);
        }
    }

    /**
     * 流式发送消息给AI（SSE）
     */
    @PostMapping(value = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    public Flux<String> sendMessageStream(@RequestBody Map<String, Object> request) {
        Long userId = request.get("user_id") instanceof Number
                ? ((Number) request.get("user_id")).longValue()
                : Long.parseLong(request.get("user_id").toString());
        String content = (String) request.get("content");
        Integer conversationType = (Integer) request.get("conversation_type");
        if (conversationType == null) {
            conversationType = 1;
        }
        Long conversationId = null;
        if (request.get("conversation_id") != null) {
            conversationId = request.get("conversation_id") instanceof Number
                    ? ((Number) request.get("conversation_id")).longValue()
                    : Long.parseLong(request.get("conversation_id").toString());
        }
        // 处理temperature参数（可选）
        Double temperature = null;
        if (request.get("temperature") != null) {
            if (request.get("temperature") instanceof Number) {
                temperature = ((Number) request.get("temperature")).doubleValue();
            } else if (request.get("temperature") instanceof String) {
                try {
                    temperature = Double.parseDouble((String) request.get("temperature"));
                } catch (NumberFormatException e) {
                    // 忽略格式错误，使用默认值
                }
            }
        }
        // 如果temperature为null，调用无temperature参数的方法（内部会使用默认值1.0）
        if (temperature == null) {
            return aiConversationService.sendMessageStream(userId, content, conversationType, conversationId);
        } else {
            return aiConversationService.sendMessageStream(userId, content, conversationType, conversationId, temperature);
        }
    }

    /**
     * 发送带图片的消息给AI并获取回复（同步）
     */
    @PostMapping("/send-with-image")
    @CrossOrigin
    public Result<?> sendMessageWithImage(@RequestBody Map<String, Object> request) {
        Long userId = request.get("user_id") instanceof Number
                ? ((Number) request.get("user_id")).longValue()
                : Long.parseLong(request.get("user_id").toString());
        String content = (String) request.get("content");
        String imageUrl = (String) request.get("image_url");
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Result.error("图片URL不能为空");
        }
        Integer conversationType = (Integer) request.get("conversation_type");
        if (conversationType == null) {
            conversationType = 1; // 默认职业规划咨询
        }
        // 处理conversationId参数（可选）
        Long conversationId = null;
        if (request.get("conversation_id") != null) {
            conversationId = request.get("conversation_id") instanceof Number
                    ? ((Number) request.get("conversation_id")).longValue()
                    : Long.parseLong(request.get("conversation_id").toString());
        }
        // 处理temperature参数（可选）
        Double temperature = null;
        if (request.get("temperature") != null) {
            if (request.get("temperature") instanceof Number) {
                temperature = ((Number) request.get("temperature")).doubleValue();
            } else if (request.get("temperature") instanceof String) {
                try {
                    temperature = Double.parseDouble((String) request.get("temperature"));
                } catch (NumberFormatException e) {
                    // 忽略格式错误，使用默认值
                }
            }
        }
        // 调用带图片的服务方法，如果temperature为null，则使用默认值0.7（带图片对话通常使用较低温度）
        if (temperature == null) {
            temperature = 0.7;
        }
        return aiConversationService.sendMessageWithImage(userId, content, conversationType, conversationId, temperature, imageUrl);
    }

    /**
     * 流式发送带图片的消息给AI（SSE）
     */
    @PostMapping(value = "/send-with-image-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    public Flux<String> sendMessageWithImageStream(@RequestBody Map<String, Object> request) {
        Long userId = request.get("user_id") instanceof Number
                ? ((Number) request.get("user_id")).longValue()
                : Long.parseLong(request.get("user_id").toString());
        String content = (String) request.get("content");
        String imageUrl = (String) request.get("image_url");
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Flux.error(new IllegalArgumentException("图片URL不能为空"));
        }
        Integer conversationType = (Integer) request.get("conversation_type");
        if (conversationType == null) {
            conversationType = 1;
        }
        Long conversationId = null;
        if (request.get("conversation_id") != null) {
            conversationId = request.get("conversation_id") instanceof Number
                    ? ((Number) request.get("conversation_id")).longValue()
                    : Long.parseLong(request.get("conversation_id").toString());
        }
        // 处理temperature参数（可选）
        Double temperature = null;
        if (request.get("temperature") != null) {
            if (request.get("temperature") instanceof Number) {
                temperature = ((Number) request.get("temperature")).doubleValue();
            } else if (request.get("temperature") instanceof String) {
                try {
                    temperature = Double.parseDouble((String) request.get("temperature"));
                } catch (NumberFormatException e) {
                    // 忽略格式错误，使用默认值
                }
            }
        }
        // 调用带图片的流式服务方法，如果temperature为null，则使用默认值0.7
        if (temperature == null) {
            temperature = 0.7;
        }
        return aiConversationService.sendMessageWithImageStream(userId, content, conversationType, conversationId, temperature, imageUrl);
    }

    /**
     * 直接调用AI服务器普通对话（无需历史，直接透传）
     */
    @PostMapping("/chat")
    @CrossOrigin
    public Result<?> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        Double temperature = (Double) request.get("temperature");
        if (temperature == null) {
            temperature = 0.7;
        }
        return aiService.chat(message, temperature);
    }

    /**
     * 直接调用AI服务器流式对话（SSE）
     */
    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    public Flux<String> chatStream(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        Double temperature = (Double) request.get("temperature");
        if (temperature == null) {
            temperature = 0.7;
        }
        return aiService.chatStream(message, temperature);
    }

    /**
     * 带图片的对话（直接透传）
     */
    @PostMapping("/chat-with-image")
    @CrossOrigin
    public Result<?> chatWithImage(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        Double temperature = (Double) request.get("temperature");
        if (temperature == null) {
            temperature = 0.7;
        }
        String imageUrl = (String) request.get("image_url");
        return aiService.chatWithImage(message, temperature, imageUrl);
    }

    /**
     * 带图片的流式对话（SSE）
     */
    @PostMapping(value = "/chat-with-image-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    public Flux<String> chatWithImageStream(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        Double temperature = (Double) request.get("temperature");
        if (temperature == null) {
            temperature = 0.7;
        }
        String imageUrl = (String) request.get("image_url");
        return aiService.chatWithImageStream(message, temperature, imageUrl);
    }

    /**
     * 创建新对话
     */
    @PostMapping("/create")
    @CrossOrigin
    public Result<?> createConversation(@RequestBody Map<String, Object> request) {
        Long userId = request.get("user_id") instanceof Number
                ? ((Number) request.get("user_id")).longValue()
                : Long.parseLong(request.get("user_id").toString());
        Integer conversationType = (Integer) request.get("conversation_type");
        String title = (String) request.get("title");
        if (conversationType == null) {
            conversationType = 1;
        }
        if (title == null || title.trim().isEmpty()) {
            title = "新对话";
        }
        return aiConversationService.createConversation(userId, conversationType, title);
    }

    /**
     * 获取用户的所有对话列表
     */
    @GetMapping("/list/{userId}")
    @CrossOrigin
    public Result<?> getUserConversations(@PathVariable Long userId) {
        return aiConversationService.getUserConversations(userId);
    }

    /**
     * 结束对话
     */
    @DeleteMapping("/end/{conversationId}")
    @CrossOrigin
    public Result<?> endConversation(@PathVariable Long conversationId) {
        return aiConversationService.endConversation(conversationId);
    }

    /**
     * 更新对话标题
     * 请求体：userId, conversationId, newTitle
     */
    @PutMapping("/update-title")
    @CrossOrigin
    public Result<?> updateConversationTitle(@RequestBody Map<String, Object> request) {
        // 参数解析
        Long userId;
        Object userIdObj = request.get("user_id");
        if (userIdObj instanceof Number) {
            userId = ((Number) userIdObj).longValue();
        } else if (userIdObj instanceof String) {
            try {
                userId = Long.parseLong((String) userIdObj);
            } catch (NumberFormatException e) {
                return Result.error("用户ID格式错误");
            }
        } else {
            return Result.error("用户ID不能为空");
        }

        Long conversationId;
        Object conversationIdObj = request.get("conversation_id");
        if (conversationIdObj instanceof Number) {
            conversationId = ((Number) conversationIdObj).longValue();
        } else if (conversationIdObj instanceof String) {
            try {
                conversationId = Long.parseLong((String) conversationIdObj);
            } catch (NumberFormatException e) {
                return Result.error("对话ID格式错误");
            }
        } else {
            return Result.error("对话ID不能为空");
        }

        String newTitle = (String) request.get("newTitle");
        if (newTitle == null || newTitle.trim().isEmpty()) {
            return Result.error("新标题不能为空");
        }

        return aiConversationService.updateConversationTitle(userId, conversationId, newTitle);
    }
}
