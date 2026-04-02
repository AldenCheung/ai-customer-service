package com.ai.customerservice.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.zhipu.ZhipuAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangchainConfig {

    @Value("${langchain4j.zhipu-ai.api-key}")
    private String apiKey;

    @Value("${langchain4j.zhipu-ai.chat-model.model-name:glm-4-flash}")
    private String chatModelName;

    @Value("${langchain4j.zhipu-ai.chat-model.temperature:0.7}")
    private Double chatTemperature;

    @Value("${langchain4j.zhipu-ai.chat-model.max-tokens:2048}")
    private Integer chatMaxTokens;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return ZhipuAiChatModel.builder()
                .apiKey(apiKey)
                .model(chatModelName)
                .temperature(chatTemperature)
                .maxToken(chatMaxTokens)
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return ZhipuAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .model(chatModelName)
                .temperature(chatTemperature)
                .maxToken(chatMaxTokens)
                .callTimeout(Duration.ofSeconds(60))
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
