package com.ai.customerservice.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

    @Value("${app.rag.chroma.url}")
    private String chromaUrl;

    @Value("${app.rag.chroma.collection-name}")
    private String collectionName;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // 创建并返回 Chroma 的 EmbeddingStore 实例
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaUrl)
                .collectionName(collectionName)
                .logRequests(true) // 开发阶段建议打开，方便排查问题
                .logResponses(true)
                .build();
    }
}