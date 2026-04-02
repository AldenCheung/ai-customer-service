package com.ai.customerservice.model;

import java.time.LocalDateTime;

public class ChatResponse {

    private String answer;
    private String sessionId;
    private LocalDateTime timestamp;

    public ChatResponse() {
    }

    public ChatResponse(String answer, String sessionId, LocalDateTime timestamp) {
        this.answer = answer;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
