package com.ai.customerservice.controller;

import com.ai.customerservice.dal.ChatHistoryDao;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat-history")
public class ChatHistoryController {

    private final ChatHistoryDao chatHistoryDao;

    public ChatHistoryController(ChatHistoryDao chatHistoryDao) {
        this.chatHistoryDao = chatHistoryDao;
    }

    @PostMapping("/resolve")
    public ResponseEntity<Map<String, Object>> resolve(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "sessionId 不能为空"));
        }
        return chatHistoryDao.findBySessionId(sessionId)
                .map(history -> {
                    chatHistoryDao.updateStatus(history.getId(), "已闭环");
                    return ResponseEntity.<Map<String, Object>>ok(Map.of("success", true));
                })
                .orElseGet(() -> ResponseEntity.<Map<String, Object>>status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "未找到该会话的客诉记录")));
    }

    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String complaintStatus) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        Map<String, Object> result = chatHistoryDao.findPage(page, size, username, complaintStatus);
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(@RequestParam String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "sessionId 不能为空"));
        }
        return chatHistoryDao.findBySessionId(sessionId)
                .map(history -> ResponseEntity.<Map<String, Object>>ok(Map.of(
                        "success", true,
                        "complaintStatus", history.getComplaintStatus(),
                        "transferredToHuman", history.isTransferredToHuman()
                )))
                .orElseGet(() -> ResponseEntity.<Map<String, Object>>ok(Map.of(
                        "success", true,
                        "complaintStatus", "无记录",
                        "transferredToHuman", false
                )));
    }
}
