package com.smarthelpdesk.dto;

public record AssignTicketRequest(Integer agentId, String reason) {
    public AssignTicketRequest(Integer agentId) {
        this(agentId, null);
    }
}
