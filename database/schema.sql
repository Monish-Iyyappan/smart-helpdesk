-- =============================================================================
-- Smart Helpdesk - Database Schema
-- Character Set: utf8mb4 | Collation: utf8mb4_unicode_ci | Engine: InnoDB
-- =============================================================================
CREATE DATABASE IF NOT EXISTS smart_helpdesk
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE smart_helpdesk;
-- -----------------------------------------------------------------------------
-- 1. Table: users
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'AGENT', 'ADMIN') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- -----------------------------------------------------------------------------
-- 2. Table: categories
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- -----------------------------------------------------------------------------
-- 3. Table: incidents
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS incidents (
    incident_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(100) NOT NULL,
    status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- -----------------------------------------------------------------------------
-- 4. Table: tickets
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tickets (
    ticket_id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(20) NOT NULL UNIQUE,
    created_by INT NOT NULL,
    assigned_to INT NULL,
    category_id INT NOT NULL,
    incident_id INT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') NOT NULL,
    status ENUM('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tickets_created_by FOREIGN KEY (created_by)
        REFERENCES users (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tickets_assigned_to FOREIGN KEY (assigned_to)
        REFERENCES users (user_id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_tickets_category FOREIGN KEY (category_id)
        REFERENCES categories (category_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_tickets_incident FOREIGN KEY (incident_id)
        REFERENCES incidents (incident_id) ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_tickets_status (status),
    INDEX idx_tickets_assigned_to (assigned_to),
    INDEX idx_tickets_created_by (created_by),
    INDEX idx_tickets_category_id (category_id),
    INDEX idx_tickets_incident_id (incident_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- -----------------------------------------------------------------------------
-- 5. Table: ticket_participants
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ticket_participants (
    participant_id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id INT NOT NULL,
    user_id INT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_ticket_participant UNIQUE (ticket_id, user_id),
    CONSTRAINT fk_participants_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets (ticket_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_participants_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_participants_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- -----------------------------------------------------------------------------
-- 6. Table: comments
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comments (
    comment_id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id INT NOT NULL,
    user_id INT NOT NULL,
    comment_text TEXT NOT NULL,
    comment_type ENUM('GENERAL', 'ISSUE', 'REASSIGNMENT', 'CLOSURE', 'REOPEN') NOT NULL DEFAULT 'GENERAL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_ticket FOREIGN KEY (ticket_id)
        REFERENCES tickets (ticket_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_comments_ticket_id (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- -----------------------------------------------------------------------------
-- 7. Table: ticket_assignment_history
-- -----------------------------------------------------------------------------
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
-- -----------------------------------------------------------------------------
-- Seed Data: Categories
-- -----------------------------------------------------------------------------
INSERT IGNORE INTO categories (category_name) VALUES
    ('IT Support'),
    ('Examination'),
    ('Student Administration'),
    ('Fees and Payments'),
    ('Transport'),
    ('Timetable'),
    ('Campus Facilities'),
    ('Other');