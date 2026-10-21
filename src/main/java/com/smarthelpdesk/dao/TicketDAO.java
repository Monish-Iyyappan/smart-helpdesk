package com.smarthelpdesk.dao;

import com.smarthelpdesk.dto.ClosedTicketResponse;
import com.smarthelpdesk.model.Ticket;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class TicketDAO {

    private final JdbcTemplate jdbcTemplate;

    public TicketDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Ticket> ticketRowMapper = (rs, rowNum) -> {
        Ticket ticket = new Ticket();
        ticket.setTicketId(rs.getInt("ticket_id"));
        ticket.setTicketNumber(rs.getString("ticket_number"));
        ticket.setCreatedBy(rs.getInt("created_by"));

        int assignedTo = rs.getInt("assigned_to");
        ticket.setAssignedTo(rs.wasNull() ? null : assignedTo);

        ticket.setCategoryId(rs.getInt("category_id"));

        int incidentId = rs.getInt("incident_id");
        ticket.setIncidentId(rs.wasNull() ? null : incidentId);

        ticket.setTitle(rs.getString("title"));
        ticket.setDescription(rs.getString("description"));
        ticket.setPriority(rs.getString("priority"));
        ticket.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            ticket.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            ticket.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return ticket;
    };

    public Integer insert(Ticket ticket) {
        String sql = "INSERT INTO tickets (ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String ticketNum = ticket.getTicketNumber() != null ? ticket.getTicketNumber() : "TMP-" + (System.currentTimeMillis() % 10000000000L);

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, ticketNum);
            ps.setInt(2, ticket.getCreatedBy());
            if (ticket.getAssignedTo() != null) {
                ps.setInt(3, ticket.getAssignedTo());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setInt(4, ticket.getCategoryId());
            if (ticket.getIncidentId() != null) {
                ps.setInt(5, ticket.getIncidentId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, ticket.getTitle());
            ps.setString(7, ticket.getDescription());
            ps.setString(8, ticket.getPriority());
            ps.setString(9, ticket.getStatus());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            int generatedId = key.intValue();
            ticket.setTicketId(generatedId);
            return generatedId;
        }
        return null;
    }

    public Optional<Ticket> findById(Integer id) {
        String sql = "SELECT ticket_id, ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at " +
                     "FROM tickets WHERE ticket_id = ?";
        List<Ticket> results = jdbcTemplate.query(sql, ticketRowMapper, id);
        return results.stream().findFirst();
    }

    public Optional<Ticket> findByIdForUpdate(Integer id) {
        String sql = "SELECT ticket_id, ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at " +
                     "FROM tickets WHERE ticket_id = ? FOR UPDATE";
        List<Ticket> results = jdbcTemplate.query(sql, ticketRowMapper, id);
        return results.stream().findFirst();
    }

    public Optional<Ticket> findByTicketNumber(String ticketNumber) {
        String sql = "SELECT ticket_id, ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at " +
                     "FROM tickets WHERE ticket_number = ?";
        List<Ticket> results = jdbcTemplate.query(sql, ticketRowMapper, ticketNumber);
        return results.stream().findFirst();
    }

    public List<Ticket> findByCreatedBy(Integer createdBy) {
        String sql = "SELECT ticket_id, ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at " +
                     "FROM tickets WHERE created_by = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ticketRowMapper, createdBy);
    }

    public List<Ticket> findByAssignedTo(Integer assignedTo) {
        String sql = "SELECT ticket_id, ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at " +
                     "FROM tickets WHERE assigned_to = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ticketRowMapper, assignedTo);
    }

    public List<Ticket> findAll() {
        String sql = "SELECT ticket_id, ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at " +
                     "FROM tickets ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ticketRowMapper);
    }

    public List<Ticket> findByStatus(String status) {
        String sql = "SELECT ticket_id, ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at " +
                     "FROM tickets WHERE status = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ticketRowMapper, status);
    }

    public int updateStatus(Integer ticketId, String status) {
        String sql = "UPDATE tickets SET status = ? WHERE ticket_id = ?";
        return jdbcTemplate.update(sql, status, ticketId);
    }

    public int assign(Integer ticketId, Integer assignedTo) {
        String sql = "UPDATE tickets SET assigned_to = ?, status = 'ASSIGNED' WHERE ticket_id = ?";
        return jdbcTemplate.update(sql, assignedTo, ticketId);
    }

    public int updateTicketNumber(Integer ticketId, String ticketNumber) {
        String sql = "UPDATE tickets SET ticket_number = ? WHERE ticket_id = ?";
        return jdbcTemplate.update(sql, ticketNumber, ticketId);
    }

    public int linkIncident(Integer ticketId, Integer incidentId) {
        String sql = "UPDATE tickets SET incident_id = ? WHERE ticket_id = ?";
        return jdbcTemplate.update(sql, incidentId, ticketId);
    }

    public List<Ticket> findByIncidentId(Integer incidentId) {
        String sql = "SELECT ticket_id, ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at " +
                     "FROM tickets WHERE incident_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, ticketRowMapper, incidentId);
    }

    public List<Ticket> findByIncidentIdAndAssignedTo(Integer incidentId, Integer assignedTo) {
        String sql = "SELECT ticket_id, ticket_number, created_by, assigned_to, category_id, incident_id, title, description, priority, status, created_at, updated_at " +
                     "FROM tickets WHERE incident_id = ? AND assigned_to = ? ORDER BY created_at DESC, ticket_id DESC";
        return jdbcTemplate.query(sql, ticketRowMapper, incidentId, assignedTo);
    }

    public List<Ticket> findByIncidentIdAndParticipant(Integer incidentId, Integer userId) {
        String sql = "SELECT DISTINCT t.ticket_id, t.ticket_number, t.created_by, t.assigned_to, t.category_id, t.incident_id, t.title, t.description, t.priority, t.status, t.created_at, t.updated_at " +
                     "FROM tickets t " +
                     "LEFT JOIN ticket_participants tp ON t.ticket_id = tp.ticket_id " +
                     "WHERE t.incident_id = ? AND (t.created_by = ? OR tp.user_id = ?) " +
                     "ORDER BY t.created_at DESC, t.ticket_id DESC";
        return jdbcTemplate.query(sql, ticketRowMapper, incidentId, userId, userId);
    }

    public List<Ticket> findByParticipant(Integer userId) {
        String sql = "SELECT DISTINCT t.ticket_id, t.ticket_number, t.created_by, t.assigned_to, t.category_id, t.incident_id, t.title, t.description, t.priority, t.status, t.created_at, t.updated_at " +
                     "FROM tickets t " +
                     "JOIN ticket_participants tp ON t.ticket_id = tp.ticket_id " +
                     "WHERE tp.user_id = ? ORDER BY t.created_at DESC, t.ticket_id DESC";
        return jdbcTemplate.query(sql, ticketRowMapper, userId);
    }

    public List<ClosedTicketResponse> findClosedTicketsLog() {
        String sql = "SELECT " +
                     "    t.ticket_number, " +
                     "    t.title, " +
                     "    cat.category_name AS category, " +
                     "    u_agent.name AS assigned_agent_name, " +
                     "    COALESCE(u_closed.name, 'Admin') AS closed_by, " +
                     "    COALESCE(comm.created_at, t.updated_at) AS closed_at, " +
                     "    COALESCE(comm.comment_text, '') AS closure_note, " +
                     "    (SELECT COUNT(*) FROM ticket_assignment_history tah " +
                     "     WHERE tah.ticket_id = t.ticket_id AND tah.from_agent IS NOT NULL) AS reassignment_count " +
                     "FROM tickets t " +
                     "JOIN categories cat ON t.category_id = cat.category_id " +
                     "LEFT JOIN users u_agent ON t.assigned_to = u_agent.user_id " +
                     "LEFT JOIN comments comm ON comm.comment_id = ( " +
                     "    SELECT c2.comment_id FROM comments c2 " +
                     "    WHERE c2.ticket_id = t.ticket_id AND c2.comment_type = 'CLOSURE' " +
                     "    ORDER BY c2.created_at DESC, c2.comment_id DESC LIMIT 1 " +
                     ") " +
                     "LEFT JOIN users u_closed ON comm.user_id = u_closed.user_id " +
                     "WHERE t.status = 'CLOSED' " +
                     "ORDER BY closed_at DESC, t.ticket_id DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp closedAtTs = rs.getTimestamp("closed_at");
            return new ClosedTicketResponse(
                    rs.getString("ticket_number"),
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getString("assigned_agent_name"),
                    rs.getString("closed_by"),
                    closedAtTs != null ? closedAtTs.toLocalDateTime() : null,
                    rs.getString("closure_note"),
                    rs.getInt("reassignment_count")
            );
        });
    }
}
