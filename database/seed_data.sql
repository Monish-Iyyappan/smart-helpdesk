-- =============================================================================
-- Smart Helpdesk - Seed Data
-- Test data only, never run on production.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Users (1 ADMIN, 2 AGENT, 4 STUDENT)
-- Password for all users: Test@1234
-- Verified BCrypt ($2a$, cost 10) hash: $2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6
-- -----------------------------------------------------------------------------

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Admin User', 'admin@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'ADMIN', '2026-10-01 08:00:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@college.example'
);

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Alex Rivera', 'agent1@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'AGENT', '2026-10-01 08:05:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'agent1@college.example'
);

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Jordan Lee', 'agent2@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'AGENT', '2026-10-01 08:10:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'agent2@college.example'
);

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Sam Taylor', 'student1@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'STUDENT', '2026-10-01 09:00:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'student1@college.example'
);

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Morgan Chen', 'student2@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'STUDENT', '2026-10-01 09:05:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'student2@college.example'
);

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Casey Patel', 'student3@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'STUDENT', '2026-10-01 09:10:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'student3@college.example'
);

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Riley Brooks', 'student4@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'STUDENT', '2026-10-01 09:15:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'student4@college.example'
);

-- -----------------------------------------------------------------------------
-- 2. Incidents (2 campus incidents)
-- -----------------------------------------------------------------------------

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 
    'Campus Wi-Fi Outage',
    'Campus-wide intermittent Wi-Fi connectivity degradation affecting academic buildings.',
    'Campus-Wide',
    'IN_PROGRESS',
    'HIGH',
    '2026-10-02 09:00:00',
    '2026-10-02 09:30:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Campus Wi-Fi Outage'
);

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 
    'Classroom Projector Failure',
    'Main digital projector in lecture hall B102 is unresponsive.',
    'Lecture Hall B102',
    'OPEN',
    'MEDIUM',
    '2026-10-02 10:00:00',
    '2026-10-02 10:00:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Classroom Projector Failure'
);

-- -----------------------------------------------------------------------------
-- 3. Tickets (HD-1001 to HD-1007)
-- Note: ticket_number equals 1000 + ticket_id only valid on an empty tickets table.
-- Covers categories: IT Support, Examination, Fees and Payments, Campus Facilities, Transport
-- Covers statuses: OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED
-- -----------------------------------------------------------------------------

-- Ticket 1: HD-1001 (OPEN, assigned_to NULL, linked to Wi-Fi outage)
INSERT INTO tickets (ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at)
SELECT
    'HD-1001',
    (SELECT user_id FROM users WHERE email = 'student1@college.example'),
    NULL,
    (SELECT category_id FROM categories WHERE category_name = 'IT Support'),
    (SELECT incident_id FROM incidents WHERE title = 'Campus Wi-Fi Outage'),
    'Library 2nd Floor Wi-Fi Disconnected',
    'Cannot connect to the university student Wi-Fi network from the second floor quiet study area.',
    'HIGH',
    'OPEN',
    '2026-10-02 09:15:00',
    '2026-10-02 09:15:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM tickets WHERE ticket_number = 'HD-1001'
);

-- Ticket 2: HD-1002 (ASSIGNED, assigned to agent1, linked to Wi-Fi outage)
INSERT INTO tickets (ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at)
SELECT
    'HD-1002',
    (SELECT user_id FROM users WHERE email = 'student2@college.example'),
    (SELECT user_id FROM users WHERE email = 'agent1@college.example'),
    (SELECT category_id FROM categories WHERE category_name = 'IT Support'),
    (SELECT incident_id FROM incidents WHERE title = 'Campus Wi-Fi Outage'),
    'Science Building Dorm Wi-Fi Dropping',
    'Wi-Fi signal keeps dropping out in Dorm Block C every few minutes.',
    'HIGH',
    'ASSIGNED',
    '2026-10-02 09:20:00',
    '2026-10-02 09:30:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM tickets WHERE ticket_number = 'HD-1002'
);

