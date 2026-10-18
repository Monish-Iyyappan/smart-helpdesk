package com.smarthelpdesk.dao;

import com.smarthelpdesk.model.User;
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
public class UserDAO {

    private final JdbcTemplate jdbcTemplate;

    public UserDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(rs.getString("role"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    };

    public Optional<User> findById(Integer id) {
        String sql = "SELECT user_id, name, email, password_hash, role, created_at FROM users WHERE user_id = ?";
        List<User> results = jdbcTemplate.query(sql, userRowMapper, id);
        return results.stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT user_id, name, email, password_hash, role, created_at FROM users WHERE email = ?";
        List<User> results = jdbcTemplate.query(sql, userRowMapper, email);
        return results.stream().findFirst();
    }

    public List<User> findByRole(String role) {
        String sql = "SELECT user_id, name, email, password_hash, role, created_at FROM users WHERE role = ? ORDER BY user_id";
        return jdbcTemplate.query(sql, userRowMapper, role);
    }

    public Integer insert(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, role) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            int generatedId = key.intValue();
            user.setUserId(generatedId);
            return generatedId;
        }
        return null;
    }
}
