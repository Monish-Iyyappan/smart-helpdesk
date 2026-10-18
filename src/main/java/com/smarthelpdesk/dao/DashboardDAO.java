package com.smarthelpdesk.dao;

import com.smarthelpdesk.model.Ticket;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for fetching system dashboard metrics from the database.
 * Uses constructor injection with JdbcTemplate and parameterized/constant SQL.
 */
@Repository
public class DashboardDAO {

    private final JdbcTemplate jdbcTemplate;

    // Inject JdbcTemplate using constructor injection
    public DashboardDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Retrieves the current connected MySQL database name.
     */
    public String getDatabaseName() {
        String sql = "SELECT DATABASE()";
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    /**
     * Counts users grouped by role (STUDENT, AGENT, ADMIN).
     * Missing roles default to 0.
     */
    public Map<String, Integer> getUserCountsByRole() {
        Map<String, Integer> roleCounts = new LinkedHashMap<>();
        // Initialize standard roles with 0
        roleCounts.put("STUDENT", 0);
        roleCounts.put("AGENT", 0);
        roleCounts.put("ADMIN", 0);

        String sql = "SELECT role, COUNT(*) AS cnt FROM users GROUP BY role";
        jdbcTemplate.query(sql, rs -> {
            String role = rs.getString("role");
            if (role != null) {
                roleCounts.put(role.toUpperCase(), rs.getInt("cnt"));
            }
        });
        return roleCounts;
    }

    /**
     * Counts tickets grouped by status (OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED).
     * Missing statuses default to 0.
     */
    public Map<String, Integer> getTicketCountsByStatus() {
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        // Initialize all 5 statuses with 0
        statusCounts.put("OPEN", 0);
        statusCounts.put("ASSIGNED", 0);
        statusCounts.put("IN_PROGRESS", 0);
        statusCounts.put("RESOLVED", 0);
        statusCounts.put("CLOSED", 0);

        String sql = "SELECT status, COUNT(*) AS cnt FROM tickets GROUP BY status";
        jdbcTemplate.query(sql, rs -> {
            String status = rs.getString("status");
            if (status != null) {
                statusCounts.put(status.toUpperCase(), rs.getInt("cnt"));
            }
        });
        return statusCounts;
    }

    /**
     * Returns the total count of reported campus incidents.
     */
    public int getIncidentCount() {
        String sql = "SELECT COUNT(*) FROM incidents";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Counts tickets grouped by priority (LOW, MEDIUM, HIGH, URGENT).
     * Missing priorities default to 0.
     */
    public Map<String, Integer> getTicketCountsByPriority() {
        Map<String, Integer> priorityCounts = new LinkedHashMap<>();
        priorityCounts.put("LOW", 0);
        priorityCounts.put("MEDIUM", 0);
        priorityCounts.put("HIGH", 0);
        priorityCounts.put("URGENT", 0);

        String sql = "SELECT priority, COUNT(*) AS cnt FROM tickets GROUP BY priority";
        jdbcTemplate.query(sql, rs -> {
            String priority = rs.getString("priority");
            if (priority != null) {
                priorityCounts.put(priority.toUpperCase(), rs.getInt("cnt"));
            }
        });
        return priorityCounts;
    }

    /**
     * Retrieves the 5 newest tickets ordered by created_at DESC, ticket_id DESC.
     */
    public List<Ticket> getRecentTickets() {
        String sql = "SELECT ticket_id, ticket_number, title, status FROM tickets " +
                     "ORDER BY created_at DESC, ticket_id DESC LIMIT 5";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Ticket ticket = new Ticket();
            ticket.setTicketId(rs.getInt("ticket_id"));
            ticket.setTicketNumber(rs.getString("ticket_number"));
            ticket.setTitle(rs.getString("title"));
            ticket.setStatus(rs.getString("status"));
            return ticket;
        });
    }
}
