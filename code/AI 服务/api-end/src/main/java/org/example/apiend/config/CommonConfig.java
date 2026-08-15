package org.example.apiend.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.example.apiend.utils.LoadExcelDocuments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CommonConfig {

    @Autowired
    private OpenAiChatModel model;

    @Autowired
    private EmbeddingModel embeddingModel;

    // PGVector 向量存储
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host("localhost")
                .port(5432)
                .database("langchain_rag")
                .user("postgres")
                .password("postgres")
                .table("embedding_knowledge_base")
                .dimension(1024)
                .dropTableFirst(false)
                .createTable(false)
                .build();
    }

    // ===================== 会话记忆 =====================
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

//    @Bean
//    public ChatMemoryProvider chatMemoryProvider() {
//        return conversation_id -> MessageWindowChatMemory.builder()
//                .id(conversation_id)
//                .maxMessages(20)
//                .build();
//    }

    // 导入数据库
//    @Bean
//    public EmbeddingStore store(EmbeddingStore<TextSegment> embeddingStore) {
//        List<Document> documents = LoadExcelDocuments.loadAllExcelWithDynamicHeader();
//        DocumentSplitter noSplit = segment -> List.of(segment.toTextSegment());
//
//        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
//                .embeddingStore(embeddingStore)
//                .documentSplitter(noSplit)
//                .embeddingModel(embeddingModel)
//                .build();
//
//        try {
//            ingestor.ingest(documents);
//            System.out.println("✅ 数据导入Docker PGVector成功，共 " + documents.size() + " 条");
//        } catch (Exception e) {
//            System.err.println("❌ 导入失败：" + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return embeddingStore;
//    }

    // ===================== RAG 检索器 =====================
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .minScore(0.7)
                .maxResults(15)
                .build();
    }
}