package com.smarthelpdesk.assignment;

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

@Repository
public class AssignmentHistoryDao {

    private final JdbcTemplate jdbcTemplate;

    public AssignmentHistoryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AssignmentRecord> rowMapper = (rs, rowNum) -> {
        int historyId = rs.getInt("history_id");
        int ticketId = rs.getInt("ticket_id");

        int fromAgentIdVal = rs.getInt("from_agent");
        Integer fromAgentId = rs.wasNull() ? null : fromAgentIdVal;
        String fromAgentName = rs.getString("from_agent_name");

        int toAgentId = rs.getInt("to_agent");
        String toAgentName = rs.getString("to_agent_name");

        int changedById = rs.getInt("changed_by");
        String changedByName = rs.getString("changed_by_name");

        String reason = rs.getString("reason");
        Timestamp changedAtTs = rs.getTimestamp("changed_at");

        AssignmentType type = fromAgentId == null ? AssignmentType.ASSIGN : AssignmentType.REASSIGN;

        return new AssignmentRecord(
                historyId,
                ticketId,
                fromAgentId,
                fromAgentName,
                toAgentId,
                toAgentName,
                changedById,
                changedByName,
                type,
                reason,
                changedAtTs != null ? changedAtTs.toLocalDateTime() : null
        );
    };

    public Integer insert(Integer ticketId, Integer fromAgent, Integer toAgent, Integer changedBy, String reason) {
        String sql = "INSERT INTO ticket_assignment_history (ticket_id, from_agent, to_agent, changed_by, reason) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, ticketId);
            if (fromAgent != null) {
                ps.setInt(2, fromAgent);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setInt(3, toAgent);
            ps.setInt(4, changedBy);
            if (reason != null) {
                ps.setString(5, reason);
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key != null ? key.intValue() : null;
    }

    public List<AssignmentRecord> findByTicketId(Integer ticketId) {
        String sql = "SELECT tah.history_id, tah.ticket_id, tah.from_agent, u_from.name AS from_agent_name, " +
                     "tah.to_agent, u_to.name AS to_agent_name, " +
                     "tah.changed_by, u_by.name AS changed_by_name, " +
                     "tah.reason, tah.changed_at " +
                     "FROM ticket_assignment_history tah " +
                     "LEFT JOIN users u_from ON tah.from_agent = u_from.user_id " +
                     "JOIN users u_to ON tah.to_agent = u_to.user_id " +
                     "JOIN users u_by ON tah.changed_by = u_by.user_id " +
                     "WHERE tah.ticket_id = ? " +
                     "ORDER BY tah.changed_at ASC, tah.history_id ASC";
        return jdbcTemplate.query(sql, rowMapper, ticketId);
    }
}
