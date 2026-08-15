package org.example.apiend.controller;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.apiend.AIService.AIChatService;
import org.example.apiend.entity.Result;
import org.example.apiend.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {
    String System = "你是大学生职业规划助手，只处理：学生画像、岗位画像创建、人岗匹配、职业规划交流。输出的消息严禁出现用户个人信息和岗位ID这样的消息\n" +
            "这是模型需要遵守的前置规律【输出铁律：必须 100% 遵守】\n" +
            "你必须严格按用户要求输出 JSON，用户让你输出什么结构，你就输出什么结构。禁止输出 ```json 代码块和markdown格式。\n" +
            "没有特殊说明时，只输出 {\"response\":\"结果内容\"}，所有的单引号在输出时改为双引号\n" +
            "若用户指定 JSON 字段，严格按字段名、类型、结构输出，不增不减\n" +
            "不编造信息，不闲聊无关话题，只做职业规划相关回答，在用户提及其他无关信息包括图片的时候，应该柔性引导用户回到职业规划相关话题\n" +
            "用户如果没有发起疑问或者提供有效信息则不用看‘Answer using the following information’后面的内容\n";
    //    @Autowired
//    private OpenAiChatModel model;
//    @GetMapping("/chat")
//    public String chat(String message){
//        String result = model.chat(message);
//        return result;
//    }
    @Resource
    private OpenAiChatModel openAiChatModel;
    @Resource
    private OpenAiStreamingChatModel openAiStreamingChatModel;
    @Autowired
    private AIChatService aiChatService;
    @Autowired
    private JsonUtils jsonUtils;
    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.base-url:https://api.openai.com/}")
    private String baseUrl;


    //private final ChatMemoryProvider chatMemoryProvider;
    private final ContentRetriever contentRetriever;


    // 构造函数注入（绝对不报错！）
    public ChatController(
            //ChatMemoryProvider chatMemoryProvider,
            ContentRetriever contentRetriever,
            OpenAiStreamingChatModel openAiStreamingChatModel
    ) {
        //this.chatMemoryProvider = chatMemoryProvider;
        this.contentRetriever = contentRetriever;
        this.openAiStreamingChatModel = openAiStreamingChatModel;
    }

    // ===================== 流式对话（POST + JSON请求体）=====================
    // ===================== 流式对话 =====================
    @PostMapping(value = "/chat_with_flux", produces = "text/html;charset=utf-8")
    public SseEmitter chat_with_flux(@RequestBody Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter();

        // 直接从body取，不调用任何getter方法
        String message = (String) body.get("message");
        Double temperature = (Double) body.get("temperature");

        OpenAiChatModel dynamicModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(Double.valueOf(temperature))
                .logRequests(true)   // ← 添加这行
                .logResponses(true)
                .build();

        // 手动构建 AI 代理（保留RAG、记忆、全部功能）
        AIChatService aiChatService = AiServices.builder(AIChatService.class)
                .chatModel(dynamicModel)
                //.chatMemoryProvider(chatMemoryProvider)
                .streamingChatModel(openAiStreamingChatModel)
                .contentRetriever(contentRetriever)
                //.tools("aIService")
                .build();
        Flux<String> flux = aiChatService.chat_with_flux(message);

        flux.subscribe(
                content -> {
                    try {
                        emitter.send(Result.success(content));
                    } catch (Exception e) {
                        emitter.complete();
                    }
                },
                error -> emitter.completeWithError(error),
                () -> emitter.complete()
        );

        return emitter;
    }

    @PostMapping(value = "/chat_with_flux_without_temperature", produces = "text/html;charset=utf-8")
    public SseEmitter chat_with_flux_without_temperature(@RequestBody Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter();

        // 直接从body取，不调用任何getter方法
        String message = (String) body.get("message");

        Flux<String> flux = aiChatService.chat_with_flux(message);

        flux.subscribe(
                content -> {
                    try {
                        emitter.send(Result.success(content));
                    } catch (Exception e) {
                        emitter.complete();
                    }
                },
                error -> emitter.completeWithError(error),
                () -> emitter.complete()
        );

        return emitter;
    }

    // ===================== 普通对话 =====================
    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, Object> body) {
        // 直接从body取
        String message = (String) body.get("message");
        Double temperature = (Double) body.get("temperature");

        OpenAiChatModel dynamicModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(Double.valueOf(temperature))
                .logRequests(true)   // ← 添加这行
                .logResponses(true)
                .build();
        // 手动构建 AI 代理（保留RAG、记忆、全部功能）
        AIChatService aiChatService = AiServices.builder(AIChatService.class)
                .chatModel(dynamicModel)
                //.chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .build();

        String result1 = aiChatService.chat(message);
        Map<String, Object> data = jsonUtils.strToObj(result1.toString(), Map.class);
        return Result.success(data);
    }

    @PostMapping("/chat_with_picture_with_RAG")
    public Result chat_with_picture_with_RAG(@RequestBody Map<String, Object> body) {

        // 1. 参数获取
        String message = (String) body.get("message");
        Double temperature = (Double) body.getOrDefault("temperature", 0.7);
        // String memoryId = (String) body.get("conversation_id"); // 已删除
        String image_url = (String) body.get("image_url");

        List<Content> contents = contentRetriever.retrieve(
                Query.from(message)
        );

        // 拼接知识库内容
        StringBuilder knowledge = new StringBuilder();
        for (var content : contents) {
            knowledge.append(content.textSegment().text()).append("\n");
        }

        // ===================== 对话记忆 =====================
        //ChatMemory chatMemory = chatMemoryProvider.get(memoryId);
        //List<ChatMessage> history = chatMemory.messages();

        // ===================== 拼接最终问题 =====================
        String finalMessage = "根据以下知识回答问题：\n" + knowledge + "\n用户问题：" + message;
        UserMessage userMessage = UserMessage.from(
                TextContent.from(finalMessage),
                ImageContent.from(image_url)
        );
        dev.langchain4j.data.message.SystemMessage system = SystemMessage.from(System);
        List<ChatMessage> messages = List.of(system, userMessage);

        // ===================== 构建消息列表（历史 + 当前） =====================
        //List<ChatMessage> allMessages = new ArrayList<>(history);
        //allMessages.add(userMessage);

        // ===================== 构建请求（1.0.1-beta6 正确API） =====================
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)  // 正确方法：messages，不是 message
                .temperature(temperature)
                .build();

        // ===================== 调用AI =====================
        ChatResponse chatResponse = openAiChatModel.chat(chatRequest);
        AiMessage aiMessage = chatResponse.aiMessage();

        // ===================== 保存记忆 =====================
        //chatMemory.add(userMessage);
        //chatMemory.add(aiMessage);
        Map<String, Object> data = jsonUtils.strToObj(aiMessage.text(), Map.class);
        // ===================== 返回 =====================
        return Result.success(data);
    }

        @PostMapping(value = "/chat_with_picture_with_RAG_stream", produces = "text/html;charset=utf-8")
        public SseEmitter chat_with_picture_with_RAG_stream(@RequestBody Map<String, Object> body) {
            SseEmitter emitter = new SseEmitter();

            try {
                String message = (String) body.get("message");
                Double temperature = (Double) body.getOrDefault("temperature", 0.7);
                String imageUrl = (String) body.get("image_url");

                // RAG 检索
                List<Content> contents = contentRetriever.retrieve(Query.from(message));
                String knowledge = contents.stream()
                        .map(c -> c.textSegment().text())
                        .collect(Collectors.joining("\n"));

                String finalMessage = "根据以下知识回答问题：\n" + knowledge + "\n用户问题：" + message;
                UserMessage userMessage = UserMessage.from(
                        TextContent.from(finalMessage),
                        ImageContent.from(imageUrl)
                );
                dev.langchain4j.data.message.SystemMessage system = SystemMessage.from(System);
                List<ChatMessage> messages = List.of(system, userMessage);
                // 构建请求（包含图片和动态 temperature）
                ChatRequest chatRequest = ChatRequest.builder()
                        .messages(messages)
                        .temperature(temperature)
                        .build();

                // 流式调用
                openAiStreamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        try {
                            emitter.send(SseEmitter.event().data(Result.success(partialResponse)));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse completeResponse) {
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        emitter.completeWithError(error);
                    }
                });

            } catch (Exception e) {
                emitter.completeWithError(e);
            }

            return emitter;
        }

    @PostMapping("/chat_without_temperature")
    public Result chat_without_temperature(@RequestBody Map<String, Object> body) {
        // 直接从body取
        String message = (String) body.get("message");

        String result1 = aiChatService.chat(message);
        Map<String, Object> data = jsonUtils.strToObj(result1.toString(), Map.class);
        return Result.success(data);
    }


    @PostMapping("/only_chat")
    public Result only_chat(@RequestBody Map<String, Object> body) {
        // 直接从body取
        String message = (String) body.get("message");
        Double temperature = (Double) body.get("temperature");

        if (temperature == null) {
            temperature = 0.7;
        }

        UserMessage userMessage = UserMessage.from(
                TextContent.from(message)
        );
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage)  // 直接传字符串即可
                .temperature(temperature)  // ✅ 温度设置在这里
                .build();
        ChatResponse chatResponse = openAiChatModel.chat(chatRequest);
        AiMessage aimessage = chatResponse.aiMessage();
        log.info(aimessage.toString());
        String content = aimessage.text();
        return Result.success(content);
    }

    @PostMapping("/chat_with_system")
    public Result chat_with_system(@RequestBody Map<String, Object> body) {
        // 直接从body取
        String message = (String) body.get("message");
        Double temperature = (Double) body.get("temperature");

        UserMessage userMessage = UserMessage.from(
                TextContent.from(message)
        );
        dev.langchain4j.data.message.SystemMessage system = SystemMessage.from(System);
        List<ChatMessage> messages = List.of(system, userMessage);
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)  // 直接传字符串即可
                .temperature(temperature)  // ✅ 温度设置在这里
                .build();
        ChatResponse chatResponse = openAiChatModel.chat(chatRequest);
        AiMessage aimessage = chatResponse.aiMessage();
        log.info(aimessage.toString());
        String content = aimessage.text();
        Map<String, Object> data = jsonUtils.strToObj(content.toString(), Map.class);
        return Result.success(data);
    }
    // ===================== 图片对话 =====================
    @PostMapping("/chat_with_picture")
    public Result chat_with_picture(@RequestBody Map<String, Object> body) {
        // 直接从body取
        String message = (String) body.get("message");
        String imageUrl = (String) body.get("image_url");
        Double temperature = (Double) body.get("temperature");

        UserMessage userMessage = UserMessage.from(
                TextContent.from(message),
                ImageContent.from(imageUrl)
        );
        dev.langchain4j.data.message.SystemMessage system = SystemMessage.from(System);
        List<ChatMessage> messages = List.of(system, userMessage);
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)  // 直接传字符串即可
                .temperature(temperature)  // ✅ 温度设置在这里
                .build();
        ChatResponse chatResponse = openAiChatModel.chat(chatRequest);
        AiMessage aimessage = chatResponse.aiMessage();
        log.info(aimessage.toString());
        String content = aimessage.text();
        Map<String, Object> data = jsonUtils.strToObj(content.toString(), Map.class);
        return Result.success(data);
    }

    String System_basic =
            "这是模型需要遵守的前置规律【输出铁律：必须 100% 遵守】\n" +
                    "你必须严格按用户要求输出 JSON，用户让你输出什么结构，你就输出什么结构。禁止输出 ```json 代码块和markdown格式。\n" +
                    "没有特殊说明时，只输出 {\"response\":\"结果内容\"}，所有的单引号在输出时改为双引号\n" +
                    "若用户指定 JSON 字段，严格按字段名、类型、结构输出，不增不减\n" +
                    "用户输入为 json 数据，其中 content 是 json 结构，注意关注 json 的键名，键值对会传输各种数据和要求";
    @PostMapping("/chat_with_picture_without_system")
    public Result chat_with_picture_without_system(@RequestBody Map<String, Object> body) {
        // 直接从body取
        String message = (String) body.get("message");
        String imageUrl = (String) body.get("image_url");
        Double temperature = (Double) body.get("temperature");
        UserMessage userMessage = UserMessage.from(
                TextContent.from(message),
                ImageContent.from(imageUrl)
        );
        dev.langchain4j.data.message.SystemMessage system = SystemMessage.from(System_basic);
        List<ChatMessage> messages = List.of(system, userMessage);
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(messages)  // 直接传字符串即可
                .temperature(temperature)  // ✅ 温度设置在这里
                .build();
        ChatResponse chatResponse = openAiChatModel.chat(chatRequest);
        AiMessage aimessage = chatResponse.aiMessage();
        log.info(aimessage.toString());
        String content = aimessage.text();
        Map<String, Object> data = jsonUtils.strToObj(content.toString(), Map.class);
        return Result.success(data);
    }

    @PostMapping("/chat_test")
    public Result chat_test(@RequestBody Map<String, Object> body) {
        // 直接从body取
        String message = (String) body.get("message");
        String imageUrl = (String) body.get("image_url");
        UserMessage userMessage = UserMessage.from(
                TextContent.from(message),
                ImageContent.from(imageUrl)
        );
        String result1 = String.valueOf(aiChatService.chat_with_picture(userMessage));
        Map<String, Object> data = jsonUtils.strToObj(result1.toString(), Map.class);
        return Result.success(data);
    }

}