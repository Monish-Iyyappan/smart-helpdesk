package com.smarthelpdesk.terminal;

import com.smarthelpdesk.dto.UserResponse;

/**
 * Holds authenticated user session state and credentials.
 */
public record TerminalSession(UserResponse user, String authHeader) {
    public String getRole() {
        return user != null ? user.role() : null;
    }

    public String getName() {
        return user != null ? user.name() : "User";
    }
}
