package com.smarthelpdesk.dto;

public record CreateIncidentRequest(String title, String description, String location, String severity) {}
