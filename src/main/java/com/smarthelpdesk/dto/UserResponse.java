package com.smarthelpdesk.dto;

import com.smarthelpdesk.model.User;
import java.time.LocalDateTime;

public record UserResponse(
    Integer userId,
    String name,
    String email,
    String role,
    LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
            user.getUserId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getCreatedAt()
        );
    }
}