-- Ticket 3: HD-1003 (IN_PROGRESS, assigned to agent1, linked to Wi-Fi outage)
INSERT INTO tickets (ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at)
SELECT
    'HD-1003',
    (SELECT user_id FROM users WHERE email = 'student3@college.example'),
    (SELECT user_id FROM users WHERE email = 'agent1@college.example'),
    (SELECT category_id FROM categories WHERE category_name = 'IT Support'),
    (SELECT incident_id FROM incidents WHERE title = 'Campus Wi-Fi Outage'),
    'Student Center Wi-Fi Authentication Failure',
    'Receiving an authentication error when entering university credentials at the student center.',
    'URGENT',
    'IN_PROGRESS',
    '2026-10-02 09:25:00',
    '2026-10-02 10:00:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM tickets WHERE ticket_number = 'HD-1003'
);

-- Ticket 4: HD-1004 (RESOLVED, assigned to agent2, no incident)
INSERT INTO tickets (ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at)
SELECT
    'HD-1004',
    (SELECT user_id FROM users WHERE email = 'student1@college.example'),
    (SELECT user_id FROM users WHERE email = 'agent2@college.example'),
    (SELECT category_id FROM categories WHERE category_name = 'Examination'),
    NULL,
    'Missing Midterm Exam Hall Ticket',
    'The student portal does not show the hall ticket link for upcoming biology midterm examinations.',
    'URGENT',
    'RESOLVED',
    '2026-10-03 08:30:00',
    '2026-10-03 11:45:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM tickets WHERE ticket_number = 'HD-1004'
);

-- Ticket 5: HD-1005 (CLOSED, assigned to agent1, no incident)
INSERT INTO tickets (ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at)
SELECT
    'HD-1005',
    (SELECT user_id FROM users WHERE email = 'student4@college.example'),
    (SELECT user_id FROM users WHERE email = 'agent1@college.example'),
    (SELECT category_id FROM categories WHERE category_name = 'Fees and Payments'),
    NULL,
    'Tuition Fee Receipt Not Generated',
    'Semester 1 tuition fee payment completed through bank gateway but receipt not listed under student account.',
    'MEDIUM',
    'CLOSED',
    '2026-10-01 10:00:00',
    '2026-10-01 16:30:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM tickets WHERE ticket_number = 'HD-1005'
);

-- Ticket 6: HD-1006 (IN_PROGRESS, assigned to agent2, linked to Projector failure)
INSERT INTO tickets (ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at)
SELECT
    'HD-1006',
    (SELECT user_id FROM users WHERE email = 'student2@college.example'),
    (SELECT user_id FROM users WHERE email = 'agent2@college.example'),
    (SELECT category_id FROM categories WHERE category_name = 'Campus Facilities'),
    (SELECT incident_id FROM incidents WHERE title = 'Classroom Projector Failure'),
    'Hall B102 Projector Display Fault',
    'Projector lamp blinks orange and screen is blank during scheduled morning seminar.',
    'MEDIUM',
    'IN_PROGRESS',
    '2026-10-02 10:05:00',
    '2026-10-02 10:30:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM tickets WHERE ticket_number = 'HD-1006'
);

-- Ticket 7: HD-1007 (RESOLVED, assigned to agent2, no incident)
INSERT INTO tickets (ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at)
SELECT
    'HD-1007',
    (SELECT user_id FROM users WHERE email = 'student3@college.example'),
    (SELECT user_id FROM users WHERE email = 'agent2@college.example'),
    (SELECT category_id FROM categories WHERE category_name = 'Transport'),
    NULL,
    'Route 4 Campus Shuttle Delayed',
    'Morning shuttle bus for Route 4 was 35 minutes late without prior notification.',
    'LOW',
    'RESOLVED',
    '2026-10-03 07:45:00',
    '2026-10-03 09:15:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM tickets WHERE ticket_number = 'HD-1007'
);

-- -----------------------------------------------------------------------------
-- 4. Ticket Participants
-- Creators automatically included + extra participants on incident tickets
-- (Wi-Fi incident tickets have at least 2 distinct students participating)
-- -----------------------------------------------------------------------------

