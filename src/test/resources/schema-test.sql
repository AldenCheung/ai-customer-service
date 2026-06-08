CREATE TABLE IF NOT EXISTS customer_service_chat_history (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    username TEXT NOT NULL,
    session_id TEXT,
    complaint_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    complaint_status TEXT NOT NULL DEFAULT '未闭环',
    transferred_to_human INTEGER NOT NULL DEFAULT 0
);
