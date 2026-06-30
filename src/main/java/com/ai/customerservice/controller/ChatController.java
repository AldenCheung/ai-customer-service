package com.ai.customerservice.controller;

import com.ai.customerservice.model.ChatRequest;
import com.ai.customerservice.model.ChatResponse;
import com.ai.customerservice.rag.DocumentProcessor;
import com.ai.customerservice.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final DocumentProcessor documentProcessor;

    public ChatController(ChatService chatService, DocumentProcessor documentProcessor) {
        this.chatService = chatService;
        this.documentProcessor = documentProcessor;
    }

    @GetMapping("/hello")
    public Map<String, String> hello() {
        try {
            String content = Files.readString(Path.of("Hello.txt"));
            return Map.of("greeting", content.trim());
        } catch (IOException e) {
            return Map.of("greeting", "您好，欢迎咨询！");
        }
    }

    @GetMapping("/knowledge")
    public List<Map<String, Object>> knowledge() {
        try {
            Path knowledgeDir = Path.of("knowledge");
            return Files.list(knowledgeDir)
                    .filter(Files::isRegularFile)
                    .map(p -> {
                        try {
                            return Map.<String, Object>of(
                                    "name", p.getFileName().toString(),
                                    "size", Files.size(p),
                                    "lastModified", Files.getLastModifiedTime(p).toMillis()
                            );
                        } catch (IOException e) {
                            return Map.<String, Object>of("name", p.getFileName().toString(), "size", 0L, "lastModified", 0L);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    @GetMapping("/knowledge/{filename}")
    public ResponseEntity<String> knowledgeFile(@PathVariable String filename) {
        try {
            Path filePath = Path.of("knowledge").resolve(filename).normalize();
            Path knowledgeDir = Path.of("knowledge").normalize();
            if (!filePath.startsWith(knowledgeDir) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            String content = Files.readString(filePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/knowledge/reload")
    public Map<String, Object> reloadKnowledge() {
        try {
            int count = documentProcessor.processDocuments("file:knowledge/*");
            return Map.of("success", true, "count", count);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    //对话接口
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String model) {
        ChatRequest request = new ChatRequest(message, sessionId);
        return chatService.chatStream(request);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "ai-customer-service");
    }
}
