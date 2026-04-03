package com.ai.customerservice.service;

import com.ai.customerservice.model.ChatRequest;
import com.ai.customerservice.model.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
// test push
public interface ChatService {

    ChatResponse chat(ChatRequest request);

    SseEmitter chatStream(ChatRequest request);
}
