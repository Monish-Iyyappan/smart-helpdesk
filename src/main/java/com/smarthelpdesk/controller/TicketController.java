package com.smarthelpdesk.controller;

import com.smarthelpdesk.config.SecurityUtils;
import com.smarthelpdesk.dto.CommentRequest;
import com.smarthelpdesk.dto.CreateTicketRequest;
import com.smarthelpdesk.dto.TicketDetailResponse;
import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.dto.UpdateStatusRequest;
import com.smarthelpdesk.model.Comment;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<Ticket> create(@RequestBody CreateTicketRequest request, Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        Ticket ticket = ticketService.create(
                actorUserId,
                request.categoryId(),
                request.title(),
                request.description(),
                request.priority()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @GetMapping("/{ticketNumber}")
    public ResponseEntity<TicketDetailResponse> getByTicketNumber(@PathVariable String ticketNumber, Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        TicketDetailResponse ticket = ticketService.getTicketDetail(ticketNumber, actorUserId);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<TicketListResponse>> mine(Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        List<TicketListResponse> tickets = ticketService.myTicketResponses(actorUserId);
        return ResponseEntity.ok(tickets);
    }

    @PostMapping("/{ticketNumber}/join")
    public ResponseEntity<Map<String, Object>> join(@PathVariable String ticketNumber, Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        boolean joined = ticketService.join(ticketNumber, actorUserId);
        return ResponseEntity.ok(Map.of("joined", joined, "ticketNumber", ticketNumber));
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable Integer ticketId,
            @RequestBody CommentRequest request,
            Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        Comment comment = ticketService.addComment(ticketId, actorUserId, request.commentText(), request.commentType());
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @GetMapping("/{ticketId}/comments")
    public ResponseEntity<List<Comment>> getComments(@PathVariable Integer ticketId, Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        List<Comment> comments = ticketService.getComments(ticketId, actorUserId);
        return ResponseEntity.ok(comments);
    }

    @PutMapping("/{ticketId}/status")
    public ResponseEntity<Ticket> updateStatus(
            @PathVariable Integer ticketId,
            @RequestBody UpdateStatusRequest request,
            Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        Ticket ticket = ticketService.updateStatus(ticketId, actorUserId, request.status());
        return ResponseEntity.ok(ticket);
    }
}
