package com.smarthelpdesk.service;

import com.smarthelpdesk.dao.DashboardDAO;
import com.smarthelpdesk.priority.UrgencyService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessResourceFailureException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {

    @Test
    void testAccessDeniedWithEmptyPassword() {
        DashboardDAO dao = mock(DashboardDAO.class);
        UrgencyService urgencyService = mock(UrgencyService.class);
        DashboardService service = new DashboardService(dao, urgencyService, "");

        SQLException sqlEx = new SQLException("Access denied for user 'root'@'localhost' (using password: NO)", "28000", 1045);
        DataAccessResourceFailureException ex = new DataAccessResourceFailureException("Failed", sqlEx);

        String hint = service.classifyFailure(ex);
        assertEquals("DB_PASSWORD is not set in this terminal", hint);

        when(dao.getDatabaseName()).thenThrow(ex);
        String dashboard = service.buildDashboard();
        assertTrue(dashboard.contains("Hint: DB_PASSWORD is not set in this terminal"));
        assertTrue(dashboard.contains("DATABASE STATUS : NOT CONNECTED"));

        for (String line : dashboard.split("\n")) {
            assertTrue(line.length() <= 52, "Line exceeds 52 characters: " + line);
        }
    }

    @Test
    void testAccessDeniedWithProvidedPassword() {
        DashboardDAO dao = mock(DashboardDAO.class);
        UrgencyService urgencyService = mock(UrgencyService.class);
        DashboardService service = new DashboardService(dao, urgencyService, "secretPass");

        SQLException sqlEx = new SQLException("Access denied for user 'root'@'localhost' (using password: YES)", "28000", 1045);
        DataAccessResourceFailureException ex = new DataAccessResourceFailureException("Failed", sqlEx);

        String hint = service.classifyFailure(ex);
        assertEquals("Wrong MySQL username or password", hint);

        when(dao.getDatabaseName()).thenThrow(ex);
        String dashboard = service.buildDashboard();
        assertTrue(dashboard.contains("Hint: Wrong MySQL username or password"));
    }

    @Test
    void testUnknownDatabase() {
        DashboardDAO dao = mock(DashboardDAO.class);
        UrgencyService urgencyService = mock(UrgencyService.class);
        DashboardService service = new DashboardService(dao, urgencyService, "secretPass");

        SQLException sqlEx = new SQLException("Unknown database 'smart_helpdesk'", "42000", 1049);
        DataAccessResourceFailureException ex = new DataAccessResourceFailureException("Failed", sqlEx);

        String hint = service.classifyFailure(ex);
        assertEquals("Database not found; create it from schema.sql", hint);

        when(dao.getDatabaseName()).thenThrow(ex);
        String dashboard = service.buildDashboard();
        assertTrue(dashboard.contains("Hint: Database not found; create it from schema.sql"));
    }

    @Test
    void testCommunicationsLinkFailure() {
        DashboardDAO dao = mock(DashboardDAO.class);
        UrgencyService urgencyService = mock(UrgencyService.class);
        DashboardService service = new DashboardService(dao, urgencyService, "secretPass");

        SQLException sqlEx = new SQLException("Communications link failure. Connection refused: connect", "08S01", 0);
        DataAccessResourceFailureException ex = new DataAccessResourceFailureException("Failed", sqlEx);

        String hint = service.classifyFailure(ex);
        assertEquals("MySQL is not running or port 3306 is wrong", hint);

        when(dao.getDatabaseName()).thenThrow(ex);
        String dashboard = service.buildDashboard();
        assertTrue(dashboard.contains("Hint: MySQL is not running or port 3306 is wrong"));
    }
}
