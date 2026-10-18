package com.smarthelpdesk.ui;

import com.smarthelpdesk.model.User;
import com.smarthelpdesk.service.AuthService;
import com.smarthelpdesk.service.DashboardService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Main console application entry point.
 * Runs only when property app.console.enabled=true is set.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.console.enabled", havingValue = "true")
public class ConsoleApp implements ApplicationRunner {

    private final AuthService authService;
    private final DashboardService dashboardService;
    private final StudentMenu studentMenu;
    private final AgentMenu agentMenu;
    private final AdminMenu adminMenu;
    private final ConsoleIO io;
    private final ApplicationContext context;

    public ConsoleApp(AuthService authService,
                      DashboardService dashboardService,
                      StudentMenu studentMenu,
                      AgentMenu agentMenu,
                      AdminMenu adminMenu,
                      ConsoleIO io,
                      ApplicationContext context) {
        this.authService = authService;
        this.dashboardService = dashboardService;
        this.studentMenu = studentMenu;
        this.agentMenu = agentMenu;
        this.adminMenu = adminMenu;
        this.io = io;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Display dashboard once at startup if available
        try {
            if (dashboardService != null) {
                System.out.println(dashboardService.buildDashboard());
            }
        } catch (Exception ignored) {
        }

        Session session = new Session();
        boolean running = true;

        while (running) {
            try {
                io.printHeader("SMART HELPDESK SYSTEM");
                System.out.println("1. Login");
                System.out.println("2. Register");
                System.out.println("3. Exit");
                io.printDivider();

                int choice = io.readInt("Select an option (1-3): ", 1, 3);
                switch (choice) {
                    case 1 -> login(session);
                    case 2 -> register();
                    case 3 -> {
                        System.out.println("Exiting Smart Helpdesk. Goodbye!");
                        running = false;
                        SpringApplication.exit(context, () -> 0);
                        return;
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
                System.out.println("Error: " + ex.getMessage());
            } catch (DataAccessException ex) {
                String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
                System.out.println("Error: Database error - " + (msg != null ? msg : "unable to perform operation"));
            }
        }
    }

    private void login(Session session) {
        String email = io.readLine("Enter email: ");
        String password = io.readPassword("Enter password: ");

        Optional<User> userOpt = authService.login(email, password);
        if (userOpt.isEmpty()) {
            System.out.println("Invalid email or password");
            return;
        }

        User user = userOpt.get();
        session.setCurrentUser(user);
        System.out.println("Welcome, " + user.getName() + " (" + user.getRole() + ")!");

        String role = user.getRole();
        if ("STUDENT".equalsIgnoreCase(role)) {
            studentMenu.show(session);
        } else if ("AGENT".equalsIgnoreCase(role)) {
            agentMenu.show(session);
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            adminMenu.show(session);
        } else {
            System.out.println("Error: Unknown role: " + role);
        }

        session.clear();
    }

    private void register() {
        String name = io.readLine("Enter full name: ");
        String email = io.readLine("Enter email address: ");
        String password = io.readPassword("Enter password (min 8 chars): ");
        String confirmPassword = io.readPassword("Confirm password: ");

        if (!password.equals(confirmPassword)) {
            System.out.println("Error: Passwords do not match.");
            return;
        }

        authService.register(name, email, password);
        System.out.println("Registration successful! You can now log in.");
    }
}
