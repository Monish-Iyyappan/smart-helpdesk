package com.smarthelpdesk.dto;

public record CommentRequest(String commentText, String commentType) {
    public CommentRequest(String commentText) {
        this(commentText, "GENERAL");
    }
}
