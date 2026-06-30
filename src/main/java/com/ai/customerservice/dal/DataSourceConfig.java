package com.ai.customerservice.dal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${app.sqlite.db-path:./data/customer_service.db}")
    private String dbPath;

    @Bean
    public DataSource dataSource() throws Exception {
        Path dbFile = Path.of(dbPath);
        if (!Files.exists(dbFile)) {
            Files.createDirectories(dbFile.getParent());
            Files.createFile(dbFile);
            log.info("Created SQLite database file: {}", dbFile.toAbsolutePath());
        }

        String jdbcUrl = "jdbc:sqlite:" + dbFile.toAbsolutePath();
        org.sqlite.SQLiteDataSource ds = new org.sqlite.SQLiteDataSource();
        ds.setUrl(jdbcUrl);

        DataSource dataSource = ds;

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS customer_service_chat_history ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT NOT NULL, "
                + "session_id TEXT, "
                + "complaint_time DATETIME NOT NULL DEFAULT (datetime('now', 'localtime')), "
                + "complaint_status TEXT NOT NULL DEFAULT '未闭环' CHECK (complaint_status IN ('未闭环', '已闭环')), "
                + "transferred_to_human INTEGER NOT NULL DEFAULT 0 CHECK (transferred_to_human IN (0, 1))"
                + ")");

        jdbc.execute("CREATE TABLE IF NOT EXISTS chat_message ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "session_id TEXT NOT NULL, "
                + "role TEXT NOT NULL, "
                + "content TEXT NOT NULL, "
                + "created_at DATETIME NOT NULL DEFAULT (datetime('now', 'localtime'))"
                + ")");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_chat_message_session_id ON chat_message(session_id)");

        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
