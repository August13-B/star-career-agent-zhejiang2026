package org.example.apiend.AIService;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.*;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import opennlp.tools.stemmer.Stemmer;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.AUTOMATIC,//动态代理,手动
        chatModel = "openAiChatModel",//指定模型
        streamingChatModel = "openAiStreamingChatModel",
        //chatMemory = "chatMemory",
        //chatMemoryProvider = "chatMemoryProvider",//会话记忆返回对象
        contentRetriever = "contentRetriever"//向量库检索对象
)

public interface AIChatService {
    @SystemMessage(fromResource = "System.txt")
    @UserMessage("{{msg}}")
    public String chat(@V("msg") String message/*@MemoryId*/);

    @SystemMessage(fromResource = "System.txt")
    @UserMessage("{{msg}} ")
    public Flux<String> chat_with_flux(@V("msg") String message/*@MemoryId*/);

    @SystemMessage(fromResource = "System.txt")
    @UserMessage("{{msg}}")
    public Flux<String> chat_with_picture_flux(@V("msg") dev.langchain4j.data.message.UserMessage userMessage);

    @SystemMessage(fromResource = "System.txt")
    @UserMessage("{{msg}}")
    public String chat_with_picture(@V("msg") dev.langchain4j.data.message.UserMessage userMessage);
}


