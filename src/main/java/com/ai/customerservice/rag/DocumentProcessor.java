package com.ai.customerservice.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentProcessor {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessor.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    @Value("${app.rag.chunk-size:300}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap:30}")
    private int chunkOverlap;

    public DocumentProcessor(EmbeddingStore<TextSegment> embeddingStore,
                             EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public int processDocuments(String resourcePattern) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(resourcePattern);

        List<Document> documents = new ArrayList<>();
        DocumentParser parser = new TextDocumentParser();

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename != null && (filename.endsWith(".txt") || filename.endsWith(".md"))) {
                try {
                    Document doc = parser.parse(resource.getInputStream());
                    doc.metadata().put("source", filename);
                    documents.add(doc);
                    log.info("Loaded document: {}", filename);
                } catch (Exception e) {
                    log.warn("Failed to load document: {}", filename, e);
                }
            }
        }

        if (documents.isEmpty()) {
            log.warn("No documents found matching pattern: {}", resourcePattern);
            return 0;
        }

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(chunkSize, chunkOverlap))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(documents);
        log.info("Successfully ingested {} documents into embedding store", documents.size());
        return documents.size();
    }
}
