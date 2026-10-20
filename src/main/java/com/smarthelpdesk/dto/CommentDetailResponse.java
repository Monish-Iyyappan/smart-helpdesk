package com.smarthelpdesk.dto;

import java.time.LocalDateTime;

/**
 * DTO for comment details including the author name and comment type.
 */
public record CommentDetailResponse(
        Integer commentId,
        Integer ticketId,
        Integer userId,
        String authorName,
        String commentText,
        String commentType,
        LocalDateTime createdAt
) {
    public CommentDetailResponse(
            Integer commentId,
            Integer ticketId,
            Integer userId,
            String authorName,
            String commentText,
            LocalDateTime createdAt
    ) {
        this(commentId, ticketId, userId, authorName, commentText, "GENERAL", createdAt);
    }
}
