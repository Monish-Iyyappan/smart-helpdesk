-- =============================================================================
-- Smart Helpdesk - Unified Seed Data (seed_schema.sql)
-- Combines users, all campus incidents, tickets, participants, and comments.
-- Safe and idempotent: can be executed on a fresh or existing database.
-- Never run in production.
-- =============================================================================

USE smart_helpdesk;

-- -----------------------------------------------------------------------------
-- 1. Users (1 ADMIN, 2 AGENTS, 4 STUDENTS)
-- Default Password for all seed users: Test@1234
-- BCrypt ($2a$, cost 10) Hash: $2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6
-- -----------------------------------------------------------------------------

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Admin User', 'admin@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'ADMIN', '2026-10-01 08:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@college.example');

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Alex Rivera', 'agent1@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'AGENT', '2026-10-01 08:05:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'agent1@college.example');

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Jordan Lee', 'agent2@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'AGENT', '2026-10-01 08:10:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'agent2@college.example');

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Sam Taylor', 'student1@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'STUDENT', '2026-10-01 09:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'student1@college.example');

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Morgan Chen', 'student2@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'STUDENT', '2026-10-01 09:05:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'student2@college.example');

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Casey Patel', 'student3@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'STUDENT', '2026-10-01 09:10:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'student3@college.example');

INSERT INTO users (name, email, password_hash, role, created_at)
SELECT 'Riley Brooks', 'student4@college.example', '$2a$10$p3sxqrfrmATxOsVlnHRckuf9OFZ1d5uvF/xxSZPLjuj5oqHr./.E6', 'STUDENT', '2026-10-01 09:15:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'student4@college.example');


-- -----------------------------------------------------------------------------
-- 2. Campus Incidents (12 Total: 2 linked to starter tickets + 10 realistic campus events)
-- -----------------------------------------------------------------------------

-- Core Incidents for Tickets
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Campus Wi-Fi Outage', 'Campus-wide intermittent Wi-Fi connectivity degradation affecting academic buildings.', 'Campus-Wide', 'IN_PROGRESS', 'HIGH', '2026-10-02 09:00:00', '2026-10-02 09:30:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Campus Wi-Fi Outage');

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Classroom Projector Failure', 'Main digital projector in lecture hall B102 is unresponsive.', 'Lecture Hall B102', 'OPEN', 'MEDIUM', '2026-10-02 10:00:00', '2026-10-02 10:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Classroom Projector Failure');

-- Category-based Campus Incidents
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Student Portal Authentication Gateway Outage', 'Single Sign-On (SSO) LDAP service is rejecting valid credentials campus-wide, preventing student course portal access.', 'Data Center / Online', 'IN_PROGRESS', 'CRITICAL', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Student Portal Authentication Gateway Outage');

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Online Examination Portal System Timeout', 'Midterm computer-based testing portal experienced database connection pool exhaustion during scheduled exam slots.', 'Computer Lab Complex C', 'OPEN', 'HIGH', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Online Examination Portal System Timeout');

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Student ID Card RFID Reader Malfunction', 'Automated turnstile RFID scanners at the main campus entrance gate are failing to validate active student smart cards.', 'Main Campus Entrance Gate', 'OPEN', 'MEDIUM', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Student ID Card RFID Reader Malfunction');

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Online Fee Payment Gateway Transaction Error', 'Third-party banking payment gateway timing out during tuition fee settlement, deducting student accounts without issuing payment confirmation.', 'Finance Office / Online', 'IN_PROGRESS', 'CRITICAL', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Online Fee Payment Gateway Transaction Error');

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Campus Shuttle GPS Tracking System Failure', 'Real-time transit API telemetry offline for Route 1 through 4 student shuttles, resulting in unannounced bus scheduling gaps.', 'Campus Transit Hub', 'RESOLVED', 'LOW', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Campus Shuttle GPS Tracking System Failure');

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Central Timetable Scheduling System Sync Failure', 'Automated course registration batch update failed overnight, creating overlapping classroom allocations across department rosters.', 'Registrar Office / Online', 'OPEN', 'HIGH', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Central Timetable Scheduling System Sync Failure');

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Central Library HVAC System Failure', 'Chilled water pipe rupture in central air handling unit has disabled climate control across third-floor graduate study floors.', 'Central Library Floor 3', 'IN_PROGRESS', 'MEDIUM', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Central Library HVAC System Failure');

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Main Cafeteria Point-of-Sale (POS) System Offline', 'Campus meal plan debit terminal network unreachable across south campus dining hall counters during peak lunch period.', 'Student Center Dining Hall', 'RESOLVED', 'MEDIUM', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Main Cafeteria Point-of-Sale (POS) System Offline');

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Campus Emergency Alert Notification Gateway Offline', 'SMS broadcast dispatch microservice timed out during scheduled monthly siren safety connectivity drill.', 'Campus Safety Operations', 'CLOSED', 'HIGH', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Campus Emergency Alert Notification Gateway Offline');

INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT 'Hostel Block D Water Supply Disruption', 'Booster pump pressure sensor malfunction in auxiliary tank has interrupted fresh water supply to floors 2 and 3.', 'Hostel Block D', 'IN_PROGRESS', 'CRITICAL', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM incidents WHERE title = 'Hostel Block D Water Supply Disruption');


-- -----------------------------------------------------------------------------
-- 3. Tickets (HD-1001 through HD-1007)
-- Covers all statuses: OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED
-- Covers categories: IT Support, Examination, Fees and Payments, Campus Facilities, Transport
-- -----------------------------------------------------------------------------

-- HD-1001 (OPEN, unassigned, linked to Wi-Fi Outage)
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
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE ticket_number = 'HD-1001');

-- HD-1002 (ASSIGNED, agent1, linked to Wi-Fi Outage)
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
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE ticket_number = 'HD-1002');

-- HD-1003 (IN_PROGRESS, agent1, linked to Wi-Fi Outage)
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
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE ticket_number = 'HD-1003');

-- HD-1004 (RESOLVED, agent2, no incident)
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
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE ticket_number = 'HD-1004');

-- HD-1005 (CLOSED, agent1, no incident)
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
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE ticket_number = 'HD-1005');

-- HD-1006 (IN_PROGRESS, agent2, linked to Projector Failure)
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
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE ticket_number = 'HD-1006');

-- HD-1007 (RESOLVED, agent2, no incident)
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
WHERE NOT EXISTS (SELECT 1 FROM tickets WHERE ticket_number = 'HD-1007');


-- -----------------------------------------------------------------------------
-- 4. Ticket Participants (Student Creators & Co-Participants)
-- -----------------------------------------------------------------------------

INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001'), (SELECT user_id FROM users WHERE email = 'student1@college.example'), '2026-10-02 09:15:00';

INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001'), (SELECT user_id FROM users WHERE email = 'student4@college.example'), '2026-10-02 09:20:00';

INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1002'), (SELECT user_id FROM users WHERE email = 'student2@college.example'), '2026-10-02 09:20:00';

INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1003'), (SELECT user_id FROM users WHERE email = 'student3@college.example'), '2026-10-02 09:25:00';

INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1004'), (SELECT user_id FROM users WHERE email = 'student1@college.example'), '2026-10-03 08:30:00';

INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1005'), (SELECT user_id FROM users WHERE email = 'student4@college.example'), '2026-10-01 10:00:00';

INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1006'), (SELECT user_id FROM users WHERE email = 'student2@college.example'), '2026-10-02 10:05:00';

INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1006'), (SELECT user_id FROM users WHERE email = 'student1@college.example'), '2026-10-02 10:10:00';

