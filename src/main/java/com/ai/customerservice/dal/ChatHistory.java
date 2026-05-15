package com.ai.customerservice.dal;

import java.time.LocalDateTime;

public class ChatHistory {

    private Long id;
    private String username;
    private String sessionId;
    private LocalDateTime complaintTime;
    private String complaintStatus;
    private boolean transferredToHuman;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getComplaintTime() {
        return complaintTime;
    }

    public void setComplaintTime(LocalDateTime complaintTime) {
        this.complaintTime = complaintTime;
    }

    public String getComplaintStatus() {
        return complaintStatus;
    }

    public void setComplaintStatus(String complaintStatus) {
        this.complaintStatus = complaintStatus;
    }

    public boolean isTransferredToHuman() {
        return transferredToHuman;
    }

    public void setTransferredToHuman(boolean transferredToHuman) {
        this.transferredToHuman = transferredToHuman;
    }

    @Override
    public String toString() {
        return "ChatHistory{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", complaintTime=" + complaintTime +
                ", complaintStatus='" + complaintStatus + '\'' +
                ", transferredToHuman=" + transferredToHuman +
                '}';
    }
}
