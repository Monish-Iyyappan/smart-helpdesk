-- =============================================================================
-- Smart Helpdesk - Migration: Ticket Assignment History and Comment Type
-- For hosted/managed MySQL database
-- Contains only ALTER and CREATE statements
-- =============================================================================

ALTER TABLE comments
    ADD COLUMN comment_type ENUM('GENERAL', 'ISSUE', 'REASSIGNMENT', 'CLOSURE') NOT NULL DEFAULT 'GENERAL'
    AFTER comment_text;

CREATE TABLE IF NOT EXISTS ticket_assignment_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id INT NOT NULL,
    from_agent INT NULL,
    to_agent INT NOT NULL,
    changed_by INT NOT NULL,
    reason TEXT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tah_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets (ticket_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tah_from_agent FOREIGN KEY (from_agent)
        REFERENCES users (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tah_to_agent FOREIGN KEY (to_agent)
        REFERENCES users (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tah_changed_by FOREIGN KEY (changed_by)
        REFERENCES users (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_assignment_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
