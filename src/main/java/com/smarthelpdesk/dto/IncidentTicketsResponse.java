package com.smarthelpdesk.dto;

import com.smarthelpdesk.model.Incident;
import com.smarthelpdesk.model.Ticket;
import java.util.List;

public record IncidentTicketsResponse(Incident incident, List<Ticket> tickets, int affectedStudentCount) {}
