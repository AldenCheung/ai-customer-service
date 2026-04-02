package com.ai.customerservice.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseInitializer.class);

    private final DocumentProcessor documentProcessor;

    @Value("${app.knowledge.path:classpath:knowledge/}")
    private String knowledgePath;

    public KnowledgeBaseInitializer(DocumentProcessor documentProcessor) {
        this.documentProcessor = documentProcessor;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing knowledge base from: {}", knowledgePath);
        try {
            String pattern = knowledgePath.endsWith("/")
                    ? knowledgePath + "*"
                    : knowledgePath + "/*";
            int count = documentProcessor.processDocuments(pattern);
            log.info("Knowledge base initialized successfully. Loaded {} documents.", count);
        } catch (Exception e) {
            log.error("Failed to initialize knowledge base", e);
        }
    }
}
