package com.smarthelpdesk.model;

import java.time.LocalDateTime;

public class Comment {
    private Integer commentId;
    private Integer ticketId;
    private Integer userId;
    private String commentText;
    private String commentType = "GENERAL";
    private LocalDateTime createdAt;

    public Comment() {
    }

    public Comment(Integer ticketId, Integer userId, String commentText) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.commentText = commentText;
        this.commentType = "GENERAL";
    }

    public Comment(Integer ticketId, Integer userId, String commentText, String commentType) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.commentText = commentText;
        this.commentType = commentType != null ? commentType : "GENERAL";
    }

    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
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

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public String getCommentType() {
        return commentType;
    }

    public void setCommentType(String commentType) {
        this.commentType = commentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
