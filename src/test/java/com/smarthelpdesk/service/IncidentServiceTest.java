package com.smarthelpdesk.service;

import com.smarthelpdesk.dao.IncidentDAO;
import com.smarthelpdesk.dao.TicketDAO;
import com.smarthelpdesk.dao.UserDAO;
import com.smarthelpdesk.dto.IncidentResponse;
import com.smarthelpdesk.dto.IncidentTicketResponse;
import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.exception.ResourceNotFoundException;
import com.smarthelpdesk.model.Incident;
import com.smarthelpdesk.model.Ticket;
import com.smarthelpdesk.model.User;
import com.smarthelpdesk.priority.UrgencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncidentServiceTest {

    private IncidentDAO incidentDAO;
    private TicketDAO ticketDAO;
    private UserDAO userDAO;
    private UrgencyService urgencyService;
    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentDAO = mock(IncidentDAO.class);
        ticketDAO = mock(TicketDAO.class);
        userDAO = mock(UserDAO.class);
        urgencyService = mock(UrgencyService.class);
        incidentService = new IncidentService(incidentDAO, ticketDAO, userDAO, urgencyService);
    }

    @Test
    @DisplayName("listAllWithCounts returns 0 counts for incident with 0 tickets")
    void listAllWithCountsZeroTickets() {
        IncidentResponse resp = new IncidentResponse(
                1, "Wi-Fi Outage", "Library Wi-Fi down", "Library", "OPEN", "HIGH",
                LocalDateTime.now(), null, 0, 0, 0
        );
        when(incidentDAO.findAllWithCounts()).thenReturn(List.of(resp));

        List<IncidentResponse> list = incidentService.listAllWithCounts();

        assertEquals(1, list.size());
        assertEquals(0, list.get(0).ticketCount());
        assertEquals(0, list.get(0).openTicketCount());
    }

    @Test
    @DisplayName("listAllWithCounts returns correct counts for incident with 1 ticket")
    void listAllWithCountsOneTicket() {
        IncidentResponse resp = new IncidentResponse(
                2, "Power Cut", "Power loss in Lab 3", "Lab 3", "OPEN", "CRITICAL",
                LocalDateTime.now(), null, 5, 1, 1
        );
        when(incidentDAO.findAllWithCounts()).thenReturn(List.of(resp));

        List<IncidentResponse> list = incidentService.listAllWithCounts();

        assertEquals(1, list.size());
        assertEquals(1, list.get(0).ticketCount());
        assertEquals(1, list.get(0).openTicketCount());
    }

    @Test
    @DisplayName("listAllWithCounts returns correct counts for incident with many tickets")
    void listAllWithCountsManyTickets() {
        IncidentResponse resp = new IncidentResponse(
                3, "AC Malfunction", "Main auditorium AC not cooling", "Auditorium", "OPEN", "MEDIUM",
                LocalDateTime.now(), null, 12, 5, 3
        );
        when(incidentDAO.findAllWithCounts()).thenReturn(List.of(resp));

        List<IncidentResponse> list = incidentService.listAllWithCounts();

        assertEquals(1, list.size());
        assertEquals(5, list.get(0).ticketCount());
        assertEquals(3, list.get(0).openTicketCount());
    }

    @Test
    @DisplayName("getTicketsByIncident throws ResourceNotFoundException for unknown incident ID")
    void getTicketsByIncidentUnknownIncidentThrows404() {
        when(incidentDAO.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                incidentService.getTicketsByIncident(999, 1)
        );
    }

    @Test
    @DisplayName("getTicketsByIncident throws SecurityException for unknown actor user")
    void getTicketsByIncidentUnknownUserThrowsSecurityException() {
        Incident inc = new Incident("Title", "Desc", "Loc", "OPEN", "HIGH");
        inc.setIncidentId(10);
        when(incidentDAO.findById(10)).thenReturn(Optional.of(inc));
        when(userDAO.findById(999)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () ->
                incidentService.getTicketsByIncident(10, 999)
        );
    }

    @Test
    @DisplayName("getTicketsByIncident for ADMIN returns all incident tickets")
    void getTicketsByIncidentAdminReturnsAllTickets() {
        Incident inc = new Incident("Title", "Desc", "Loc", "OPEN", "HIGH");
        inc.setIncidentId(10);
        when(incidentDAO.findById(10)).thenReturn(Optional.of(inc));

        User admin = new User("Admin", "admin@test.com", "hash", "ADMIN");
        admin.setUserId(1);
        when(userDAO.findById(1)).thenReturn(Optional.of(admin));

        Ticket t1 = new Ticket("T-100", 1, 1, "Ticket 1", "Desc 1", "HIGH", "OPEN");
        t1.setTicketId(101);
        t1.setIncidentId(10);
        Ticket t2 = new Ticket("T-102", 2, 1, "Ticket 2", "Desc 2", "LOW", "IN_PROGRESS");
        t2.setTicketId(102);
        t2.setIncidentId(10);

        when(ticketDAO.findByIncidentId(10)).thenReturn(List.of(t1, t2));

        TicketListResponse ev1 = new TicketListResponse(
                101, "T-100", "Ticket 1", "Desc 1", "IT", "HIGH", "HIGH",
                10, false, null, List.of(), "OPEN", 1, null, 10, LocalDateTime.now(), null
        );
        TicketListResponse ev2 = new TicketListResponse(
                102, "T-102", "Ticket 2", "Desc 2", "IT", "LOW", "LOW",
                5, false, null, List.of(), "IN_PROGRESS", 2, null, 10, LocalDateTime.now(), null
        );
        when(urgencyService.evaluateTicketsBatch(anyList())).thenReturn(List.of(ev1, ev2));
        when(userDAO.findAll()).thenReturn(List.of(admin));

        List<IncidentTicketResponse> result = incidentService.getTicketsByIncident(10, 1);

        assertEquals(2, result.size());
        assertEquals("T-100", result.get(0).ticketNumber());
        assertEquals("T-102", result.get(1).ticketNumber());

        verify(ticketDAO).findByIncidentId(10);
        verify(ticketDAO, never()).findByIncidentIdAndAssignedTo(10, 1);
        verify(ticketDAO, never()).findByIncidentIdAndParticipant(10, 1);
    }

    @Test
    @DisplayName("getTicketsByIncident for AGENT returns only tickets assigned to that agent")
    void getTicketsByIncidentAgentReturnsOnlyAssignedTickets() {
        Incident inc = new Incident("Title", "Desc", "Loc", "OPEN", "HIGH");
        inc.setIncidentId(10);
        when(incidentDAO.findById(10)).thenReturn(Optional.of(inc));

        User agent = new User("Agent Smith", "agent@test.com", "hash", "AGENT");
        agent.setUserId(2);
        when(userDAO.findById(2)).thenReturn(Optional.of(agent));

        Ticket t1 = new Ticket("T-101", 1, 1, "Assigned Ticket", "Desc", "HIGH", "ASSIGNED");
        t1.setTicketId(201);
        t1.setAssignedTo(2);
        t1.setIncidentId(10);

        when(ticketDAO.findByIncidentIdAndAssignedTo(10, 2)).thenReturn(List.of(t1));

        TicketListResponse ev1 = new TicketListResponse(
                201, "T-101", "Assigned Ticket", "Desc", "IT", "HIGH", "HIGH",
                10, false, null, List.of(), "ASSIGNED", 1, 2, 10, LocalDateTime.now(), null
        );
        when(urgencyService.evaluateTicketsBatch(anyList())).thenReturn(List.of(ev1));
        when(userDAO.findAll()).thenReturn(List.of(agent));

        List<IncidentTicketResponse> result = incidentService.getTicketsByIncident(10, 2);

        assertEquals(1, result.size());
        assertEquals("T-101", result.get(0).ticketNumber());
        assertEquals(2, result.get(0).assignedTo());
        assertEquals("Agent Smith", result.get(0).assignedAgentName());

        verify(ticketDAO).findByIncidentIdAndAssignedTo(10, 2);
        verify(ticketDAO, never()).findByIncidentId(10);
        verify(ticketDAO, never()).findByIncidentIdAndParticipant(10, 2);
    }

    @Test
    @DisplayName("getTicketsByIncident for STUDENT returns only tickets where student is creator or participant")
    void getTicketsByIncidentStudentReturnsOnlyOwnTickets() {
        Incident inc = new Incident("Title", "Desc", "Loc", "OPEN", "HIGH");
        inc.setIncidentId(10);
        when(incidentDAO.findById(10)).thenReturn(Optional.of(inc));

        User student = new User("Alice Student", "student@test.com", "hash", "STUDENT");
        student.setUserId(3);
        when(userDAO.findById(3)).thenReturn(Optional.of(student));

        Ticket t1 = new Ticket("T-105", 3, 1, "Alice Ticket", "Desc", "MEDIUM", "OPEN");
        t1.setTicketId(301);
        t1.setIncidentId(10);

        when(ticketDAO.findByIncidentIdAndParticipant(10, 3)).thenReturn(List.of(t1));

        TicketListResponse ev1 = new TicketListResponse(
                301, "T-105", "Alice Ticket", "Desc", "IT", "MEDIUM", "MEDIUM",
                5, false, null, List.of(), "OPEN", 3, null, 10, LocalDateTime.now(), null
        );
        when(urgencyService.evaluateTicketsBatch(anyList())).thenReturn(List.of(ev1));
        when(userDAO.findAll()).thenReturn(List.of(student));

        List<IncidentTicketResponse> result = incidentService.getTicketsByIncident(10, 3);

        assertEquals(1, result.size());
        assertEquals("T-105", result.get(0).ticketNumber());

        verify(ticketDAO).findByIncidentIdAndParticipant(10, 3);
        verify(ticketDAO, never()).findByIncidentId(10);
        verify(ticketDAO, never()).findByIncidentIdAndAssignedTo(10, 3);
    }

    @Test
    @DisplayName("getTicketsByIncident throws SecurityException for unknown/unsupported role")
    void getTicketsByIncidentUnsupportedRoleThrowsSecurityException() {
        Incident inc = new Incident("Title", "Desc", "Loc", "OPEN", "HIGH");
        inc.setIncidentId(10);
        when(incidentDAO.findById(10)).thenReturn(Optional.of(inc));

        User guest = new User("Guest", "guest@test.com", "hash", "GUEST");
        guest.setUserId(4);
        when(userDAO.findById(4)).thenReturn(Optional.of(guest));

        assertThrows(SecurityException.class, () ->
                incidentService.getTicketsByIncident(10, 4)
        );
    }
}
