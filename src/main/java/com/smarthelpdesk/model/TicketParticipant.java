package com.smarthelpdesk.model;

import java.time.LocalDateTime;

public class TicketParticipant {
    private Integer participantId;
    private Integer ticketId;
    private Integer userId;
    private LocalDateTime joinedAt;

    public TicketParticipant() {
    }

    public TicketParticipant(Integer ticketId, Integer userId) {
        this.ticketId = ticketId;
        this.userId = userId;
    }

    public Integer getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Integer participantId) {
        this.participantId = participantId;
    }

    public Integer getTicketId() {
        return ticketId;
    }

    public void setTicketId(Integer ticketId) {
        this.ticketId = ticketId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