INSERT IGNORE INTO ticket_participants (ticket_id, user_id, joined_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1007'), (SELECT user_id FROM users WHERE email = 'student3@college.example'), '2026-10-03 07:45:00';


-- -----------------------------------------------------------------------------
-- 5. Comments (Timeline messages between students, agents, and admin)
-- -----------------------------------------------------------------------------

INSERT INTO comments (ticket_id, user_id, comment_text, comment_type, created_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001'), (SELECT user_id FROM users WHERE email = 'student4@college.example'), 'I am also experiencing this on the second floor near the chemistry stacks.', 'ISSUE', '2026-10-02 09:20:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments WHERE ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001') AND comment_text LIKE '%chemistry stacks%'
);

INSERT INTO comments (ticket_id, user_id, comment_text, comment_type, created_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001'), (SELECT user_id FROM users WHERE email = 'admin@college.example'), 'We have linked this ticket under the ongoing Campus Wi-Fi Outage incident.', 'GENERAL', '2026-10-02 09:25:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments WHERE ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1001') AND comment_text LIKE '%Wi-Fi Outage incident%'
);

INSERT INTO comments (ticket_id, user_id, comment_text, comment_type, created_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1002'), (SELECT user_id FROM users WHERE email = 'agent1@college.example'), 'Assigned to network engineering team. Checking the dorm access points now.', 'GENERAL', '2026-10-02 09:35:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments WHERE ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1002') AND comment_text LIKE '%network engineering%'
);

INSERT INTO comments (ticket_id, user_id, comment_text, comment_type, created_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1002'), (SELECT user_id FROM users WHERE email = 'student2@college.example'), 'Thank you, signal keeps disconnecting every few minutes.', 'GENERAL', '2026-10-02 09:40:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments WHERE ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1002') AND comment_text LIKE '%signal keeps disconnecting%'
);

INSERT INTO comments (ticket_id, user_id, comment_text, comment_type, created_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1003'), (SELECT user_id FROM users WHERE email = 'agent1@college.example'), 'Radius authentication service reboot is in progress to restore logins.', 'GENERAL', '2026-10-02 10:05:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments WHERE ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1003') AND comment_text LIKE '%Radius authentication%'
);

INSERT INTO comments (ticket_id, user_id, comment_text, comment_type, created_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1004'), (SELECT user_id FROM users WHERE email = 'agent2@college.example'), 'Duplicate hall ticket generated and sent to your registered email address.', 'GENERAL', '2026-10-03 11:30:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments WHERE ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1004') AND comment_text LIKE '%Duplicate hall ticket%'
);

INSERT INTO comments (ticket_id, user_id, comment_text, comment_type, created_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1004'), (SELECT user_id FROM users WHERE email = 'student1@college.example'), 'Received and printed. Thank you for the quick resolution!', 'GENERAL', '2026-10-03 11:45:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments WHERE ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1004') AND comment_text LIKE '%quick resolution%'
);

INSERT INTO comments (ticket_id, user_id, comment_text, comment_type, created_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1005'), (SELECT user_id FROM users WHERE email = 'agent1@college.example'), 'Payment reconciliation verified. The official receipt is now available in your portal.', 'GENERAL', '2026-10-01 14:00:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments WHERE ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1005') AND comment_text LIKE '%reconciliation verified%'
);

INSERT INTO comments (ticket_id, user_id, comment_text, comment_type, created_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1005'), (SELECT user_id FROM users WHERE email = 'admin@college.example'), 'Ticket closed following successful student confirmation.', 'CLOSURE', '2026-10-01 16:30:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments WHERE ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1005') AND comment_text LIKE '%Ticket closed following%'
);

INSERT INTO comments (ticket_id, user_id, comment_text, comment_type, created_at)
SELECT (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1006'), (SELECT user_id FROM users WHERE email = 'student1@college.example'), 'Lecturer is waiting for AV support in room B102.', 'GENERAL', '2026-10-02 10:15:00'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM comments WHERE ticket_id = (SELECT ticket_id FROM tickets WHERE ticket_number = 'HD-1006') AND comment_text LIKE '%AV support%'
);


-- -----------------------------------------------------------------------------
-- 6. Ticket Assignment History (Audit Log)
-- -----------------------------------------------------------------------------

INSERT INTO ticket_assignment_history (ticket_id, from_agent, to_agent, changed_by, reason, changed_at)
SELECT
    t.ticket_id,
    NULL,
    t.assigned_to,
    (SELECT MIN(user_id) FROM users WHERE role = 'ADMIN'),
    'Initial assignment to support staff',
    t.created_at
FROM tickets t
WHERE t.assigned_to IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM ticket_assignment_history h WHERE h.ticket_id = t.ticket_id
  );
