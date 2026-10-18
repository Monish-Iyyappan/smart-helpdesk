package com.smarthelpdesk.dao;

import com.smarthelpdesk.model.TicketParticipant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TicketParticipantDAO {

    private final JdbcTemplate jdbcTemplate;

    public TicketParticipantDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<TicketParticipant> participantRowMapper = (rs, rowNum) -> {
        TicketParticipant participant = new TicketParticipant();
        participant.setParticipantId(rs.getInt("participant_id"));
        participant.setTicketId(rs.getInt("ticket_id"));
        participant.setUserId(rs.getInt("user_id"));
        Timestamp joinedAt = rs.getTimestamp("joined_at");
        if (joinedAt != null) {
            participant.setJoinedAt(joinedAt.toLocalDateTime());
        }
        return participant;
    };

    public boolean add(Integer ticketId, Integer userId) {
        if (isParticipant(ticketId, userId)) {
            return false;
        }
        try {
            String sql = "INSERT INTO ticket_participants (ticket_id, user_id) VALUES (?, ?)";
            int rows = jdbcTemplate.update(sql, ticketId, userId);
            return rows > 0;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public List<TicketParticipant> findByTicketId(Integer ticketId) {
        String sql = "SELECT participant_id, ticket_id, user_id, joined_at " +
                     "FROM ticket_participants WHERE ticket_id = ? ORDER BY joined_at ASC";
        return jdbcTemplate.query(sql, participantRowMapper, ticketId);
    }

    public boolean isParticipant(Integer ticketId, Integer userId) {
        String sql = "SELECT COUNT(*) FROM ticket_participants WHERE ticket_id = ? AND user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, ticketId, userId);
        return count != null && count > 0;
    }

    public Map<Integer, Integer> countStudentsByTicket() {
        String sql = "SELECT tp.ticket_id, COUNT(DISTINCT tp.user_id) AS student_count " +
                     "FROM ticket_participants tp " +
                     "JOIN users u ON tp.user_id = u.user_id " +
                     "WHERE u.role = 'STUDENT' " +
                     "GROUP BY tp.ticket_id";
        Map<Integer, Integer> counts = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            counts.put(rs.getInt("ticket_id"), rs.getInt("student_count"));
        });
        return counts;
    }
}