-- HD-1001 creator (student1)
INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001'),
    (SELECT user_id FROM users WHERE email = 'student1@college.example'),
    '2026-10-02 09:15:00';

-- HD-1001 co-participant (student4 joined Wi-Fi ticket)
INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001'),
    (SELECT user_id FROM users WHERE email = 'student4@college.example'),
    '2026-10-02 09:20:00';

-- HD-1002 creator (student2)
INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1002'),
    (SELECT user_id FROM users WHERE email = 'student2@college.example'),
    '2026-10-02 09:20:00';

-- HD-1003 creator (student3)
INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1003'),
    (SELECT user_id FROM users WHERE email = 'student3@college.example'),
    '2026-10-02 09:25:00';

-- HD-1004 creator (student1)
INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1004'),
    (SELECT user_id FROM users WHERE email = 'student1@college.example'),
    '2026-10-03 08:30:00';

-- HD-1005 creator (student4)
INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1005'),
    (SELECT user_id FROM users WHERE email = 'student4@college.example'),
    '2026-10-01 10:00:00';

-- HD-1006 creator (student2)
INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1006'),
    (SELECT user_id FROM users WHERE email = 'student2@college.example'),
    '2026-10-02 10:05:00';

-- HD-1006 co-participant (student1 joined projector ticket)
INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1006'),
    (SELECT user_id FROM users WHERE email = 'student1@college.example'),
    '2026-10-02 10:10:00';

-- HD-1007 creator (student3)
INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1007'),
    (SELECT user_id FROM users WHERE email = 'student3@college.example'),
    '2026-10-03 07:45:00';

-- -----------------------------------------------------------------------------
-- 5. Comments (10 comments, written by authorized parties, after ticket created_at)
-- -----------------------------------------------------------------------------

-- Comment 1 on HD-1001 by participant student4
INSERT INTO comments (ticket_id, user_id, comment_text, created_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001'),
    (SELECT user_id FROM users WHERE email = 'student4@college.example'),
    'I am also experiencing this on the second floor near the chemistry stacks.',
    '2026-10-02 09:20:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001')
      AND c.comment_text = 'I am also experiencing this on the second floor near the chemistry stacks.'
);

-- Comment 2 on HD-1001 by admin
INSERT INTO comments (ticket_id, user_id, comment_text, created_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001'),
    (SELECT user_id FROM users WHERE email = 'admin@college.example'),
    'We have linked this ticket under the ongoing Campus Wi-Fi Outage incident.',
    '2026-10-02 09:25:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001')
      AND c.comment_text = 'We have linked this ticket under the ongoing Campus Wi-Fi Outage incident.'
);

-- Comment 3 on HD-1002 by assigned agent1
INSERT INTO comments (ticket_id, user_id, comment_text, created_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1002'),
    (SELECT user_id FROM users WHERE email = 'agent1@college.example'),
    'Assigned to network engineering team. Checking the dorm access points now.',
    '2026-10-02 09:35:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1002')
      AND c.comment_text = 'Assigned to network engineering team. Checking the dorm access points now.'
);

-- Comment 4 on HD-1002 by creator student2
INSERT INTO comments (ticket_id, user_id, comment_text, created_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1002'),
    (SELECT user_id FROM users WHERE email = 'student2@college.example'),
    'Thank you, signal keeps disconnecting every few minutes.',
    '2026-10-02 09:40:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1002')
      AND c.comment_text = 'Thank you, signal keeps disconnecting every few minutes.'
);

-- Comment 5 on HD-1003 by assigned agent1
INSERT INTO comments (ticket_id, user_id, comment_text, created_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1003'),
    (SELECT user_id FROM users WHERE email = 'agent1@college.example'),
    'Radius authentication service reboot is in progress to restore logins.',
    '2026-10-02 10:05:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1003')
      AND c.comment_text = 'Radius authentication service reboot is in progress to restore logins.'
);

