package com.ai.customerservice.dal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class ChatMessageDao {

    private static final String TABLE = "chat_message";

    private final JdbcTemplate jdbcTemplate;

    public ChatMessageDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, String>> findBySessionId(String sessionId) {
        String sql = "SELECT role, content FROM " + TABLE + " WHERE session_id = ? ORDER BY id ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                Map.of("role", rs.getString("role"), "content", rs.getString("content")),
                sessionId);
    }

    public void insert(String sessionId, String role, String content) {
        String sql = "INSERT INTO " + TABLE + " (session_id, role, content) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, sessionId, role, content);
    }

    public void deleteBySessionId(String sessionId) {
        String sql = "DELETE FROM " + TABLE + " WHERE session_id = ?";
        jdbcTemplate.update(sql, sessionId);
    }

    public void replaceMessages(String sessionId, List<Map<String, String>> messages) {
        deleteBySessionId(sessionId);
        String sql = "INSERT INTO " + TABLE + " (session_id, role, content) VALUES (?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<>();
        for (Map<String, String> msg : messages) {
            batchArgs.add(new Object[]{sessionId, msg.get("role"), msg.get("content")});
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    public int countToolCallsBySessionId(String sessionId) {
        String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE session_id = ? AND role = 'tool_result'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, sessionId);
        return count != null ? count : 0;
    }
}
