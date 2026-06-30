package com.ai.customerservice.service.impl;

import com.ai.customerservice.config.AuthProperties;
import com.ai.customerservice.dal.ChatHistory;
import com.ai.customerservice.dal.ChatHistoryDao;
import com.ai.customerservice.dal.ChatMessageDao;
import com.ai.customerservice.model.ChatRequest;
import com.ai.customerservice.model.ChatResponse;
import com.ai.customerservice.service.AuthService;
import com.ai.customerservice.service.ChatService;
import com.ai.customerservice.service.CustomerServiceAgent;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class LangchainChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(LangchainChatService.class);

    private final CustomerServiceAgent customerServiceAgent;
    private final ChatHistoryDao chatHistoryDao;
    private final ChatMessageDao chatMessageDao;
    private final AuthService authService;
    private final AuthProperties authProperties;

    @Value("${app.chat.sse-timeout:300000}")
    private long sseTimeout;

    @Value("${app.chat.max-tool-calls:10}")
    private int maxToolCalls;

    public LangchainChatService(CustomerServiceAgent customerServiceAgent,
                                ChatHistoryDao chatHistoryDao,
                                ChatMessageDao chatMessageDao,
                                AuthService authService,
                                AuthProperties authProperties) {
        this.customerServiceAgent = customerServiceAgent;
        this.chatHistoryDao = chatHistoryDao;
        this.chatMessageDao = chatMessageDao;
        this.authService = authService;
        this.authProperties = authProperties;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        log.info("Chat request - sessionId: {}, message: {}", sessionId, request.getMessage());

        ensureChatHistory(sessionId);

        String toolLimitMsg = checkToolCallLimit(sessionId);
        if (toolLimitMsg != null) {
            ChatResponse response = new ChatResponse();
            response.setAnswer(toolLimitMsg);
            response.setSessionId(sessionId);
            response.setTimestamp(LocalDateTime.now());
            return response;
        }

        String answer = customerServiceAgent.chat(sessionId, request.getMessage());
        log.info("Chat answer - sessionId: {}, message: {}", sessionId, answer);

        checkTransferredToHuman(sessionId, answer);

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

        ensureChatHistory(sessionId);

        SseEmitter emitter = new SseEmitter(sseTimeout);

        String toolLimitMsg = checkToolCallLimit(sessionId);
        if (toolLimitMsg != null) {
            try {
                emitter.send(SseEmitter.event().data(toolLimitMsg));
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        emitter.onTimeout(() -> log.warn("SSE connection timed out for session: {}", sessionId));
        emitter.onError(e -> log.error("SSE error for session: {}", sessionId, e));

        StringBuilder fullAnswer = new StringBuilder();

        customerServiceAgent.chatStream(sessionId, request.getMessage())
                .onNext(token -> {
                    fullAnswer.append(token);
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        log.error("Failed to send SSE event", e);
                        emitter.completeWithError(e);
                    }
                })
                .onComplete(response -> {
                    checkTransferredToHuman(sessionId, fullAnswer.toString());
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

    private void ensureChatHistory(String sessionId) {
        if (chatHistoryDao.findBySessionId(sessionId).isPresent()) {
            return;
        }
        String username = resolveCurrentUsername();
        ChatHistory history = new ChatHistory();
        history.setUsername(username != null ? username : authProperties.getDefaultUsername());
        history.setSessionId(sessionId);
        history.setComplaintTime(LocalDateTime.now());
        history.setComplaintStatus("未闭环");
        history.setTransferredToHuman(false);
        chatHistoryDao.insert(history);
        log.info("Created ChatHistory for session: {}, username: {}", sessionId, username);
    }

    private void checkTransferredToHuman(String sessionId, String answer) {
        if (answer != null && answer.contains("666666")) {
            chatHistoryDao.findBySessionId(sessionId).ifPresent(history -> {
                if (!history.isTransferredToHuman()) {
                    chatHistoryDao.updateTransferredToHuman(history.getId(), true);
                    log.info("Marked session {} as transferred to human", sessionId);
                }
            });
        }
    }

    private String checkToolCallLimit(String sessionId) {
        int toolCallCount = chatMessageDao.countToolCallsBySessionId(sessionId);
        if (toolCallCount >= maxToolCalls) {
            log.warn("Session {} reached max tool call limit: {}", sessionId, maxToolCalls);
            return "抱歉，当前会话的工具调用次数已达上限，建议您联系人工客服（电话：666666）获取进一步帮助。";
        }
        return null;
    }

    private String resolveCurrentUsername() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest httpRequest = attrs.getRequest();
        String token = extractToken(httpRequest);
        if (token == null) {
            return null;
        }
        return authService.resolveUsername(token).orElse(null);
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AuthService.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String resolveSessionId(String sessionId) {
        return (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();
    }
}
