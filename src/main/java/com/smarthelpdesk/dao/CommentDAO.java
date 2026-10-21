package com.smarthelpdesk.dao;

import com.smarthelpdesk.model.Comment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class CommentDAO {

    private final JdbcTemplate jdbcTemplate;

    public CommentDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Comment> commentRowMapper = (rs, rowNum) -> {
        Comment comment = new Comment();
        comment.setCommentId(rs.getInt("comment_id"));
        comment.setTicketId(rs.getInt("ticket_id"));
        comment.setUserId(rs.getInt("user_id"));
        comment.setCommentText(rs.getString("comment_text"));
        comment.setCommentType(rs.getString("comment_type"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            comment.setCreatedAt(createdAt.toLocalDateTime());
        }
        return comment;
    };

    public Integer insert(Comment comment) {
        String sql = "INSERT INTO comments (ticket_id, user_id, comment_text, comment_type) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, comment.getTicketId());
            ps.setInt(2, comment.getUserId());
            ps.setString(3, comment.getCommentText());
            ps.setString(4, comment.getCommentType() != null ? comment.getCommentType() : "GENERAL");
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            int generatedId = key.intValue();
            comment.setCommentId(generatedId);
            return generatedId;
        }
        return null;
    }

    public List<Comment> findByTicketId(Integer ticketId) {
        String sql = "SELECT comment_id, ticket_id, user_id, comment_text, comment_type, created_at " +
                     "FROM comments WHERE ticket_id = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, commentRowMapper, ticketId);
    }
}
