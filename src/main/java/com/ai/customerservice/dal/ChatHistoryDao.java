package com.ai.customerservice.dal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ChatHistoryDao {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryDao.class);

    private static final String TABLE = "customer_service_chat_history";

    private static final RowMapper<ChatHistory> ROW_MAPPER = (rs, rowNum) -> {
        ChatHistory ch = new ChatHistory();
        ch.setId(rs.getLong("id"));
        ch.setUsername(rs.getString("username"));
        ch.setSessionId(rs.getString("session_id"));
        String time = rs.getString("complaint_time");
        if (time != null) {
            ch.setComplaintTime(LocalDateTime.parse(time.replace(" ", "T")));
        }
        ch.setComplaintStatus(rs.getString("complaint_status"));
        ch.setTransferredToHuman(rs.getInt("transferred_to_human") == 1);
        return ch;
    };

    private final JdbcTemplate jdbcTemplate;

    public ChatHistoryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ChatHistory insert(ChatHistory chatHistory) {
        String sql = "INSERT INTO " + TABLE
                + " (username, session_id, complaint_time, complaint_status, transferred_to_human)"
                + " VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                chatHistory.getUsername(),
                chatHistory.getSessionId(),
                chatHistory.getComplaintTime() != null
                        ? chatHistory.getComplaintTime().toString().replace("T", " ")
                        : null,
                chatHistory.getComplaintStatus(),
                chatHistory.isTransferredToHuman() ? 1 : 0
        );
        Long id = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
        chatHistory.setId(id);
        log.info("Inserted ChatHistory: {}", chatHistory);
        return chatHistory;
    }

    public Optional<ChatHistory> findById(Long id) {
        String sql = "SELECT * FROM " + TABLE + " WHERE id = ?";
        List<ChatHistory> results = jdbcTemplate.query(sql, ROW_MAPPER, id);
        return results.stream().findFirst();
    }

    public List<ChatHistory> findByUsername(String username) {
        String sql = "SELECT * FROM " + TABLE + " WHERE username = ? ORDER BY complaint_time DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, username);
    }

    public Optional<ChatHistory> findBySessionId(String sessionId) {
        String sql = "SELECT * FROM " + TABLE + " WHERE session_id = ?";
        List<ChatHistory> results = jdbcTemplate.query(sql, ROW_MAPPER, sessionId);
        return results.stream().findFirst();
    }

    public List<ChatHistory> findByStatus(String complaintStatus) {
        String sql = "SELECT * FROM " + TABLE + " WHERE complaint_status = ? ORDER BY complaint_time DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, complaintStatus);
    }

    public List<ChatHistory> findAll() {
        String sql = "SELECT * FROM " + TABLE + " ORDER BY complaint_time DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public int updateStatus(Long id, String complaintStatus) {
        String sql = "UPDATE " + TABLE + " SET complaint_status = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, complaintStatus, id);
        log.info("Updated ChatHistory id={} status to {}", id, complaintStatus);
        return rows;
    }

    public int updateTransferredToHuman(Long id, boolean transferredToHuman) {
        String sql = "UPDATE " + TABLE + " SET transferred_to_human = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, transferredToHuman ? 1 : 0, id);
        log.info("Updated ChatHistory id={} transferredToHuman to {}", id, transferredToHuman);
        return rows;
    }

    public int deleteById(Long id) {
        String sql = "DELETE FROM " + TABLE + " WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        log.info("Deleted ChatHistory id={}", id);
        return rows;
    }

    public int countByStatus(String complaintStatus) {
        String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE complaint_status = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, complaintStatus);
        return count != null ? count : 0;
    }

    public int countTransferredToHuman() {
        String sql = "SELECT COUNT(*) FROM " + TABLE + " WHERE transferred_to_human = 1";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    public Map<String, Object> findPage(int page, int size) {
        return findPage(page, size, null, null);
    }

    public Map<String, Object> findPage(int page, int size, String username, String complaintStatus) {
        int offset = (page - 1) * size;

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM " + TABLE + " WHERE 1=1");
        StringBuilder querySql = new StringBuilder("SELECT * FROM " + TABLE + " WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (username != null && !username.isBlank()) {
            countSql.append(" AND username = ?");
            querySql.append(" AND username = ?");
            params.add(username);
        }
        if (complaintStatus != null && !complaintStatus.isBlank()) {
            countSql.append(" AND complaint_status = ?");
            querySql.append(" AND complaint_status = ?");
            params.add(complaintStatus);
        }

        Integer total = jdbcTemplate.queryForObject(countSql.toString(), Integer.class, params.toArray());
        if (total == null) {
            total = 0;
        }
        int totalPages = (int) Math.ceil((double) total / size);

        querySql.append(" ORDER BY complaint_time DESC LIMIT ? OFFSET ?");
        List<Object> queryParams = new java.util.ArrayList<>(params);
        queryParams.add(size);
        queryParams.add(offset);

        List<ChatHistory> records = jdbcTemplate.query(querySql.toString(), ROW_MAPPER, queryParams.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", totalPages);
        return result;
    }
}
