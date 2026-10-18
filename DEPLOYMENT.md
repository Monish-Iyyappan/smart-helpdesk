# Deployment Guide: Smart Helpdesk Backend on Render

This guide outlines how to deploy the Smart Helpdesk Spring Boot backend to Render with a hosted MySQL database, and connect it to your frontend on Vercel.

---

### 1. Hosted MySQL Setup & Schema Initialization

Most cloud providers (Aiven, PlanetScale, Railway, AWS RDS, DigitalOcean) provide managed MySQL instances.

> [!WARNING]
> **Never run `seed_data.sql` on a public or production database.**
> `seed_data.sql` contains default test accounts and sample data with well-known test passwords. Only run `schema_hosted.sql`.

#### Initialize the Schema with SSL
Run `database/schema_hosted.sql` against your hosted MySQL instance using `mysql.exe`:

**PowerShell:**
```powershell
Get-Content database/schema_hosted.sql | mysql -h HOST -P PORT -u USERNAME -p --ssl-mode=REQUIRED DBNAME
```

**Bash / Command Prompt:**
```bash
mysql -h HOST -P PORT -u USERNAME -p --ssl-mode=REQUIRED DBNAME < database/schema_hosted.sql
```

---

### 2. Configure Render Web Service

1. Create a new **Web Service** on [Render](https://render.com).
2. Connect your repository. Render will automatically detect `render.yaml` or you can configure a **Docker** runtime.
3. In the Render Dashboard, set the following **Environment Variables**:

| Variable | Description & Required Format | Example |
| :--- | :--- | :--- |
| `DB_URL` | **Must use standard JDBC format** with SSL and timezone parameters. Never use provider `mysql://` URIs. | `jdbc:mysql://HOST:PORT/DBNAME?sslMode=REQUIRED&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true` |
| `DB_USERNAME` | Your hosted MySQL username | `avnadmin` or `root` |
| `DB_PASSWORD` | Your hosted MySQL password | *(your database password)* |
| `CORS_ORIGINS` | Exact HTTPS URL of your Vercel deployment **without a trailing slash** | `https://smart-helpdesk.vercel.app` |

> [!IMPORTANT]
> Render sets the `PORT` environment variable automatically. The application is configured to listen on `${PORT:8080}`.
> Render uses `/api/health` as the health check path.

#### Free-Tier Sleep Warning
On Render's free tier, the service automatically spins down after 15 minutes of inactivity. When a new request arrives, it may take 50–60 seconds for the container to wake up (cold start).

---

### 3. Create the First Admin Account

The application schema starts empty (except for categories). To create your initial administrator:

1. Open your frontend or send a `POST /api/auth/register` request to create your account with your desired email and password.
2. Connect to your hosted MySQL database via `mysql.exe`:
   ```bash
   mysql -h HOST -P PORT -u USERNAME -p --ssl-mode=REQUIRED DBNAME
   ```
3. Promote your registered account to `ADMIN`:
   ```sql
   UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';
   ```
4. Log in to the frontend; you now have full Admin access. From the Admin Center, you can create agents, declare incidents, and manage tickets.

---

### 4. Connect Frontend on Vercel

1. In your **Vercel** dashboard, navigate to **Project Settings** > **Environment Variables**.
2. Add or update:
   - `VITE_API_URL`: Your Render backend URL (e.g., `https://smart-helpdesk.onrender.com` without trailing slash).
3. **Redeploy** the latest deployment on Vercel so Vite embeds the new API base URL into the client bundle.
