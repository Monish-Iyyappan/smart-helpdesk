# Smart Helpdesk UI Specification for Terminal Application (TUI/CLI)

This specification defines the complete UI/UX architecture, screen flows, ASCII wireframes, form schemas, role-based capabilities, data entities, and visual token mappings of the Smart Helpdesk frontend so it can be rebuilt as a terminal application.

---

## 1. Screen List & Navigation Flow

```
                      +-----------------------------+
                      |     1. Authentication       |
                      |   [Sign In] / [Register]    |
                      +--------------+--------------+
                                     |
               +---------------------+---------------------+
               |                     |                     |
     (role: STUDENT)           (role: AGENT)         (role: ADMIN)
               v                     v                     v
    +--------------------+  +--------------------+  +--------------------+
    | 2. Student Portal  |  | 3. Agent Workspace |  | 4. Admin Center    |
    | - My Tickets View  |  | - Kanban Board     |  | - Tab: Stats       |
    |                    |  | - List View        |  | - Tab: All Tickets |
    +---------+----------+  +---------+----------+  | - Tab: Unassigned  |
              |                       |             | - Tab: Overdue     |
              +-----------+-----------+             | - Tab: Incidents   |
                          |                         | - Tab: Agents      |
                          v                         | - Tab: Closed Log  |
              +-----------------------+             +---------+----------+
              | 5. Ticket Detail View |                       |
              |    (Shared Modal)     |<----------------------+
              +-----------+-----------+
                          |
         +----------------+----------------+
         | (Admin only)   | (Admin only)   | (Admin only)
         v                v                v
   +-----------+    +-----------+    +-----------+
   | Reassign  |    |   Close   |    |  Reopen   |
   |  Dialog   |    |  Dialog   |    |  Dialog   |
   +-----------+    +-----------+    +-----------+

Direct Modal Actions:
- From Student Screen: [Create Ticket Form], [Join Ticket Form]
- From Admin Screen:   [Add Agent Form], [Declare Incident Form], [Link Ticket Form]
- Global Navigation:   [Logout] returns to Authentication screen from any portal.
```

---

## 2. Screen Wireframes, Data, Actions & States

### Screen 1: Authentication (`/login`)
```
+--------------------------------------------------------------------------+
|  SMART HELPDESK SYSTEM                       [ [D] Light / Dark Theme ]  |
|  Signal Desk Support Engine                                              |
+--------------------------------------------------------------------------+
|                                                                          |
|       +----------------------------------------------------------+       |
|       |                     [ SH ] LOGO                          |       |
|       |             Sign in to Helpdesk Portal                   |       |
|       |                                                          |       |
|       |   < [1] Sign In >                [2] Register            |       |
|       |  ------------------------------------------------------  |       |
|       |   [DEV QUICK-FILL]:  [1] Student  [2] Agent  [3] Admin   |       |
|       |  ------------------------------------------------------  |       |
|       |   Email Address:    [ user@college.example             ] |       |
|       |   Password:         [ ********            ] [Show/Hide]  |       |
|       |                                                          |       |
|       |   [                 SIGN IN (Enter)                    ] |       |
|       +----------------------------------------------------------+       |
|                                                                          |
+--------------------------------------------------------------------------+
```
* **Data Shown**: Active tab mode, email input, password (masked), validation states, dev quick-fill buttons.
* **User Actions**:
  - `[Tab]`: Switch between Sign In and Register modes.
  - `[1/2/3]`: Fast-fill demo credentials (student, agent, admin) when dev mode is active.
  - `[V]`: Toggle password visibility.
  - `[Enter]`: Submit credentials.
* **States**:
  - *Loading*: Button displays `[ * ] Signing in...` / `Creating account...` (disabled).
  - *Error*: Red banner with alert icon: `"Login failed. Please check your email and password."`
  - *Success*: Green toast notification: `"Account created! Welcome to Smart Helpdesk."`

---

