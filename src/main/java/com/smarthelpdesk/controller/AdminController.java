package com.smarthelpdesk.controller;

import com.smarthelpdesk.config.SecurityUtils;
import com.smarthelpdesk.dto.AdminStatsResponse;
import com.smarthelpdesk.dto.AssignTicketRequest;
import com.smarthelpdesk.dto.CreateAgentRequest;
import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.dto.UrgencySummaryResponse;
import com.smarthelpdesk.dto.UserResponse;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.model.User;
import com.smarthelpdesk.priority.UrgencyService;
import com.smarthelpdesk.service.AdminService;
import com.smarthelpdesk.service.IncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import com.smarthelpdesk.assignment.AssignmentRecord;
import com.smarthelpdesk.assignment.AssignmentService;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final IncidentService incidentService;
    private final UrgencyService urgencyService;
    private final AssignmentService assignmentService;

    public AdminController(AdminService adminService, IncidentService incidentService,
                           UrgencyService urgencyService, AssignmentService assignmentService) {
        this.adminService = adminService;
        this.incidentService = incidentService;
        this.urgencyService = urgencyService;
        this.assignmentService = assignmentService;
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<TicketListResponse>> viewAll(
            @RequestParam(value = "sort", required = false) String sort,
            Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        List<TicketListResponse> tickets = adminService.viewAllWithUrgency(actorUserId, sort);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/tickets/overdue")
    public ResponseEntity<List<TicketListResponse>> viewOverdue(Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        List<TicketListResponse> tickets = urgencyService.listOverdue(actorUserId);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/urgency/summary")
    public ResponseEntity<UrgencySummaryResponse> getUrgencySummary(Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        UrgencySummaryResponse summary = urgencyService.summary(actorUserId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/tickets/unassigned")
    public ResponseEntity<List<TicketListResponse>> viewUnassigned(Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        List<TicketListResponse> tickets = adminService.viewUnassignedWithUrgency(actorUserId);
        return ResponseEntity.ok(tickets);
    }

    @RequestMapping(value = "/tickets/{ticketId}/assign", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Ticket> assign(
            @PathVariable Integer ticketId,
            @RequestBody AssignTicketRequest request,
            Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        Ticket ticket = assignmentService.assignTicket(ticketId, actorUserId, request.agentId(), request.reason());
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/tickets/{ticketId}/assignments")
    public ResponseEntity<List<AssignmentRecord>> getAssignmentHistory(
            @PathVariable Integer ticketId,
            Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        List<AssignmentRecord> history = assignmentService.getAssignmentHistory(ticketId, actorUserId);
        return ResponseEntity.ok(history);
    }

    @PutMapping("/tickets/{ticketId}/close")
    public ResponseEntity<Ticket> close(@PathVariable Integer ticketId, Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        Ticket ticket = adminService.close(ticketId, actorUserId);
        return ResponseEntity.ok(ticket);
    }

    @PutMapping("/tickets/{ticketId}/reopen")
    public ResponseEntity<Ticket> reopen(@PathVariable Integer ticketId, Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        Ticket ticket = adminService.reopen(ticketId, actorUserId);
        return ResponseEntity.ok(ticket);
    }

    @PutMapping("/incidents/{incidentId}/tickets/{ticketId}")
    public ResponseEntity<Map<String, Object>> linkTicket(
            @PathVariable Integer incidentId,
            @PathVariable Integer ticketId,
            Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        incidentService.linkTicket(actorUserId, ticketId, incidentId);
        return ResponseEntity.ok(Map.of(
                "message", "Ticket linked to incident successfully",
                "ticketId", ticketId,
                "incidentId", incidentId
        ));
    }

    @GetMapping("/agents")
    public ResponseEntity<List<UserResponse>> listAgents(Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        List<User> agents = adminService.listAgents(actorUserId);
        List<UserResponse> response = agents.stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/agents")
    public ResponseEntity<UserResponse> createAgent(
            @RequestBody CreateAgentRequest request,
            Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        User agent = adminService.createAgent(actorUserId, request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(agent));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats(Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        AdminStatsResponse stats = adminService.getStats(actorUserId);
        return ResponseEntity.ok(stats);
    }
}
