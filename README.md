# Smart Helpdesk

Accountable support ticketing for campuses. Every ticket has an owner, a reason and an ending.

**Live demo:** https://smarthelpdesk-jet.vercel.app
(The backend runs on a free tier, so the first load can take about 30 seconds.)

<img width="1535" height="775" alt="image" src="https://github.com/user-attachments/assets/2b398f80-447f-4614-a099-a3c23df7cae0" />


## About
Complaints on a campus arrive through scattered channels, get reported again and again, and nobody knows who owns them. Smart Helpdesk fixes this: students raise tickets, an admin assigns one owner agent, the agent resolves it, and the admin closes it with a written note.

Built solo by **Monish Iyyappan**.

## Features
- **Role-based access:** Student, Agent and Admin, with roles checked on the server
- **Single owner:** one agent per ticket
- **Reasoned handover:** every reassignment needs a written reason (10-500 characters)
- **Closure notes and reopen:** closing needs a note, and an admin can reopen a resolved ticket with a reason
- **Live urgency:** LOW, MEDIUM, HIGH or URGENT, calculated on every read and never typed by the user
- **Overdue alerts:** tickets past their SLA deadline turn red and escalate a level
- **Shared incidents:** many tickets link to one incident, with ticket counts per incident
- **Join a ticket:** students join an existing ticket instead of filing a duplicate
- **Audit trail:** assignment history, typed comments and a closed-ticket log

## Ticket lifecycle
`OPEN` → `ASSIGNED` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`
(an admin can reopen a `RESOLVED` ticket back to `IN_PROGRESS`)

## Screenshots
| Student dashboard | Agent dashboard |
|---|---|
| <img width="1516" height="773" alt="image" src="https://github.com/user-attachments/assets/2ebb9c78-e56e-4ff4-8032-3124c22f7dc0" /> | <img width="1517" height="767" alt="image" src="https://github.com/user-attachments/assets/786ae1db-8283-4d21-9afc-bdcd1aaedd35" /> |


| Admin dashboard | Incidents |
|---|---|
| <img width="1517" height="770" alt="image" src="https://github.com/user-attachments/assets/2263f666-ec99-456b-8de7-16ee007cfd77" /> | ![Uploading image.png…]() |

| Tickets Details | Ticket History |
|---|---|
| <img width="1518" height="776" alt="Screenshot 2026-10-28 130709" src="https://github.com/user-attachments/assets/63c4cf29-ff1f-4a58-946b-8a0a71b4182f" /> | <img width="1520" height="762" alt="image" src="https://github.com/user-attachments/assets/9af3e0e0-d95f-48fb-b3de-ab8f14f937bd" /> |

## Tech stack
| Layer | Technology | Hosting |
|---|---|---|
| Frontend | React, Vite, TypeScript, Tailwind | Vercel |
| Backend | Java 17, Spring Boot 3.4, Spring JDBC | Render (Docker) |
| Database | MySQL 8 (7 tables) | Aiven |
| Security | HTTP Basic login, BCrypt password hashing | |
| Tests | JUnit 5 (108 backend tests) | |

## Architecture
```text
React app  →  REST API (controller → service → dao)  →  MySQL
```
Backend packages: `controller`, `service`, `dao`, `model`, `assignment`, `priority`, `config`, `exception`.

## Database
```mermaid
erDiagram
    USERS ||--o{ TICKETS : creates
    USERS |o--o{ TICKETS : "is assigned"
    CATEGORIES ||--o{ TICKETS : classifies
    INCIDENTS |o--o{ TICKETS : groups
    TICKETS ||--o{ COMMENTS : has
    USERS ||--o{ COMMENTS : writes
    TICKETS ||--o{ TICKET_PARTICIPANTS : has
    USERS ||--o{ TICKET_PARTICIPANTS : joins
    TICKETS ||--o{ TICKET_ASSIGNMENT_HISTORY : logs
    USERS ||--o{ TICKET_ASSIGNMENT_HISTORY : "changed by / from / to"
```

## How to run locally
**Prerequisites:** Java 17, Maven, Node.js (YOUR_NODE_VERSION) and MySQL 8.

**1. Clone**
```bash
git clone https://github.com/Monish-Iyyappan/smarthelpdesk.git
cd smarthelpdesk
```

**2. Create the database**
```bash
mysql -u root -p -e "CREATE DATABASE smart_helpdesk"
mysql -u root -p smart_helpdesk < database/schema.sql
mysql -u root -p smart_helpdesk < database/migration_assignment.sql
mysql -u root -p smart_helpdesk < database/migration_reopen.sql
mysql -u root -p smart_helpdesk < database/seed_data.sql
```
Check that the order matches your repo. If `schema.sql` already includes the migrations, skip those two lines.

**3. Run the backend** (http://localhost:8080)
```bash
cd YOUR_BACKEND_FOLDER
# set the DB username and password as environment variables
mvn spring-boot:run
```

**4. Run the frontend** (http://localhost:3000)
```bash
cd YOUR_FRONTEND_FOLDER
npm install
npm run dev
```

**5. Run the tests**
```bash
mvn test
```

## Demo accounts
| Role | Email | Password |
|---|---|---|
| Admin | admin@college.example | Test@1234 |
| Agent | agent1@college.example | Test@1234 |
| Student | student1@college.example | Test@1234 |

These are demo accounts with sample data only. Do not reuse this password anywhere real.

## Roadmap
Email or SMS alerts, login rate limiting and lockout, SLA and reassignment reports, sub-categories, agent routing by skill, a mobile app (PWA), AI auto-categorization.

## Author
**Monish Iyyappan** · [GitHub](https://github.com/Monish-Iyyappan)
