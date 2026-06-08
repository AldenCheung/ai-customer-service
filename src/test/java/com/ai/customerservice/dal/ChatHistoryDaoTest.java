package com.ai.customerservice.dal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ChatHistoryDaoTest {

    private ChatHistoryDao chatHistoryDao;
    private JdbcTemplate jdbcTemplate;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        String dbName = "testdb_" + System.nanoTime();
        dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName(dbName)
                .addScript("classpath:schema-test.sql")
                .build();
        jdbcTemplate = new JdbcTemplate(dataSource);
        chatHistoryDao = new ChatHistoryDao(jdbcTemplate);
    }

    @AfterEach
    void tearDown() {
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception ignored) {}
        }
    }

    private long insertDirect(String username, String sessionId, String status, boolean transferred) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO customer_service_chat_history (username, session_id, complaint_time, complaint_status, transferred_to_human) VALUES (?, ?, ?, ?, ?)",
                    new String[]{"ID"}
            );
            ps.setString(1, username);
            ps.setString(2, sessionId);
            ps.setString(3, "2026-05-15 10:00:00");
            ps.setString(4, status);
            ps.setInt(5, transferred ? 1 : 0);
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : -1L;
    }

    @Test
    void findById_shouldReturnPresentWhenExists() {
        long id = insertDirect("user1", "sess-002", "未闭环", false);

        Optional<ChatHistory> found = chatHistoryDao.findById(id);

        assertTrue(found.isPresent());
        assertEquals("user1", found.get().getUsername());
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        Optional<ChatHistory> found = chatHistoryDao.findById(99999L);
        assertTrue(found.isEmpty());
    }

    @Test
    void findByUsername_shouldReturnMatchingRecords() {
        insertDirect("alice", "sess-a1", "未闭环", false);
        insertDirect("alice", "sess-a2", "已闭环", true);
        insertDirect("bob", "sess-b1", "未闭环", false);

        List<ChatHistory> results = chatHistoryDao.findByUsername("alice");

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(r -> "alice".equals(r.getUsername())));
    }

    @Test
    void findByUsername_shouldReturnEmptyForNoMatch() {
        List<ChatHistory> results = chatHistoryDao.findByUsername("nonexistent");
        assertTrue(results.isEmpty());
    }

    @Test
    void findBySessionId_shouldReturnPresentWhenExists() {
        insertDirect("user1", "sess-unique", "未闭环", false);

        Optional<ChatHistory> found = chatHistoryDao.findBySessionId("sess-unique");

        assertTrue(found.isPresent());
        assertEquals("sess-unique", found.get().getSessionId());
    }

    @Test
    void findBySessionId_shouldReturnEmptyWhenNotExists() {
        Optional<ChatHistory> found = chatHistoryDao.findBySessionId("nonexistent");
        assertTrue(found.isEmpty());
    }

    @Test
    void findByStatus_shouldReturnMatchingRecords() {
        insertDirect("u1", "s1", "未闭环", false);
        insertDirect("u2", "s2", "已闭环", true);
        insertDirect("u3", "s3", "未闭环", false);

        List<ChatHistory> openList = chatHistoryDao.findByStatus("未闭环");
        List<ChatHistory> closedList = chatHistoryDao.findByStatus("已闭环");

        assertEquals(2, openList.size());
        assertEquals(1, closedList.size());
    }

    @Test
    void findAll_shouldReturnAllRecords() {
        insertDirect("u1", "s1", "未闭环", false);
        insertDirect("u2", "s2", "已闭环", true);

        List<ChatHistory> all = chatHistoryDao.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void updateStatus_shouldUpdateSuccessfully() {
        long id = insertDirect("u1", "s1", "未闭环", false);

        int rows = chatHistoryDao.updateStatus(id, "已闭环");

        assertEquals(1, rows);
        ChatHistory updated = chatHistoryDao.findById(id).orElseThrow();
        assertEquals("已闭环", updated.getComplaintStatus());
    }

    @Test
    void updateStatus_shouldReturnZeroForNonExistentId() {
        int rows = chatHistoryDao.updateStatus(99999L, "已闭环");
        assertEquals(0, rows);
    }

    @Test
    void updateTransferredToHuman_shouldUpdateSuccessfully() {
        long id = insertDirect("u1", "s1", "未闭环", false);

        int rows = chatHistoryDao.updateTransferredToHuman(id, true);

        assertEquals(1, rows);
        ChatHistory updated = chatHistoryDao.findById(id).orElseThrow();
        assertTrue(updated.isTransferredToHuman());
    }

    @Test
    void deleteById_shouldDeleteSuccessfully() {
        long id = insertDirect("u1", "s1", "未闭环", false);

        int rows = chatHistoryDao.deleteById(id);

        assertEquals(1, rows);
        assertTrue(chatHistoryDao.findById(id).isEmpty());
    }

    @Test
    void deleteById_shouldReturnZeroForNonExistentId() {
        int rows = chatHistoryDao.deleteById(99999L);
        assertEquals(0, rows);
    }

    @Test
    void countByStatus_shouldReturnCorrectCount() {
        insertDirect("u1", "s1", "未闭环", false);
        insertDirect("u2", "s2", "已闭环", true);
        insertDirect("u3", "s3", "未闭环", false);

        assertEquals(2, chatHistoryDao.countByStatus("未闭环"));
        assertEquals(1, chatHistoryDao.countByStatus("已闭环"));
    }

    @Test
    void countTransferredToHuman_shouldReturnCorrectCount() {
        insertDirect("u1", "s1", "未闭环", true);
        insertDirect("u2", "s2", "已闭环", true);
        insertDirect("u3", "s3", "未闭环", false);

        assertEquals(2, chatHistoryDao.countTransferredToHuman());
    }

    @Test
    void findPage_simple_shouldReturnPaginatedResults() {
        for (int i = 0; i < 15; i++) {
            insertDirect("user" + i, "sess" + i, "未闭环", false);
        }

        Map<String, Object> page1 = chatHistoryDao.findPage(1, 10);
        assertEquals(15, page1.get("total"));
        assertEquals(1, page1.get("page"));
        assertEquals(10, page1.get("size"));
        assertEquals(2, page1.get("totalPages"));
        @SuppressWarnings("unchecked")
        List<ChatHistory> records1 = (List<ChatHistory>) page1.get("records");
        assertEquals(10, records1.size());

        Map<String, Object> page2 = chatHistoryDao.findPage(2, 10);
        @SuppressWarnings("unchecked")
        List<ChatHistory> records2 = (List<ChatHistory>) page2.get("records");
        assertEquals(5, records2.size());
    }

    @Test
    void findPage_withUsernameFilter_shouldReturnFilteredResults() {
        insertDirect("alice", "s1", "未闭环", false);
        insertDirect("bob", "s2", "未闭环", false);
        insertDirect("alice", "s3", "已闭环", true);

        Map<String, Object> result = chatHistoryDao.findPage(1, 10, "alice", null);

        assertEquals(2, result.get("total"));
        @SuppressWarnings("unchecked")
        List<ChatHistory> records = (List<ChatHistory>) result.get("records");
        assertTrue(records.stream().allMatch(r -> "alice".equals(r.getUsername())));
    }

    @Test
    void findPage_withStatusFilter_shouldReturnFilteredResults() {
        insertDirect("u1", "s1", "未闭环", false);
        insertDirect("u2", "s2", "已闭环", true);
        insertDirect("u3", "s3", "未闭环", false);

        Map<String, Object> result = chatHistoryDao.findPage(1, 10, null, "已闭环");

        assertEquals(1, result.get("total"));
    }

    @Test
    void findPage_withBothFilters_shouldReturnFilteredResults() {
        insertDirect("alice", "s1", "未闭环", false);
        insertDirect("alice", "s2", "已闭环", true);
        insertDirect("bob", "s3", "未闭环", false);

        Map<String, Object> result = chatHistoryDao.findPage(1, 10, "alice", "已闭环");

        assertEquals(1, result.get("total"));
    }

    @Test
    void findPage_emptyResult_shouldReturnZeroTotal() {
        Map<String, Object> result = chatHistoryDao.findPage(1, 10, "nobody", null);

        assertEquals(0, result.get("total"));
        assertEquals(0, ((List<?>) result.get("records")).size());
    }
}
