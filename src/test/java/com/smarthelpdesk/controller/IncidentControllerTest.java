package com.smarthelpdesk.controller;

import com.smarthelpdesk.config.CustomUserDetails;
import com.smarthelpdesk.dto.IncidentResponse;
import com.smarthelpdesk.dto.IncidentTicketResponse;
import com.smarthelpdesk.model.User;
import com.smarthelpdesk.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncidentControllerTest {

    private IncidentService incidentService;
    private IncidentController controller;

    @BeforeEach
    void setUp() {
        incidentService = mock(IncidentService.class);
        controller = new IncidentController(incidentService);
    }

    @Test
    @DisplayName("listAll returns incidents with ticketCount and openTicketCount from incidentService")
    void listAllReturnsIncidentsWithCounts() {
        IncidentResponse r1 = new IncidentResponse(
                1, "Inc 1", "Desc 1", "Loc 1", "OPEN", "HIGH",
                LocalDateTime.now(), null, 0, 4, 2
        );
        when(incidentService.listAllWithCounts()).thenReturn(List.of(r1));

        ResponseEntity<List<IncidentResponse>> response = controller.listAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(4, response.getBody().get(0).ticketCount());
        assertEquals(2, response.getBody().get(0).openTicketCount());
        verify(incidentService).listAllWithCounts();
    }

    @Test
    @DisplayName("getIncidentTickets extracts actor and returns ticket list")
    void getIncidentTicketsReturnsTickets() {
        User user = new User("Admin", "admin@test.com", "pass", "ADMIN");
        user.setUserId(1);
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        IncidentTicketResponse ticket = new IncidentTicketResponse(
                10, 10, "T-10", "Title", "Desc", "OPEN", "HIGH", "HIGH",
                "IT", "IT", "Agent", "Agent", 2, LocalDateTime.now(), LocalDateTime.now(),
                false, "HIGH", 80, 5
        );
        when(incidentService.getTicketsByIncident(5, 1)).thenReturn(List.of(ticket));

        ResponseEntity<List<IncidentTicketResponse>> response = controller.getIncidentTickets(5, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("T-10", response.getBody().get(0).ticketNumber());
        verify(incidentService).getTicketsByIncident(5, 1);
    }

    @Test
    @DisplayName("getIncidentTickets throws SecurityException when unauthenticated")
    void getIncidentTicketsUnauthenticatedThrows() {
        assertThrows(SecurityException.class, () ->
                controller.getIncidentTickets(5, null)
        );
    }
}
