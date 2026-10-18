package com.smarthelpdesk.dto;

import com.smarthelpdesk.model.Category;

/**
 * DTO for category details.
 */
public record CategoryResponse(
        Integer categoryId,
        String categoryName
) {
    public static CategoryResponse from(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(category.getCategoryId(), category.getCategoryName());
    }
}
