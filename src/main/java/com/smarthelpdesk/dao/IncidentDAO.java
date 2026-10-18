package com.smarthelpdesk.dao;

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

    public int countAffectedStudents(Integer incidentId) {
        String sql = "SELECT COUNT(DISTINCT tp.user_id) " +
                     "FROM tickets t " +
                     "JOIN ticket_participants tp ON t.ticket_id = tp.ticket_id " +
                     "WHERE t.incident_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, incidentId);
        return count != null ? count : 0;
    }
}