### Screen 2: Student Portal (`/student`)
```
+--------------------------------------------------------------------------+
| MY TICKETS                       [JD] Jane Doe (STUDENT)    [Logout (Q)] |
+--------------------------------------------------------------------------+
| WELCOME BACK, JANE DOE                                                   |
| Track submitted requests, join shared tickets, and check live updates.   |
|                                    [ (J) Join Ticket ] [ (C) + New Ticket]
+--------------------------------------------------------------------------+
| [ STATS ]                                                                |
| Total Tickets: 4  | Active In Flight: 2 | Resolved: 2 | Overdue SLA: 0   |
+--------------------------------------------------------------------------+
| SEARCH: [ Search tickets by title, #HD-1001, or category...            ] |
| STATUS: <[All (4)]>  [Open (1)]  [Assigned (1)]  [Resolved (2)]          |
| SORT:   [ Newest First v ]                                               |
+--------------------------------------------------------------------------+
| TICKET LIST:                                                             |
| ------------------------------------------------------------------------ |
| [!] HD-1042  * Network WiFi         [IN_PROGRESS] [HIGH Urgency (78)]    |
|     WiFi drops intermittently in Library 2nd floor                       |
|     Requested: HIGH | Agent: Alex Smith | Created: 10/28/2026            |
| ------------------------------------------------------------------------ |
|     HD-1011  * Hardware / Lab       [RESOLVED]    [LOW Urgency (15)]     |
|     Projector bulb flickering in Seminar Hall A                          |
|     Requested: LOW  | Agent: Bob Taylor | Created: 10/22/2026            |
+--------------------------------------------------------------------------+
| Controls: [Up/Down] Navigate | [Enter] Open Ticket Detail | [C] New Ticket |
+--------------------------------------------------------------------------+
```
* **Data Shown**:
  - User initials, full name, role badge (`STUDENT`).
  - Stat counters: Total, Active in flight (OPEN/ASSIGNED/IN_PROGRESS), Resolved/Closed, Overdue alerts.
  - Ticket row items: Ticket Number (`HD-XXXX`), Category, Status badge, Urgency score & level, Overdue tag, Title, Description snippet (max 75 chars), Requested priority, Assigned agent name, Created date.
