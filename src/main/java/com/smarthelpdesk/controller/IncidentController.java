package com.smarthelpdesk.controller;

import com.smarthelpdesk.config.SecurityUtils;
import com.smarthelpdesk.dto.CreateIncidentRequest;
import com.smarthelpdesk.dto.IncidentResponse;
import com.smarthelpdesk.dto.IncidentTicketResponse;
import com.smarthelpdesk.dto.IncidentTicketsResponse;
import com.smarthelpdesk.model.Incident;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.service.IncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(
            @RequestBody CreateIncidentRequest request,
            Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        Incident incident = incidentService.create(
                actorUserId,
                request.title(),
                request.description(),
                request.location(),
                request.severity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentResponse.from(incident, 0));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getById(@PathVariable Integer id) {
        Incident incident = incidentService.getById(id);
        int affected = incidentService.affectedStudents(id);
        return ResponseEntity.ok(IncidentResponse.from(incident, affected));
    }

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> listAll() {
        List<IncidentResponse> response = incidentService.listAllWithCounts();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/tickets")
    public ResponseEntity<List<IncidentTicketResponse>> getIncidentTickets(
            @PathVariable Integer id,
            Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        List<IncidentTicketResponse> tickets = incidentService.getTicketsByIncident(id, actorUserId);
        return ResponseEntity.ok(tickets);
    }
}