-- Comment 6 on HD-1004 by assigned agent2
INSERT INTO comments (ticket_id, user_id, comment_text, created_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1004'),
    (SELECT user_id FROM users WHERE email = 'agent2@college.example'),
    'Duplicate hall ticket generated and sent to your registered email address.',
    '2026-10-03 11:30:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1004')
      AND c.comment_text = 'Duplicate hall ticket generated and sent to your registered email address.'
);

-- Comment 7 on HD-1004 by creator student1
INSERT INTO comments (ticket_id, user_id, comment_text, created_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1004'),
    (SELECT user_id FROM users WHERE email = 'student1@college.example'),
    'Received and printed. Thank you for the quick resolution!',
    '2026-10-03 11:45:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1004')
      AND c.comment_text = 'Received and printed. Thank you for the quick resolution!'
);

-- Comment 8 on HD-1005 by assigned agent1
INSERT INTO comments (ticket_id, user_id, comment_text, created_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1005'),
    (SELECT user_id FROM users WHERE email = 'agent1@college.example'),
    'Payment reconciliation verified. The official receipt is now available in your portal.',
    '2026-10-01 14:00:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1005')
      AND c.comment_text = 'Payment reconciliation verified. The official receipt is now available in your portal.'
);

-- Comment 9 on HD-1005 by admin
INSERT INTO comments (ticket_id, user_id, comment_text, created_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1005'),
    (SELECT user_id FROM users WHERE email = 'admin@college.example'),
    'Ticket closed following successful student confirmation.',
    '2026-10-01 16:30:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1005')
      AND c.comment_text = 'Ticket closed following successful student confirmation.'
);

-- Comment 10 on HD-1006 by participant student1
INSERT INTO comments (ticket_id, user_id, comment_text, created_at)
SELECT
    (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1006'),
    (SELECT user_id FROM users WHERE email = 'student1@college.example'),
    'Lecturer is waiting for AV support in room B102.',
    '2026-10-02 10:15:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments c
    WHERE c.ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1006')
      AND c.comment_text = 'Lecturer is waiting for AV support in room B102.'
);

-- -----------------------------------------------------------------------------
-- 6. Verification Queries (Commented Out)
-- -----------------------------------------------------------------------------

-- A. Row counts per table
-- SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
-- UNION ALL
-- SELECT 'categories', COUNT(*) FROM categories
-- UNION ALL
-- SELECT 'incidents', COUNT(*) FROM incidents
-- UNION ALL
-- SELECT 'tickets', COUNT(*) FROM tickets
-- UNION ALL
-- SELECT 'ticket_participants', COUNT(*) FROM ticket_participants
-- UNION ALL
-- SELECT 'comments', COUNT(*) FROM comments;

-- B. Tickets joined with creator and assigned agent names
-- SELECT 
--     t.ticket_number,
--     t.title,
--     t.status,
--     t.priority,
--     c.category_name,
--     u_creator.name AS creator_name,
--     u_agent.name AS assigned_agent_name,
--     i.title AS incident_title,
--     t.created_at,
--     t.updated_at
-- FROM tickets t
-- JOIN categories c ON t.category_id = c.category_id
-- JOIN users u_creator ON t.created_by = u_creator.user_id
-- LEFT JOIN users u_agent ON t.assigned_to = u_agent.user_id
-- LEFT JOIN incidents i ON t.incident_id = i.incident_id
-- ORDER BY t.ticket_number;

-- C. Affected students per incident using COUNT(DISTINCT user_id)
-- SELECT 
--     i.incident_id,
--     i.title AS incident_title,
--     i.status AS incident_status,
--     COUNT(DISTINCT affected.user_id) AS affected_students_count
-- FROM incidents i
-- LEFT JOIN (
--     SELECT incident_id, created_by AS user_id FROM tickets WHERE incident_id IS NOT NULL
--     UNION
--     SELECT t.incident_id, tp.user_id
--     FROM ticket_participants tp
--     JOIN tickets t ON tp.ticket_id = t.ticket_id
--     WHERE t.incident_id IS NOT NULL
-- ) affected ON i.incident_id = affected.incident_id
-- GROUP BY i.incident_id, i.title, i.status;
