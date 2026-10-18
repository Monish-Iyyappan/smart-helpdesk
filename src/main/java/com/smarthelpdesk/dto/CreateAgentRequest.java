package com.smarthelpdesk.dto;

/**
 * DTO for creating a new support agent account.
 */
public record CreateAgentRequest(
        String name,
        String email,
        String password
) {
}
