package com.ai.customerservice.config;

import com.ai.customerservice.service.CustomerServiceAgent;
import com.ai.customerservice.service.tool.CalculatorService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Value("${app.rag.max-results:3}")
    private int maxResults;

    @Value("${app.rag.min-score:0.5}")
    private double minScore;

    @Value("${app.chat.memory-max-messages:20}")
    private int memoryMaxMessages;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    public EmbeddingStoreContentRetriever contentRetriever(
            EmbeddingStore<TextSegment> embeddingStore,
            EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    @Bean
    public CustomerServiceAgent customerServiceAgent(
            ChatLanguageModel chatLanguageModel,
            StreamingChatLanguageModel streamingChatLanguageModel,
            EmbeddingStoreContentRetriever contentRetriever,
            CalculatorService calculatorService) {
        return AiServices.builder(CustomerServiceAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .contentRetriever(contentRetriever)
                .tools(calculatorService)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(memoryMaxMessages))
                .build();
    }
}
