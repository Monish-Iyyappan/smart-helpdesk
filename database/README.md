# Smart Helpdesk — Database Management Guide

## 1. Project Overview

Smart Helpdesk is a College Support Ticketing System built using Java 17, Spring Boot, Maven, and MySQL.

The database stores users, support tickets, issue categories, shared incidents, ticket participants, and comments.

Database name: `smart_helpdesk`

Storage engine: InnoDB

Character set: `utf8mb4`

## 2. Database Tables and Why They Exist

### 2.1 `users`

**Purpose:** Stores all users of the application.

**Columns:**
- `user_id`: Unique identifier for each user.
- `name`: User's name.
- `email`: Unique email address.
- `password_hash`: Securely stored password hash.
- `role`: Defines whether the user is a STUDENT, AGENT, or ADMIN.
- `created_at`: Date and time the account was created.

**Why is this table needed?**

The system needs to identify who creates tickets, who handles them, and who administers the application. A single table stores all user types without duplicating their common information.

### 2.2 `categories`

**Purpose:** Classifies tickets by the type of college issue.

**Columns:**
- `category_id`: Unique category identifier.
- `category_name`: Category name.

**Why is this table needed?**

Without a separate table, category names would be repeated in every ticket. Storing categories once improves consistency and allows tickets to reference categories through a foreign key.

**Default categories:**
- IT Support
- Examination
- Student Administration
- Fees and Payments
- Transport
- Timetable
- Campus Facilities
- Other

### 2.3 `tickets`

**Purpose:** Stores individual support requests submitted by students.

**Important columns:**
- `ticket_id`: Internal primary key.
- `ticket_number`: Unique, readable identifier such as HD-1001.
- `created_by`: User who created the ticket.
- `assigned_to`: Agent assigned to the ticket; can be NULL initially.
- `category_id`: Category of the issue.
- `incident_id`: Related shared incident; can be NULL.
- `title` and `description`: Details of the reported problem.
- `priority`: LOW, MEDIUM, HIGH, or URGENT.
- `status`: OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, or CLOSED.
- `created_at` and `updated_at`: Timestamps.

**Why is this table needed?**

It is the central table of the application. It tracks the reported issue, its owner, assignment, category, priority, and progress.

A ticket represents an individual support request, even when other students report the same underlying problem.

### 2.4 `ticket_participants`

**Purpose:** Records users who join an existing ticket using its ticket number.

**Columns:**
- `participant_id`: Unique participation record.
- `ticket_id`: Ticket being joined.
- `user_id`: User who joined.
- `joined_at`: Date and time of joining.

**Why is this table needed?**

If multiple students experience the same Wi-Fi problem, they can join one ticket rather than creating duplicate tickets.

The unique constraint on (`ticket_id`, `user_id`) prevents the same user from joining the same ticket more than once.

The ticket creator should also be recorded as a participant when the ticket is created.

### 2.5 `incidents`

**Purpose:** Represents an underlying problem that may affect multiple tickets.

**Columns:**
- `incident_id`: Unique incident identifier.
- `title`: Short description of the shared problem.
- `description`: Details of the incident.
- `location`: Where the problem occurred.
- `status`: OPEN, IN_PROGRESS, RESOLVED, or CLOSED.
- `severity`: LOW, MEDIUM, HIGH, or CRITICAL.
- `created_at` and `updated_at`: Timestamps.

**Why is this table needed?**

For example, a college Wi-Fi outage may affect several classrooms and generate multiple tickets. Those tickets can be linked to one incident so the administrator can coordinate the investigation and resolution.

Incidents also provide a foundation for future priority-based arrangements using severity, affected users, and operational impact.

The affected-student count should initially be calculated from participant records instead of stored as a separate value that might become inconsistent.

### 2.6 `comments`

**Purpose:** Stores messages exchanged about a ticket.

**Columns:**
- `comment_id`: Unique comment identifier.
- `ticket_id`: Ticket associated with the message.
- `user_id`: User who wrote the message.
- `comment_text`: Message content.
- `created_at`: Date and time the message was posted.

**Why is this table needed?**

Students and support agents may need to ask questions, provide updates, or explain solutions. A separate comments table stores each message as its own record and preserves the conversation history.

## 3. Relationships Between Tables

- One user can create many tickets.
- One agent can be assigned many tickets.
- One category can contain many tickets.
- One incident can be linked to many tickets.
- One ticket can have many participants.
- One user can participate in many tickets.
- One ticket can have many comments.
- One user can write many comments.

Primary keys uniquely identify records. Foreign keys maintain valid relationships between tables.

## 4. Running the Schema

### Option A: MySQL Command Line

From the project root, execute:

```powershell
mysql -u root -p < database/schema.sql
```

Enter your MySQL password when prompted.

This creates the database and tables and inserts the default categories.

### Option B: MySQL Workbench

1. Open MySQL Workbench.
2. Connect to the local MySQL Server.
3. Open `database/schema.sql`.
4. Execute the script using the lightning bolt button.
5. Refresh the Schemas panel and locate `smart_helpdesk`.

## 5. Verifying the Database

Execute these SQL commands:

```sql
USE smart_helpdesk;
SHOW TABLES;
SELECT * FROM categories;
```

The six expected tables are:

1. `users`
2. `categories`
3. `tickets`
4. `ticket_participants`
5. `incidents`
6. `comments`

The categories query should return eight default categories.

## 6. Backing Up the Database

Create a backup before significant database changes.

Run this command from the project root:

```powershell
mysqldump -u root -p --single-transaction --routines --triggers --events smart_helpdesk > "database/backup/smart_helpdesk_backup.sql"
```

Enter your MySQL password when prompted.

The backup contains the database structure and records present at the time of export.

Keep another copy outside the project folder, preferably on an external drive or secure cloud storage. Do not commit backup files or credentials to GitHub.

## 7. Restoring a Backup

First, create a separate recovery database:

```sql
CREATE DATABASE smart_helpdesk_recovery;
```

Then import the backup from PowerShell:

```powershell
Get-Content "database/backup/smart_helpdesk_backup.sql" |
    mysql -u root -p smart_helpdesk_recovery
```

Check the recovered tables and records before planning any restoration of the main database.

Do not import a backup directly over the main database without verifying the current data and making a safety copy.

## 8. Git and Backup Safety

The `database/.gitignore` file contains:

```gitignore
backup/*
!backup/.gitkeep
```

This keeps database backup files out of Git while allowing the empty backup directory to be represented in the repository.

The `schema.sql` file should be tracked in Git because it defines the database structure. Real database backups should remain outside version control.

## 9. Important Notes

- `schema.sql` creates the database structure; it does not automatically back up live data.
- `CREATE TABLE IF NOT EXISTS` does not update an existing table when the schema changes.
- Back up the database before schema modifications.
- Do not delete the MySQL data directory when troubleshooting.
- Do not store plain-text passwords.
- Keep database credentials out of source control.
- Test backup restoration periodically.
- A Java application crash does not necessarily mean the MySQL database has been damaged.

## 10. Future Improvements

Possible future enhancements include:
- Automated daily database backups.
- Incident matching and priority-based sorting.
- Ticket status history and audit logging.
- Role-based access control.
- Better incident impact analysis.

These features can be added as the application develops.