package com.smarthelpdesk.dto;

public record CreateTicketRequest(Integer categoryId, String title, String description, String priority) {}