* **User Actions**:
  - `[C]`: Open Create Ticket Form modal.
  - `[J]`: Open Join Shared Ticket modal.
  - `[/]`: Focus Search filter.
  - `[1-5]`: Quick status filter tabs (`ALL`, `OPEN`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`).
  - `[Up/Down] + [Enter]`: Select ticket to view full conversation and detail.
* **States**:
  - *Loading*: 3 animated card skeleton placeholders (`[====] Loading ticket data...`).
  - *Empty (no tickets)*: ASCII graphic with message `"No Tickets Found. Create your first ticket to get started."`
  - *Empty (search/filter)*: `"No tickets match your filter. [Reset Filters]"`

---

### Screen 3: Agent Workspace (`/agent`)
```
+--------------------------------------------------------------------------+
| AGENT WORKSPACE                  [AS] Alex Smith (AGENT)    [Logout (Q)] |
+--------------------------------------------------------------------------+
| MANAGE QUEUE & SLA TARGETS                 VIEW: < [1] Kanban >  [2] List |
+--------------------------------------------------------------------------+
| [ STATS ]                                                                |
| Assigned Queue: 3 | In Progress: 2 | Resolved: 5 | Overdue SLA: 1 [!]    |
+--------------------------------------------------------------------------+
| SEARCH: [ Filter by title, #HD-1001, category... ]    [X] Overdue Only   |
| SORT:   [ Urgency Level (Highest First) v ]                              |
+--------------------------------------------------------------------------+
| KANBAN BOARD:                                                            |
| +---------------------+ +---------------------+ +----------------------+ |
| | ASSIGNED (3)        | | IN PROGRESS (2)     | | RESOLVED (5)         | |
| +---------------------+ +---------------------+ +----------------------+ |
| | [!] HD-1045  [HIGH] | | HD-1042  [HIGH]     | | HD-1020  [LOW]       | |
| | VPN authentication  | | WiFi drops Library  | | Lab password reset   | |
| | Req: HIGH           | | Req: HIGH           | | Req: LOW             | |
| | [ Start Work (S) ]  | | [ Mark Resolved (R)]| | (Closed by Student)  | |
| |---------------------| |---------------------| |----------------------| |
| | HD-1050  [MED]      | |                     | |                      | |
| | Canvas portal error | |                     | |                      | |
| | Req: MED            | |                     | |                      | |
| | [ Start Work (S) ]  | |                     | |                      | |
| +---------------------+ +---------------------+ +----------------------+ |
| Controls: [Tab] Switch Columns | [Up/Down] Pick Ticket | [Enter] Details |
+--------------------------------------------------------------------------+
```
* **Data Shown**:
  - Agent personal queue stats (Assigned, In Progress, Resolved, Overdue).
  - Kanban columns: `ASSIGNED` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `RESOLVED`.
  - Ticket cards with urgency badge, SLA status, and inline fast-action buttons.
* **User Actions**:
  - `[1/2]`: Toggle Kanban vs. List view.
  - `[O]`: Toggle "Overdue Only" filter.
  - `[S]`: Fast-action "Start Work" (advances ASSIGNED $\rightarrow$ IN_PROGRESS).
  - `[R]`: Fast-action "Resolve" (advances IN_PROGRESS $\rightarrow$ RESOLVED).
  - `[Enter]`: View full Ticket Detail dialog.
* **States**:
  - *Empty Column*: Box with dashed border: `[ No tickets in this stage ]`.
  - *Loading*: Column skeleton placeholders.

---

### Screen 4: Admin Center (`/admin`)
```
+--------------------------------------------------------------------------+
| ADMIN CENTER                      [AD] Admin User (ADMIN)   [Logout (Q)] |
+--------------------------------------------------------------------------+
| ACTIONS:  [ (A) Add Agent ]    [ (L) Link Ticket ]    [ (N) + Declare Inc]
| TABS:     <[1] Stats> [2] All Tickets [3] Unassigned [4] Overdue [5] Inc |
|           [6] Agents  [7] Closed Log                                     |
+--------------------------------------------------------------------------+
| TAB 1: SYSTEM OVERVIEW & METRICS                                         |
|                                                                          |
| Total Tickets: 48   Unassigned: 6      Overdue SLA: 3 [!]  Incidents: 2  |
| ------------------------------------------------------------------------ |
| URGENCY BREAKDOWN:                                                       |
| [CRITICAL/URGENT]: 3   [HIGH]: 8       [MEDIUM]: 22        [LOW]: 15     |
| ------------------------------------------------------------------------ |
| STATUS DISTRIBUTION:                   USERS BY ROLE:                    |
| - OPEN:        6                       - STUDENT:  120                   |
| - ASSIGNED:   14                       - AGENT:      8                   |
| - IN_PROGRESS:12                       - ADMIN:      2                   |
| - RESOLVED:   10                                                         |
| - CLOSED:      6                                                         |
+--------------------------------------------------------------------------+
| TAB 5: CAMPUS INCIDENTS & LINKED TICKETS VIEW:                           |
| ------------------------------------------------------------------------ |
| INCIDENTS (Left Column)           | LINKED TICKETS (Right Column)        |
| > INC-1: Science Block Power Cut  | Selected: INC-1 (Tickets: 4 | Open: 2|
|   Severity: CRITICAL (+30 pts)    | ------------------------------------ |
|   Tickets: 4 (2 Open)             | * HD-1048 [OPEN] Chem Lab Server     |
|                                   | * HD-1049 [IN_PROGRESS] Hood Sensors |
|   INC-2: Campus WiFi Authentication| * HD-1040 [RESOLVED] Physics Lab PC  |
|   Severity: HIGH (+20 pts)        |                                      |
+--------------------------------------------------------------------------+
| Controls: [1-7] Select Tab | [N] Declare Incident | [A] Add Agent        |
+--------------------------------------------------------------------------+
```
* **Data Shown**:
  - Global helpdesk metrics, SLA compliance rate, active incidents.
  - Unassigned ticket triage queue.
  - Incident management split view: Left = Incident list; Right = Linked ticket sub-panel.
  - Active agents directory table (Name, Email, Specialization Category, Workload count).
  - Closed ticket audit log (Closure note, closed by, reassignment count).
* **User Actions**:
  - `[1-7]`: Switch between Admin tabs.
  - `[N]`: Declare new campus-wide incident.
  - `[A]`: Add new support agent.
  - `[L]`: Link ticket to an existing incident.
  - `[Up/Down]`: Browse records in active tab.
* **States**:
  - *Empty State*: `"No unassigned tickets pending triage."`
  - *Error State*: Red error banner inside the incident sub-panel if ticket fetch fails.

---

### Screen 5: Ticket Detail & Conversation View (Shared View / Modal)
```
+==========================================================================+
| TICKET DETAILS: HD-1042                                     [ Close (X) ]|
+==========================================================================+
| STATUS: [IN_PROGRESS]   OVERDUE: [NO]   URGENCY: [HIGH (78 pts)]         |
| SLA DEADLINE: 10/29/2026, 4:00 PM (18h remaining)                        |
+--------------------------------------------------------------------------+
| PROGRESS: (1) Open === (2) Assigned === [3] In Progress --- (4) Resolved |
+--------------------------------------------------------------------------+
| Title:     WiFi connection drops intermittently in Library 2nd floor     |
| Category:  Network Infrastructure        Creator: Jane Doe (Student)     |
| Priority:  Requested: HIGH               Assigned: Alex Smith (Agent)    |
| Incident:  INC-2: Campus WiFi Auth Failure                               |
+--------------------------------------------------------------------------+
| DESCRIPTION:                                                             |
| Whenever more than 20 students connect to AP-Lib-02, all connections     |
| disconnect with authentication timeout error 0x44.                       |
+--------------------------------------------------------------------------+
| URGENCY ANALYSIS & SLA FACTORS (Staff / Admin):                          |
| - Keyword boost: 'drops intermittently', 'authentication timeout' (+25)  |
| - Linked Incident: INC-2 Campus WiFi Severity HIGH (+20)                 |
| - Student requested priority: HIGH (+20)                                 |
+--------------------------------------------------------------------------+
| ADMIN / AGENT ACTIONS:                                                   |
| [ Advance to RESOLVED (R) ]  [ Reassign Agent (A) ]  [ Close Ticket (C) ]|
+--------------------------------------------------------------------------+
| COMMENT TIMELINE (2 comments):                                           |
| ------------------------------------------------------------------------ |
| [10/28 10:15 AM] Jane Doe (Student):                                     |
| "Issue started after the library opened this morning."                   |
|                                                                          |
| [10/28 11:30 AM] Alex Smith (Agent) [STAFF]:                             |
| "Investigating channel contention on Access Point 02."                   |
+--------------------------------------------------------------------------+
| ADD COMMENT:                                                             |
| Write comment: [                                                       ] |
| [ ] Mark as student issue note                                           |
| [ Post Comment (Enter) ]                                                 |
+==========================================================================+
```
* **Data Shown**:
  - Ticket Number, Status progression dot indicator, SLA timers.
  - Full title, metadata tags, incident association.
  - Prose description (max 75 chars per line).
  - Calculated urgency factor breakdown (for Agent/Admin).
  - Chronological comment timeline with author role, staff badge, timestamp, and message type.
* **User Actions**:
  - `[Enter]`: Post new comment or issue note.
  - `[R]`: Resolve ticket (Agent/Admin).
  - `[A]`: Reassign ticket (Admin).
  - `[C]`: Close resolved ticket with closure note (Admin).
  - `[O]`: Reopen closed/resolved ticket with reason (Admin).
  - `[H]`: Toggle assignment audit history log (Admin).
  - `[Esc/X]`: Close detail view.

---

## 3. Form Schemas & Validation Rules

| Form Name | Field Label | Data Type | UI Input Widget | Validation Rules & Constraints | Required? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Sign In** | Email Address | String | Text Input | Valid RFC email regex, auto-trimmed | **Required** |
| | Password | String | Masked Password | Min 1 character | **Required** |
| **Registration** | Full Name | String | Text Input | Non-empty, trimmed | **Required** |
| | Email Address | String | Text Input | Valid email regex (`user@college.example`) | **Required** |
| | Password | String | Masked Password | Min 6 characters | **Required** |
| **Create Ticket** | Category | Integer | Select Dropdown | Must select an existing category ID | **Required** |
| | Title | String | Text Input | 5–100 characters, trimmed | **Required** |
| | Description | String | Textarea (4 rows) | 10–1000 characters, trimmed | **Required** |
| | Requested Priority | String | Select Dropdown | Enum: `LOW`, `MEDIUM`, `HIGH`, `URGENT` | **Required** (default: `MEDIUM`) |
| **Join Ticket** | Ticket Number | String | Monospace Input | Auto-trimmed, auto-uppercased (`HD-XXXX`) | **Required** |
| **Add Comment** | Comment Text | String | Textarea (2 rows) | Non-empty, trimmed | **Required** |
| | Mark as Issue | Boolean | Checkbox Toggle | Only visible for `STUDENT` role | Optional |
| **Reassign Ticket** | Target Agent | Integer | Select Dropdown | Agent ID $\ne$ currently assigned agent ID | **Required** |
| | Reassignment Reason| String | Textarea (3 rows) | 10–500 characters, trimmed | **Required** |
| **Close Ticket** | Closure Note | String | Textarea (3 rows) | 10–500 characters, trimmed | **Required** |
| **Reopen Ticket** | Reopen Reason | String | Textarea (3 rows) | 10–500 characters, trimmed | **Required** |
| **Declare Incident**| Incident Title | String | Text Input | Non-empty, trimmed | **Required** |
| | Location | String | Text Input | Non-empty campus location | **Required** |
| | Severity Level | String | Select Dropdown | Enum: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` | **Required** (default: `MEDIUM`) |
| | Description | String | Textarea (3 rows) | Non-empty description of scope & impact | **Required** |
| **Add Agent** | Agent Name | String | Text Input | Non-empty full name | **Required** |
| | Agent Email | String | Text Input | Valid college email regex | **Required** |
| | Temp Password | String | Masked Password | Min 6 characters | **Required** |
| **Link Ticket** | Incident | Integer | Select Dropdown | Select existing active Incident ID | **Required** |
| | Ticket to Link | Integer | Select Dropdown | Select existing Ticket ID | **Required** |

---

## 4. Role-Based Access Control (RBAC) Matrix

| Feature / Action | Student (`STUDENT`) | Support Agent (`AGENT`) | Administrator (`ADMIN`) |
| :--- | :---: | :---: | :---: |
| Self-Registration & Sign In | Yes | Sign In only | Sign In only |
| Submit New Ticket | Yes | No | Yes (via API) |
| Join Existing Ticket (`HD-XXXX`) | Yes | No | No |
| View Ticket List | Own tickets only | Assigned category queue only | All campus tickets |
| View Urgency Scoring Factors | No (Calculated hidden) | Yes | Yes |
| View Overdue SLA Countdown | Yes | Yes | Yes |
| Post Public Comments | Yes | Yes | Yes |
| Post "Issue Note" Comment Type | Yes | No | No |
| Advance Status: `ASSIGNED` $\rightarrow$ `IN_PROGRESS` | No | Yes | Yes |
| Advance Status: `IN_PROGRESS` $\rightarrow$ `RESOLVED` | No | Yes | Yes |
| Reassign Ticket to Different Agent | No | No | Yes (Requires reason) |
| Close Ticket (`RESOLVED` $\rightarrow$ `CLOSED`) | No | No | Yes (Requires note) |
| Reopen Ticket (`RESOLVED` $\rightarrow$ `IN_PROGRESS`) | No | No | Yes (Requires reason) |
| View Audit Assignment History | No | No | Yes |
| Declare / Manage Campus Incidents | View linked incident only | View linked incident only | Yes (Full CRUD) |
| Create Support Agent Accounts | No | No | Yes |
| View Closed Ticket Audit Log | No | No | Yes |
| View System Analytics & Distribution | No | No | Yes |

---

## 5. Frontend Data Entities & Fields

### `User`
* `userId` (number): Unique user identifier.
* `name` (string): Full name.
* `email` (string): Login email address.
* `role` (enum): `'STUDENT' | 'AGENT' | 'ADMIN'`.
* `createdAt` (ISO-8601 string): Account registration date.

### `TicketItem` / `TicketDetail`
* `ticketId` (number): Internal primary key.
* `ticketNumber` (string): Public business reference (e.g., `"HD-1001"`).
* `title` (string): Issue headline.
* `description` (string): Full narrative of the problem.
* `categoryName` (string): e.g. `"Network"`, `"Hardware"`, `"Software"`.
* `requestedPriority` (enum): Priority chosen by creator (`'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'`).
* `urgencyLevel` (enum): Algorithmic rating (`'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'`).
* `urgencyScore` (number): Calculated 0–100 integer score.
* `overdue` (boolean): Flag indicating SLA target resolution breach.
* `dueAt` (ISO-8601 string | null): SLA deadline timestamp.
* `reasons` (string[] | undefined): List of textual rule triggers explaining urgency.
* `status` (enum): `'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'`.
* `createdByName` / `creatorName` (string): Author's full name.
* `assignedTo` / `assignedAgentId` (number | null): ID of assignee.
* `assignedAgentName` (string | null): Full name of assigned agent.
* `incidentId` (number | null): Foreign key to linked incident.
* `incidentTitle` (string | null): Headline of linked campus incident.
* `participantCount` (number): Number of students tracking this ticket.
* `comments` (CommentDetail[]): Array of chronological conversation notes.

### `CommentDetail`
* `commentId` (number): Unique comment ID.
* `ticketId` (number): Parent ticket ID.
* `userId` (number): Author user ID.
* `authorName` (string): Author display name.
* `commentText` (string): Comment content.
* `commentType` (enum): `'GENERAL' | 'ISSUE' | 'REASSIGNMENT' | 'CLOSURE' | 'REOPEN'`.
* `createdAt` (ISO-8601 string): Post timestamp.

### `IncidentItem`
* `incidentId` (number): Unique incident ID.
* `title` (string): Incident headline (e.g., `"Campus WiFi Outage"`).
* `description` (string): Scope and diagnostic summary.
* `location` (string): Physical campus location (e.g., `"Library Complex"`).
* `status` (string): e.g., `"ACTIVE"`, `"RESOLVED"`.
* `severity` (enum): `'LOW'` (+5) | `'MEDIUM'` (+10) | `'HIGH'` (+20) | `'CRITICAL'` (+30).
* `ticketCount` (number): Total tickets linked to this incident.
* `openTicketCount` (number): Number of linked tickets not yet resolved/closed.

### `AssignmentRecord`
* `historyId` (number): Audit log ID.
* `ticketId` (number): Associated ticket ID.
* `fromAgentName` (string | null): Previous agent.
* `toAgentName` (string): New assignee.
* `changedByName` (string): Admin who performed the reassignment.
* `assignmentType` (enum): `'ASSIGN' | 'REASSIGN'`.
* `reason` (string | null): Justification submitted by admin.
* `changedAt` (ISO-8601 string): Audit timestamp.

---

## 6. Visual Tokens, Colors & Status Labels

### Ticket Status Tokens
| Ticket Status | Visual Label | ANSI / Terminal Color | Background / Hex Reference | Meaning |
| :--- | :--- | :--- | :--- | :--- |
| **`OPEN`** | `OPEN` | Blue / Cyan (`\033[34m`) | `#312e81` on `#eef2ff` | Submitted by student, unassigned |
| **`ASSIGNED`** | `ASSIGNED` | Magenta / Purple (`\033[35m`) | `#1e1b4b` on `#dedcff` | Assigned to technician category |
| **`IN_PROGRESS`** | `IN PROGRESS` | Yellow / Amber (`\033[33m`) | `#78350f` on `#fef3c7` | Active investigation underway |
| **`RESOLVED`** | `RESOLVED` | Green (`\033[32m`) | `#14532d` on `#dcfce7` | Work finished, pending confirmation |
| **`CLOSED`** | `CLOSED` | Gray / Slate (`\033[90m`) | `#334155` on `#f1f5f9` | Verified and archived |

### Urgency Level & SLA Alert Tokens
| Urgency Level | Numeric Score | ANSI Color | Badge Background | SLA Action Window |
| :--- | :--- | :--- | :--- | :--- |
| **`LOW`** | 0 – 29 pts | Dim Gray (`\033[37m`) | Slate (`#f1f5f9`) | Standard 72h window |
| **`MEDIUM`** | 30 – 59 pts | Yellow (`\033[33m`) | Amber (`#fef3c7`) | Standard 48h window |
| **`HIGH`** | 60 – 79 pts | Light Red (`\033[91m`) | Orange (`#ffedd5`) | Expedited 24h window |
| **`URGENT`** | 80 – 100 pts | Bold Red (`\033[1;31m`) | Red (`#fee2e2`) | Emergency 4h SLA window |
| **`OVERDUE`** | *Breached* | Flashing Red / Bold Red | Red `#dc2626` | Breached SLA deadline |

### Incident Severity Tokens
* **`LOW`**: Blue (`+5` score weight)
* **`MEDIUM`**: Yellow (`+10` score weight)
* **`HIGH`**: Orange / Red (`+20` score weight)
* **`CRITICAL`**: Bold Purple / Flashing Red (`+30` score weight)

---
*End of Smart Helpdesk UI Specification.*
