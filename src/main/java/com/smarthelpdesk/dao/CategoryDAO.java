package com.smarthelpdesk.dao;

import com.smarthelpdesk.model.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryDAO {

    private final JdbcTemplate jdbcTemplate;

    public CategoryDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Category> categoryRowMapper = (rs, rowNum) -> {
        Category category = new Category();
        category.setCategoryId(rs.getInt("category_id"));
        category.setCategoryName(rs.getString("category_name"));
        return category;
    };

    public List<Category> findAll() {
        String sql = "SELECT category_id, category_name FROM categories ORDER BY category_id";
        return jdbcTemplate.query(sql, categoryRowMapper);
    }

    public Optional<Category> findById(Integer id) {
        String sql = "SELECT category_id, category_name FROM categories WHERE category_id = ?";
        List<Category> results = jdbcTemplate.query(sql, categoryRowMapper, id);
        return results.stream().findFirst();
    }
}
