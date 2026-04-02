package com.ai.customerservice.service.impl;

import com.ai.customerservice.model.ChatRequest;
import com.ai.customerservice.model.ChatResponse;
import com.ai.customerservice.service.ChatService;
import com.ai.customerservice.service.CustomerServiceAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LangchainChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(LangchainChatService.class);

    private final CustomerServiceAgent customerServiceAgent;

    @Value("${app.chat.sse-timeout:300000}")
    private long sseTimeout;

    public LangchainChatService(CustomerServiceAgent customerServiceAgent) {
        this.customerServiceAgent = customerServiceAgent;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        log.info("Chat request - sessionId: {}, message: {}", sessionId, request.getMessage());

        String answer = customerServiceAgent.chat(sessionId, request.getMessage());

        ChatResponse response = new ChatResponse();
        response.setAnswer(answer);
        response.setSessionId(sessionId);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    @Override
    public SseEmitter chatStream(ChatRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        log.info("Stream chat request - sessionId: {}, message: {}", sessionId, request.getMessage());

        SseEmitter emitter = new SseEmitter(sseTimeout);

        emitter.onTimeout(() -> log.warn("SSE connection timed out for session: {}", sessionId));
        emitter.onError(e -> log.error("SSE error for session: {}", sessionId, e));

        customerServiceAgent.chatStream(sessionId, request.getMessage())
                .onNext(token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        log.error("Failed to send SSE event", e);
                        emitter.completeWithError(e);
                    }
                })
                .onComplete(response -> {
                    try {
                        emitter.send(SseEmitter.event().data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        log.error("Failed to complete SSE", e);
                    }
                })
                .onError(e -> {
                    log.error("LLM streaming error for session: {}", sessionId, e);
                    emitter.completeWithError(e);
                })
                .start();

        return emitter;
    }

    private String resolveSessionId(String sessionId) {
        return (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();
    }
}
