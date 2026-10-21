package com.smarthelpdesk.dao;

import com.smarthelpdesk.dto.IncidentResponse;
import com.smarthelpdesk.model.Incident;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class IncidentDAO {

    private final JdbcTemplate jdbcTemplate;

    public IncidentDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Incident> incidentRowMapper = (rs, rowNum) -> {
        Incident incident = new Incident();
        incident.setIncidentId(rs.getInt("incident_id"));
        incident.setTitle(rs.getString("title"));
        incident.setDescription(rs.getString("description"));
        incident.setLocation(rs.getString("location"));
        incident.setStatus(rs.getString("status"));
        incident.setSeverity(rs.getString("severity"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            incident.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            incident.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return incident;
    };

    public Integer insert(Incident incident) {
        String sql = "INSERT INTO incidents (title, description, location, status, severity) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, incident.getTitle());
            ps.setString(2, incident.getDescription());
            ps.setString(3, incident.getLocation());
            ps.setString(4, incident.getStatus());
            ps.setString(5, incident.getSeverity());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            int generatedId = key.intValue();
            incident.setIncidentId(generatedId);
            return generatedId;
        }
        return null;
    }

    public Optional<Incident> findById(Integer id) {
        String sql = "SELECT incident_id, title, description, location, status, severity, created_at, updated_at " +
                     "FROM incidents WHERE incident_id = ?";
        List<Incident> results = jdbcTemplate.query(sql, incidentRowMapper, id);
        return results.stream().findFirst();
    }

    public List<Incident> findAll() {
        String sql = "SELECT incident_id, title, description, location, status, severity, created_at, updated_at " +
                     "FROM incidents ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, incidentRowMapper);
    }

    public List<IncidentResponse> findAllWithCounts() {
        String sql = "SELECT " +
                     "    i.incident_id, " +
                     "    i.title, " +
                     "    i.description, " +
                     "    i.location, " +
                     "    i.status, " +
                     "    i.severity, " +
                     "    i.created_at, " +
                     "    i.updated_at, " +
                     "    COUNT(t.ticket_id) AS ticket_count, " +
                     "    COUNT(CASE WHEN t.status NOT IN ('RESOLVED', 'CLOSED') THEN 1 END) AS open_ticket_count " +
                     "FROM incidents i " +
                     "LEFT JOIN tickets t ON i.incident_id = t.incident_id " +
                     "GROUP BY i.incident_id, i.title, i.description, i.location, i.status, i.severity, i.created_at, i.updated_at " +
                     "ORDER BY i.created_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Integer id = rs.getInt("incident_id");
            String title = rs.getString("title");
            String description = rs.getString("description");
            String location = rs.getString("location");
            String status = rs.getString("status");
            String severity = rs.getString("severity");
            Timestamp cat = rs.getTimestamp("created_at");
            Timestamp uat = rs.getTimestamp("updated_at");
            int ticketCount = rs.getInt("ticket_count");
            int openTicketCount = rs.getInt("open_ticket_count");

            return new IncidentResponse(
                    id,
                    title,
                    description,
                    location,
                    status,
                    severity,
                    cat != null ? cat.toLocalDateTime() : null,
                    uat != null ? uat.toLocalDateTime() : null,
                    0,
                    ticketCount,
                    openTicketCount
            );
        });
    }

    public int countAffectedStudents(Integer incidentId) {
        String sql = "SELECT COUNT(DISTINCT tp.user_id) " +
                     "FROM tickets t " +
                     "JOIN ticket_participants tp ON t.ticket_id = tp.ticket_id " +
                     "WHERE t.incident_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, incidentId);
        return count != null ? count : 0;
    }
}
