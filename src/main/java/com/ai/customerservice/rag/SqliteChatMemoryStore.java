package com.ai.customerservice.rag;

import com.ai.customerservice.dal.ChatMessageDao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SqliteChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(SqliteChatMemoryStore.class);
    private static final String TOOL_CALLS_PREFIX = "[TOOL_CALLS]";
    private static final String TOOL_RESULT_PREFIX = "[TOOL_RESULT]";
    private final ChatMessageDao chatMessageDao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SqliteChatMemoryStore(ChatMessageDao chatMessageDao) {
        this.chatMessageDao = chatMessageDao;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        List<Map<String, String>> rows = chatMessageDao.findBySessionId(sessionId);
        List<ChatMessage> messages = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String role = row.get("role");
            String content = row.get("content");
            switch (role) {
                case "user" -> messages.add(UserMessage.from(content));
                case "ai" -> {
                    if (content.startsWith(TOOL_CALLS_PREFIX)) {
                        String json = content.substring(TOOL_CALLS_PREFIX.length());
                        try {
                            List<ToolExecutionRequest> requests = objectMapper.readValue(json,
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, ToolExecutionRequest.class));
                            messages.add(AiMessage.from(requests));
                        } catch (JsonProcessingException e) {
                            log.warn("Failed to deserialize tool calls, falling back to text", e);
                            messages.add(AiMessage.from(content));
                        }
                    } else {
                        messages.add(AiMessage.from(content));
                    }
                }
                case "tool_result" -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, String> data = objectMapper.readValue(content, Map.class);
                        String toolName = data.get("toolName");
                        String toolId = data.get("id");
                        String result = data.get("result");
                        messages.add(ToolExecutionResultMessage.from(toolName, toolId, result));
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize tool result", e);
                    }
                }
                case "system" -> messages.add(SystemMessage.from(content));
            }
        }
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String sessionId = memoryId.toString();
        List<Map<String, String>> rows = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg instanceof UserMessage userMsg) {
                rows.add(Map.of("role", "user", "content", userMsg.singleText()));
            } else if (msg instanceof AiMessage aiMsg) {
                if (aiMsg.hasToolExecutionRequests()) {
                    try {
                        String json = objectMapper.writeValueAsString(aiMsg.toolExecutionRequests());
                        rows.add(Map.of("role", "ai", "content", TOOL_CALLS_PREFIX + json));
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to serialize tool calls", e);
                    }
                } else if (aiMsg.text() != null) {
                    rows.add(Map.of("role", "ai", "content", aiMsg.text()));
                }
            } else if (msg instanceof ToolExecutionResultMessage toolResult) {
                try {
                    String json = objectMapper.writeValueAsString(
                            Map.of("toolName", toolResult.toolName() != null ? toolResult.toolName() : "",
                                    "id", toolResult.id() != null ? toolResult.id() : "",
                                    "result", toolResult.text()));
                    rows.add(Map.of("role", "tool_result", "content", json));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize tool result", e);
                }
            } else if (msg instanceof SystemMessage sysMsg) {
                rows.add(Map.of("role", "system", "content", sysMsg.text()));
            }
        }
        chatMessageDao.replaceMessages(sessionId, rows);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        chatMessageDao.deleteBySessionId(memoryId.toString());
    }
}
