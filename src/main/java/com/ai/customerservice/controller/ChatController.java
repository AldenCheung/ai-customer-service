package com.ai.customerservice.controller;

import com.ai.customerservice.model.ChatRequest;
import com.ai.customerservice.model.ChatResponse;
import com.ai.customerservice.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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
