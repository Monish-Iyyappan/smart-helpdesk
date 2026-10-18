package com.smarthelpdesk.priority;

import com.smarthelpdesk.dao.CategoryDAO;
import com.smarthelpdesk.dao.IncidentDAO;
import com.smarthelpdesk.dao.TicketDAO;
import com.smarthelpdesk.dao.TicketParticipantDAO;
import com.smarthelpdesk.dao.UserDAO;
import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.dto.UrgencySummaryResponse;
import com.smarthelpdesk.model.Category;
import com.smarthelpdesk.model.Incident;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UrgencyService {

    private final TicketDAO ticketDAO;
    private final TicketParticipantDAO ticketParticipantDAO;
    private final IncidentDAO incidentDAO;
    private final CategoryDAO categoryDAO;
    private final UserDAO userDAO;
    private final UrgencyCalculator calculator;

    private static final Comparator<TicketListResponse> URGENCY_COMPARATOR = Comparator
            .comparing((TicketListResponse t) -> UrgencyLevel.valueOf(t.urgencyLevel()).ordinal())
            .reversed()
            .thenComparing(TicketListResponse::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));

    public UrgencyService(TicketDAO ticketDAO,
                          TicketParticipantDAO ticketParticipantDAO,
                          IncidentDAO incidentDAO,
                          CategoryDAO categoryDAO,
                          UserDAO userDAO) {
        this.ticketDAO = ticketDAO;
        this.ticketParticipantDAO = ticketParticipantDAO;
        this.incidentDAO = incidentDAO;
        this.categoryDAO = categoryDAO;
        this.userDAO = userDAO;
        this.calculator = new UrgencyCalculator();
    }

    private User checkUser(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return userDAO.findById(userId)
                .orElseThrow(() -> new SecurityException("User not found: " + userId));
    }

    private User checkAdmin(Integer adminUserId) {
        User user = checkUser(adminUserId);
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new SecurityException("Action requires ADMIN role");
        }
        return user;
    }

    public UrgencyResult evaluate(String ticketNumber, Integer actorUserId) {
        if (ticketNumber == null || ticketNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket number cannot be blank");
        }
        User actor = checkUser(actorUserId);
        Ticket ticket = ticketDAO.findByTicketNumber(ticketNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketNumber));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(actor.getRole());
        boolean isCreator = actorUserId.equals(ticket.getCreatedBy());
        boolean isParticipant = ticketParticipantDAO.isParticipant(ticket.getTicketId(), actorUserId);
        boolean isAssignedAgent = "AGENT".equalsIgnoreCase(actor.getRole()) && actorUserId.equals(ticket.getAssignedTo());

        if (!isAdmin && !isCreator && !isParticipant && !isAssignedAgent) {
            throw new SecurityException("Access denied for ticket: " + ticketNumber);
        }

        String categoryName = categoryDAO.findById(ticket.getCategoryId())
                .map(Category::getCategoryName)
                .orElse("Other");

        int studentCount = ticketParticipantDAO.countStudentsByTicket().getOrDefault(ticket.getTicketId(), 0);

        String incidentSeverity = null;
        int incidentStudentCount = 0;
        if (ticket.getIncidentId() != null) {
            Optional<Incident> incOpt = incidentDAO.findById(ticket.getIncidentId());
            if (incOpt.isPresent()) {
                incidentSeverity = incOpt.get().getSeverity();
                incidentStudentCount = incidentDAO.countAffectedStudents(ticket.getIncidentId());
            }
        }

        UrgencyInput input = new UrgencyInput(
                categoryName,
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCreatedAt(),
                ticket.getStatus(),
                studentCount,
                incidentSeverity,
                incidentStudentCount
        );

        return calculator.calculate(input, LocalDateTime.now());
    }

    public List<TicketListResponse> evaluateAll(Integer adminUserId) {
        checkAdmin(adminUserId);
        List<Ticket> tickets = ticketDAO.findAll();
        List<TicketListResponse> responses = evaluateTicketsBatch(tickets);
        responses.sort(URGENCY_COMPARATOR);
        return responses;
    }

    public List<TicketListResponse> listOverdue(Integer adminUserId) {
        checkAdmin(adminUserId);
        List<TicketListResponse> all = evaluateAll(adminUserId);
        return all.stream()
                .filter(TicketListResponse::overdue)
                .toList();
    }

    public UrgencySummaryResponse summary(Integer adminUserId) {
        checkAdmin(adminUserId);
        return getSummary();
    }

    public UrgencySummaryResponse getSummary() {
        List<Ticket> tickets = ticketDAO.findAll();
        List<TicketListResponse> evaluated = evaluateTicketsBatch(tickets);

        int totalOverdue = (int) evaluated.stream().filter(TicketListResponse::overdue).count();

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("LOW", 0);
        counts.put("MEDIUM", 0);
        counts.put("HIGH", 0);
        counts.put("URGENT", 0);

        for (TicketListResponse t : evaluated) {
            counts.merge(t.urgencyLevel(), 1, Integer::sum);
        }

        return new UrgencySummaryResponse(totalOverdue, counts);
    }

    public List<TicketListResponse> evaluateTicketsBatch(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, String> categoryMap = categoryDAO.findAll().stream()
                .collect(Collectors.toMap(Category::getCategoryId, Category::getCategoryName, (a, b) -> a));

        Map<Integer, Integer> studentCountMap = ticketParticipantDAO.countStudentsByTicket();

        Map<Integer, Incident> incidentMap = incidentDAO.findAll().stream()
                .collect(Collectors.toMap(Incident::getIncidentId, inc -> inc, (a, b) -> a));

        Map<Integer, Integer> incidentStudentCounts = new HashMap<>();
        for (Integer incId : incidentMap.keySet()) {
            incidentStudentCounts.put(incId, incidentDAO.countAffectedStudents(incId));
        }

        LocalDateTime now = LocalDateTime.now();
        List<TicketListResponse> responses = new ArrayList<>(tickets.size());

        for (Ticket ticket : tickets) {
            String categoryName = categoryMap.getOrDefault(ticket.getCategoryId(), "Other");
            int studentCount = studentCountMap.getOrDefault(ticket.getTicketId(), 0);

            String incidentSeverity = null;
            int incStudentCount = 0;
            if (ticket.getIncidentId() != null) {
                Incident inc = incidentMap.get(ticket.getIncidentId());
                if (inc != null) {
                    incidentSeverity = inc.getSeverity();
                    incStudentCount = incidentStudentCounts.getOrDefault(ticket.getIncidentId(), 0);
                }
            }

            UrgencyInput input = new UrgencyInput(
                    categoryName,
                    ticket.getTitle(),
                    ticket.getDescription(),
                    ticket.getCreatedAt(),
                    ticket.getStatus(),
                    studentCount,
                    incidentSeverity,
                    incStudentCount
            );

            UrgencyResult res = calculator.calculate(input, now);

            responses.add(new TicketListResponse(
                    ticket.getTicketId(),
                    ticket.getTicketNumber(),
                    ticket.getTitle(),
                    ticket.getDescription(),
                    categoryName,
                    ticket.getPriority(),
                    res.level().name(),
                    res.score(),
                    res.overdue(),
                    res.dueAt(),
                    res.reasons(),
                    ticket.getStatus(),
                    ticket.getCreatedBy(),
                    ticket.getAssignedTo(),
                    ticket.getIncidentId(),
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt()
            ));
        }

        return responses;
    }
}
