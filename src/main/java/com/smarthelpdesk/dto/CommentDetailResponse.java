package com.smarthelpdesk.dto;

import java.time.LocalDateTime;

/**
 * DTO for comment details including the author name.
 */
public record CommentDetailResponse(
        Integer commentId,
        Integer ticketId,
        Integer userId,
        String authorName,
        String commentText,
        LocalDateTime createdAt
) {
}
