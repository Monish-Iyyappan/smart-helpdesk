package com.smarthelpdesk.config;

import com.smarthelpdesk.service.DashboardService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Startup runner that executes once when the Spring Boot application starts.
 * Prints the ASCII dashboard to the terminal and ensures it never throws an exception.
 */
@Component
@Order(1)
public class DashboardRunner implements ApplicationRunner {

    private final DashboardService dashboardService;

    // Inject DashboardService via constructor injection
    public DashboardRunner(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Build and display the startup dashboard
            String dashboard = dashboardService.buildDashboard();
            System.out.println(dashboard);

            // Display completion status based on database connectivity
            if (dashboardService.isDatabaseReachable()) {
                System.out.println("Application started successfully!");
            } else {
                System.out.println("Application started, but the database is unavailable.");
            }
        } catch (Exception e) {
            // Safety guard: The runner must never throw or prevent application startup
            System.out.println("Application started, but the database is unavailable.");
        }
    }
}
