package com.smarthelpdesk.service;

import com.smarthelpdesk.dao.DashboardDAO;
import com.smarthelpdesk.dto.UrgencySummaryResponse;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.priority.UrgencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service that collects metrics from DashboardDAO and formats an ASCII startup dashboard.
 * Formats output strictly within a 52-character width.
 */
@Service
public class DashboardService {

    private static final int DASHBOARD_WIDTH = 52;
    private static final String LINE_EQUALS = "====================================================";
    private static final String LINE_DASHES = "----------------------------------------------------";
    private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/smart_helpdesk";

    private final DashboardDAO dashboardDAO;
    private final UrgencyService urgencyService;
    private final String configuredPassword;
    private final String configuredUrl;
    private boolean databaseReachable = false;

    // Inject DashboardDAO, UrgencyService, and configured datasource properties
    @Autowired
    public DashboardService(DashboardDAO dashboardDAO, UrgencyService urgencyService,
                            @Value("${spring.datasource.password:}") String configuredPassword,
                            @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/smart_helpdesk}") String configuredUrl) {
        this.dashboardDAO = dashboardDAO;
        this.urgencyService = urgencyService;
        this.configuredPassword = configuredPassword != null ? configuredPassword : "";
        this.configuredUrl = (configuredUrl != null && !configuredUrl.isBlank()) ? configuredUrl : DEFAULT_DB_URL;
    }

    public DashboardService(DashboardDAO dashboardDAO, UrgencyService urgencyService, String configuredPassword) {
        this(dashboardDAO, urgencyService, configuredPassword, DEFAULT_DB_URL);
    }

    public DashboardService(DashboardDAO dashboardDAO, UrgencyService urgencyService) {
        this(dashboardDAO, urgencyService, "", DEFAULT_DB_URL);
    }

    /**
     * Checks if the database was reachable during the last dashboard generation.
     */
    public boolean isDatabaseReachable() {
        return databaseReachable;
    }

    /**
     * Builds and returns the complete ASCII terminal dashboard as a String.
     */
    public String buildDashboard() {
        try {
            // 1. Fetch data from DAO
            String dbName = dashboardDAO.getDatabaseName();
            Map<String, Integer> userCounts = dashboardDAO.getUserCountsByRole();
            Map<String, Integer> ticketCounts = dashboardDAO.getTicketCountsByStatus();
            int totalIncidents = dashboardDAO.getIncidentCount();
            List<Ticket> recentTickets = dashboardDAO.getRecentTickets();

            this.databaseReachable = true;

            int students = userCounts.getOrDefault("STUDENT", 0);
            int agents = userCounts.getOrDefault("AGENT", 0);
            int admins = userCounts.getOrDefault("ADMIN", 0);
            int totalUsers = students + agents + admins;

            int openTickets = ticketCounts.getOrDefault("OPEN", 0);
            int assignedTickets = ticketCounts.getOrDefault("ASSIGNED", 0);
            int inProgressTickets = ticketCounts.getOrDefault("IN_PROGRESS", 0);
            int resolvedTickets = ticketCounts.getOrDefault("RESOLVED", 0);
            int closedTickets = ticketCounts.getOrDefault("CLOSED", 0);
            int totalTickets = openTickets + assignedTickets + inProgressTickets + resolvedTickets + closedTickets;

            UrgencySummaryResponse urgencySummary = urgencyService.getSummary();
            int overdueTickets = urgencySummary.totalOverdue();
            int urgentTickets = urgencySummary.countsByLevel().getOrDefault("URGENT", 0);

            // 2. Format the 52-character ASCII dashboard
            StringBuilder sb = new StringBuilder();
            sb.append(LINE_EQUALS).append("\n");
            sb.append(center("SMART HELPDESK SYSTEM", DASHBOARD_WIDTH)).append("\n");
            sb.append(center("College Support Portal", DASHBOARD_WIDTH)).append("\n");
            sb.append(LINE_EQUALS).append("\n");

            // Database status section (database name only, never credentials or URL)
            sb.append(String.format("DATABASE STATUS : %s%n", dbName != null ? dbName : "CONNECTED"));

            // System summary section
            sb.append(LINE_DASHES).append("\n");
            sb.append("SYSTEM SUMMARY\n");
            sb.append(LINE_DASHES).append("\n");
            sb.append(String.format("  Total Users     : %d%n", totalUsers));
            sb.append(String.format("    Students      : %d%n", students));
            sb.append(String.format("    Agents        : %d%n", agents));
            sb.append(String.format("    Admins        : %d%n", admins));
            sb.append(String.format("  Total Tickets   : %d%n", totalTickets));
            sb.append(String.format("    Open          : %d%n", openTickets));
            sb.append(String.format("    Assigned      : %d%n", assignedTickets));
            sb.append(String.format("    In Progress   : %d%n", inProgressTickets));
            sb.append(String.format("    Resolved      : %d%n", resolvedTickets));
            sb.append(String.format("    Closed        : %d%n", closedTickets));
            sb.append(String.format("  Overdue Tickets : %d%n", overdueTickets));
            sb.append(String.format("  Urgent Tickets  : %d%n", urgentTickets));
            sb.append(String.format("  Total Incidents : %d%n", totalIncidents));

            // Recent tickets section
            sb.append(LINE_DASHES).append("\n");
            sb.append("RECENT TICKETS\n");
            sb.append(LINE_DASHES).append("\n");

            if (recentTickets.isEmpty()) {
                sb.append("  No tickets found\n");
            } else {
                sb.append(String.format("%-4s %-10s %-22s %-11s%n", "ID", "TICKET NO", "TITLE", "STATUS"));
                for (Ticket t : recentTickets) {
                    String title = t.getTitle() != null ? t.getTitle() : "";
                    // Truncate title to 20 characters with "..." if longer
                    if (title.length() > 20) {
                        title = title.substring(0, 17) + "...";
                    }
                    sb.append(String.format("%-4d %-10s %-22s %-11s%n",
                            t.getTicketId(),
                            t.getTicketNumber() != null ? t.getTicketNumber() : "-",
                            title,
                            t.getStatus() != null ? t.getStatus() : "-"));
                }
            }

            sb.append(LINE_EQUALS);
            return sb.toString();

        } catch (DataAccessException ex) {
            // Handle database unavailability gracefully without exposing credentials or stack traces
            this.databaseReachable = false;

            StringBuilder sb = new StringBuilder();
            sb.append(LINE_EQUALS).append("\n");
            sb.append(center("SMART HELPDESK SYSTEM", DASHBOARD_WIDTH)).append("\n");
            sb.append(center("College Support Portal", DASHBOARD_WIDTH)).append("\n");
            sb.append(LINE_EQUALS).append("\n");
            sb.append("DATABASE STATUS : NOT CONNECTED\n");

            String hint = classifyFailure(ex);
            appendWrapped(sb, "Hint: " + hint);
            appendWrapped(sb, formatCause(ex));
            appendWrapped(sb, formatHost());
            sb.append(LINE_EQUALS);
            return sb.toString();
        }
    }

    /**
     * Classifies the connection failure into a friendly hint line.
     * Never exposes database URL, username, or password.
     */
    public String classifyFailure(Throwable ex) {
        String allMessages = extractAllMessages(ex).toLowerCase();

        // 1. Access denied
        if (allMessages.contains("access denied") || hasSqlState(ex, "28000") || hasErrorCode(ex, 1045)) {
            if (configuredPassword == null || configuredPassword.isBlank()) {
                return "DB_PASSWORD is not set in this terminal";
            } else {
                return "Wrong MySQL username or password";
            }
        }

        // 2. Unknown database
        if (allMessages.contains("unknown database") || allMessages.contains("database not found")
                || hasSqlState(ex, "42000") || hasErrorCode(ex, 1049)) {
            return "Database not found; create it from schema.sql";
        }

        // 3. Communications link failure
        if (allMessages.contains("communications link failure")
                || allMessages.contains("connection refused")
                || allMessages.contains("connectexception")
                || allMessages.contains("port 3306")
                || hasSqlState(ex, "08S01") || hasSqlState(ex, "08001")) {
            return "MySQL is not running or port 3306 is wrong";
        }

        return "Database is unreachable or connection failed";
    }

    private String extractAllMessages(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable cur = ex;
        while (cur != null) {
            if (cur.getMessage() != null) {
                sb.append(" ").append(cur.getMessage());
            }
            cur = cur.getCause();
        }
        return sb.toString();
    }

    private boolean hasSqlState(Throwable ex, String targetState) {
        Throwable cur = ex;
        while (cur != null) {
            if (cur instanceof SQLException sqlEx) {
                if (targetState.equalsIgnoreCase(sqlEx.getSQLState())) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    private boolean hasErrorCode(Throwable ex, int targetCode) {
        Throwable cur = ex;
        while (cur != null) {
            if (cur instanceof SQLException sqlEx) {
                if (sqlEx.getErrorCode() == targetCode) {
                    return true;
                }
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * Extracts the deepest root cause from a throwable chain.
     */
    public Throwable getDeepestCause(Throwable ex) {
        Throwable root = ex;
        while (root != null && root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }

    /**
     * Formats the cause line for the deepest root cause:
     * "Cause: <ExceptionClassSimpleName>: <message>"
     * with any jdbc: URL, user:password@ pair or password-like text removed.
     */
    public String formatCause(Throwable ex) {
        Throwable root = getDeepestCause(ex);
        if (root == null) {
            return "Cause: Unknown";
        }
        String className = root.getClass().getSimpleName();
        String msg = root.getMessage();
        String sanitized = sanitizeMessage(msg);
        if (sanitized.isEmpty()) {
            return "Cause: " + className;
        }
        return "Cause: " + className + ": " + sanitized;
    }

    /**
     * Sanitizes an exception message to remove any jdbc: URL, user:password@ pair, or password-like text.
     */
    private String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String s = message.replaceAll("[\\r\\n]+", " ");

        // 1. Strip configured password if non-blank
        if (configuredPassword != null && !configuredPassword.isBlank()) {
            s = s.replace(configuredPassword, "");
        }

        // 2. Remove any jdbc: URL or web URL
        s = s.replaceAll("(?i)jdbc:[^\\s,\"'>)]+", "");
        s = s.replaceAll("(?i)https?://[^\\s,\"'>)]+", "");

        // 3. Remove user:password@ pair
        s = s.replaceAll("(?i)[^/\\s]+:[^@\\s]+@", "");

        // 4. Remove password-like text
        // MySQL access denied text: (using password: YES) or (using password: NO)
        s = s.replaceAll("(?i)\\(?using password:\\s*(?:YES|NO)\\)?", "");
        // Generic password patterns: password=..., pwd=..., password: ..., password is/was '...'
        s = s.replaceAll("(?i)(?:password|passwd|pwd)\\s*[:=]\\s*(?:'[^']*'|\"[^\"]*\"|\\S+)", "");
        s = s.replaceAll("(?i)(?:password|passwd|pwd)\\s+(?:is|was)\\s+['\"][^'\"]*['\"]", "");

        // Clean up empty parentheses or orphaned whitespace
        s = s.replaceAll("\\(\\s*\\)", "");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    /**
     * Formats the host line:
     * "Host: default localhost:3306 (DB_URL not set)" when the URL equals the built-in default,
     * or "Host: <host>:<port>" parsed from the configured datasource URL (host and port only).
     */
    public String formatHost() {
        if (isDefaultUrl(configuredUrl)) {
            return "Host: default localhost:3306 (DB_URL not set)";
        }
        String hostPort = parseHostPort(configuredUrl);
        return "Host: " + hostPort;
    }

    private boolean isDefaultUrl(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        String u = url.trim();
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u.equalsIgnoreCase(DEFAULT_DB_URL);
    }

    /**
     * Parses host and port from a database URL.
     * Extracts only host and port, discarding credentials, path, and query parameters.
     */
    public String parseHostPort(String url) {
        if (url == null || url.isBlank()) {
            return "localhost:3306";
        }
        String clean = url.trim();
        if (clean.toLowerCase().startsWith("jdbc:")) {
            clean = clean.substring(5);
        }
        Pattern pattern = Pattern.compile("^(?:[a-zA-Z0-9_+.-]+://|//)?(?:[^/\\s]+@)?(?<host>[a-zA-Z0-9.-]+|\\[[a-fA-F0-9:]+\\])(?::(?<port>\\d+))?");
        Matcher matcher = pattern.matcher(clean);
        if (matcher.find()) {
            String host = matcher.group("host");
            String port = matcher.group("port");
            if (host != null && !host.isBlank()) {
                if (port == null || port.isBlank()) {
                    port = "3306";
                }
                return host + ":" + port;
            }
        }
        return "localhost:3306";
    }

    private void appendWrapped(StringBuilder sb, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (text.length() <= DASHBOARD_WIDTH) {
            sb.append(text).append("\n");
            return;
        }
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (currentLine.length() == 0) {
                while (word.length() > DASHBOARD_WIDTH) {
                    sb.append(word, 0, DASHBOARD_WIDTH).append("\n");
                    word = word.substring(DASHBOARD_WIDTH);
                }
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= DASHBOARD_WIDTH) {
                currentLine.append(" ").append(word);
            } else {
                sb.append(currentLine).append("\n");
                currentLine = new StringBuilder();
                while (word.length() > DASHBOARD_WIDTH) {
                    sb.append(word, 0, DASHBOARD_WIDTH).append("\n");
                    word = word.substring(DASHBOARD_WIDTH);
                }
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) {
            sb.append(currentLine).append("\n");
        }
    }

    /**
     * Helper to center text within a fixed character width.
     */
    private String center(String text, int width) {
        if (text == null) {
            return "";
        }
        int padding = Math.max(0, (width - text.length()) / 2);
        int remaining = Math.max(0, width - text.length() - padding);
        return " ".repeat(padding) + text + " ".repeat(remaining);
    }
}
