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

/**
 * Service that collects metrics from DashboardDAO and formats an ASCII startup dashboard.
 * Formats output strictly within a 52-character width.
 */
@Service
public class DashboardService {

    private static final int DASHBOARD_WIDTH = 52;
    private static final String LINE_EQUALS = "====================================================";
    private static final String LINE_DASHES = "----------------------------------------------------";

    private final DashboardDAO dashboardDAO;
    private final UrgencyService urgencyService;
    private final String configuredPassword;
    private boolean databaseReachable = false;

    // Inject DashboardDAO, UrgencyService, and configured datasource password
    @Autowired
    public DashboardService(DashboardDAO dashboardDAO, UrgencyService urgencyService,
                            @Value("${spring.datasource.password:}") String configuredPassword) {
        this.dashboardDAO = dashboardDAO;
        this.urgencyService = urgencyService;
        this.configuredPassword = configuredPassword != null ? configuredPassword : "";
    }

    public DashboardService(DashboardDAO dashboardDAO, UrgencyService urgencyService) {
        this(dashboardDAO, urgencyService, "");
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

    private void appendWrapped(StringBuilder sb, String text) {
        if (text == null) {
            return;
        }
        if (text.length() <= DASHBOARD_WIDTH) {
            sb.append(text).append("\n");
            return;
        }
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + DASHBOARD_WIDTH, text.length());
            sb.append(text, start, end).append("\n");
            start = end;
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
