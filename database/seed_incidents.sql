-- =============================================================================
-- Smart Helpdesk - Seed Incidents
-- Adds 10 unique, realistic campus incidents to the local database.
-- Re-runnable: Skips any incident if an incident with the same title exists.
-- =============================================================================

-- 1. Category: IT Support
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT
    'Student Portal Authentication Gateway Outage',
    'Single Sign-On (SSO) LDAP service is rejecting valid credentials campus-wide, preventing student course portal access.',
    'Data Center / Online',
    'IN_PROGRESS',
    'CRITICAL',
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Student Portal Authentication Gateway Outage'
);

-- 2. Category: Examination
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT
    'Online Examination Portal System Timeout',
    'Midterm computer-based testing portal experienced database connection pool exhaustion during scheduled exam slots.',
    'Computer Lab Complex C',
    'OPEN',
    'HIGH',
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Online Examination Portal System Timeout'
);

-- 3. Category: Student Administration
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT
    'Student ID Card RFID Reader Malfunction',
    'Automated turnstile RFID scanners at the main campus entrance gate are failing to validate active student smart cards.',
    'Main Campus Entrance Gate',
    'OPEN',
    'MEDIUM',
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Student ID Card RFID Reader Malfunction'
);

-- 4. Category: Fees and Payments
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT
    'Online Fee Payment Gateway Transaction Error',
    'Third-party banking payment gateway timing out during tuition fee settlement, deducting student accounts without issuing payment confirmation.',
    'Finance Office / Online',
    'IN_PROGRESS',
    'CRITICAL',
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Online Fee Payment Gateway Transaction Error'
);

-- 5. Category: Transport
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT
    'Campus Shuttle GPS Tracking System Failure',
    'Real-time telematics and live bus location feed for all North Campus shuttle routes are offline on the mobile transit tracker.',
    'Transit Depot & North Route',
    'OPEN',
    'LOW',
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Campus Shuttle GPS Tracking System Failure'
);

-- 6. Category: Timetable
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT
    'Academic Timetable Room Conflict Anomaly',
    'Scheduling software synchronization error caused overlapping classroom bookings for Chemistry and Physics lectures in Science Hall 204.',
    'Science Block Hall 204',
    'OPEN',
    'MEDIUM',
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Academic Timetable Room Conflict Anomaly'
);

-- 7. Category: Campus Facilities
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT
    'Central Library HVAC Cooling System Breakdown',
    'Chiller unit compressor failure on the third-floor study lounge resulting in elevated ambient temperatures and noise disruption.',
    'Central Library Level 3',
    'OPEN',
    'MEDIUM',
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Central Library HVAC Cooling System Breakdown'
);

-- 8. Category: IT Support
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT
    'Engineering Lab Computer Network Switch Loop',
    'Core edge switch in Electrical Engineering Lab experienced a broadcast storm due to an unmanaged loop, cutting wired LAN connectivity.',
    'Engineering Building Lab 3',
    'IN_PROGRESS',
    'HIGH',
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Engineering Lab Computer Network Switch Loop'
);

-- 9. Category: Campus Facilities
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT
    'Dormitory Water Pressure Booster Pump Tripped',
    'Main circuit breaker for the domestic water booster pump tripped, interrupting water supply to upper floors of Student Residence Hall 2.',
    'Residence Hall Block 2',
    'RESOLVED',
    'HIGH',
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Dormitory Water Pressure Booster Pump Tripped'
);

-- 10. Category: Other
INSERT INTO incidents (title, description, location, status, severity, created_at, updated_at)
SELECT
    'Campus Health Center Prescription Dispenser Offline',
    'Automated medical locker system at the student health clinic lost network connectivity, delaying automated after-hours medicine pickups.',
    'Student Wellness Center',
    'CLOSED',
    'LOW',
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM incidents WHERE title = 'Campus Health Center Prescription Dispenser Offline'
);
