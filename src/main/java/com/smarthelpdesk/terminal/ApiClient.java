package com.smarthelpdesk.terminal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthelpdesk.assignment.AssignmentRecord;
import com.smarthelpdesk.dto.AdminStatsResponse;
import com.smarthelpdesk.dto.AssignTicketRequest;
import com.smarthelpdesk.dto.CategoryResponse;
import com.smarthelpdesk.dto.CloseTicketRequest;
import com.smarthelpdesk.dto.ClosedTicketResponse;
import com.smarthelpdesk.dto.CommentRequest;
import com.smarthelpdesk.dto.CreateAgentRequest;
import com.smarthelpdesk.dto.CreateIncidentRequest;
import com.smarthelpdesk.dto.CreateTicketRequest;
import com.smarthelpdesk.dto.ErrorResponse;
import com.smarthelpdesk.dto.IncidentResponse;
import com.smarthelpdesk.dto.IncidentTicketResponse;
import com.smarthelpdesk.dto.RegisterRequest;
import com.smarthelpdesk.dto.ReopenTicketRequest;
import com.smarthelpdesk.dto.TicketDetailResponse;
import com.smarthelpdesk.dto.TicketListResponse;
import com.smarthelpdesk.dto.UpdateStatusRequest;
import com.smarthelpdesk.dto.UserResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpConnectTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * REST API client for the Terminal UI using standard java.net.http.HttpClient.
 */
@Component
public class ApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public ApiClient(ObjectMapper objectMapper, Environment environment) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .build();
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    public String getBaseUrl() {
        String port = environment.getProperty("local.server.port");
        if (port == null || port.isBlank()) {
            port = environment.getProperty("server.port", "8080");
        }
        return "http://localhost:" + port;
    }

    public String createBasicAuthHeader(String email, String password) {
        String credentials = email + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Authenticates via GET /api/auth/me using Basic Auth.
     */
    public UserResponse login(String email, String password) throws Exception {
        String authHeader = createBasicAuthHeader(email, password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/auth/me"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), UserResponse.class);
            } else if (response.statusCode() == 401) {
                throw new SecurityException("Login failed. Please check your email and password.");
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Login failed (HTTP " + response.statusCode() + ")");
                throw new RuntimeException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server. Is the backend running?");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot reach the server. Is the backend running?");
        }
    }

    /**
     * Registers a new student account via POST /api/auth/register.
     */
    public UserResponse register(String name, String email, String password) throws Exception {
        RegisterRequest registerReq = new RegisterRequest(name, email, password);
        String jsonBody = objectMapper.writeValueAsString(registerReq);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/auth/register"))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                return objectMapper.readValue(response.body(), UserResponse.class);
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Registration failed (HTTP " + response.statusCode() + ")");
                throw new IllegalArgumentException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server. Is the backend running?");
        }
    }

    /**
     * Retrieves tickets submitted or joined by the student via GET /api/tickets/mine.
     */
    public List<TicketListResponse> getMyTickets(String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/tickets/mine"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<TicketListResponse>>() {});
            } else if (response.statusCode() == 401) {
                throw new SecurityException("Session expired. Please sign in again.");
            } else {
                throw new RuntimeException("Failed to fetch tickets (HTTP " + response.statusCode() + ")");
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Retrieves full ticket details via GET /api/tickets/{ticketNumber}.
     */
    public TicketDetailResponse getTicketDetail(String ticketNumber, String authHeader) throws Exception {
        String encoded = URLEncoder.encode(ticketNumber, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/tickets/" + encoded))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), TicketDetailResponse.class);
            } else if (response.statusCode() == 404 || response.statusCode() == 403) {
                throw new NoSuchElementException("Ticket not found, or you don't have access to it.");
            } else {
                String msg = extractErrorMessage(response.body(), "Failed to fetch ticket (HTTP " + response.statusCode() + ")");
                throw new RuntimeException(msg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Retrieves assignment history for a ticket via GET /api/admin/tickets/{ticketId}/assignments.
     * Note: If caller is not admin, the server responds with 403 Forbidden; in that case, returns an empty list.
     */
    public List<AssignmentRecord> getAssignmentHistory(Integer ticketId, String authHeader) {
        if (ticketId == null) {
            return List.of();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/api/admin/tickets/" + ticketId + "/assignments"))
                    .timeout(Duration.ofSeconds(4))
                    .header("Authorization", authHeader)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<AssignmentRecord>>() {});
            }
        } catch (Exception ignored) {
            // Assignment history is admin-only on the backend, or server unreachable
        }
        return List.of();
    }

    /**
     * Retrieves available categories via GET /api/categories.
     */
    public List<CategoryResponse> getCategories(String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/categories"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<CategoryResponse>>() {});
            } else {
                throw new RuntimeException("Failed to fetch categories (HTTP " + response.statusCode() + ")");
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Submits a new ticket via POST /api/tickets.
     */
    public String createTicket(Integer categoryId, String title, String description, String priority, String authHeader) throws Exception {
        CreateTicketRequest req = new CreateTicketRequest(categoryId, title, description, priority);
        String jsonBody = objectMapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/tickets"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                JsonNode node = objectMapper.readTree(response.body());
                if (node.has("ticketNumber")) {
                    return node.get("ticketNumber").asText();
                } else if (node.has("ticketId")) {
                    return "HD-" + node.get("ticketId").asText();
                }
                return "HD-NEW";
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Failed to create ticket (HTTP " + response.statusCode() + ")");
                throw new IllegalArgumentException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Joins an existing ticket via POST /api/tickets/{ticketNumber}/join.
     */
    public boolean joinTicket(String ticketNumber, String authHeader) throws Exception {
        String encoded = URLEncoder.encode(ticketNumber, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/tickets/" + encoded + "/join"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                return node.has("joined") && node.get("joined").asBoolean();
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Could not join this ticket.");
                throw new IllegalArgumentException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Updates ticket status via PUT /api/tickets/{ticketId}/status.
     */
    public void updateTicketStatus(Integer ticketId, String newStatus, String authHeader) throws Exception {
        UpdateStatusRequest req = new UpdateStatusRequest(newStatus);
        String jsonBody = objectMapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/tickets/" + ticketId + "/status"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return;
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Failed to update ticket status (HTTP " + response.statusCode() + ")");
                throw new IllegalArgumentException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Adds a comment to a ticket via POST /api/tickets/{ticketId}/comments.
     */
    public void addComment(Integer ticketId, String commentText, String commentType, String authHeader) throws Exception {
        CommentRequest req = new CommentRequest(commentText, commentType);
        String jsonBody = objectMapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/tickets/" + ticketId + "/comments"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                return;
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Failed to post comment (HTTP " + response.statusCode() + ")");
                throw new IllegalArgumentException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Retrieves overall system statistics via GET /api/admin/stats.
     */
    public AdminStatsResponse getAdminStats(String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/admin/stats"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), AdminStatsResponse.class);
            } else {
                throw new RuntimeException("Failed to fetch admin stats (HTTP " + response.statusCode() + ")");
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Retrieves all campus tickets via GET /api/admin/tickets.
     */
    public List<TicketListResponse> getAllAdminTickets(String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/admin/tickets"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<TicketListResponse>>() {});
            } else {
                throw new RuntimeException("Failed to fetch all tickets (HTTP " + response.statusCode() + ")");
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Retrieves unassigned tickets via GET /api/admin/tickets/unassigned.
     */
    public List<TicketListResponse> getUnassignedTickets(String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/admin/tickets/unassigned"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<TicketListResponse>>() {});
            } else {
                throw new RuntimeException("Failed to fetch unassigned tickets (HTTP " + response.statusCode() + ")");
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Retrieves overdue tickets via GET /api/admin/tickets/overdue.
     */
    public List<TicketListResponse> getOverdueTickets(String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/admin/tickets/overdue"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<TicketListResponse>>() {});
            } else {
                throw new RuntimeException("Failed to fetch overdue tickets (HTTP " + response.statusCode() + ")");
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Retrieves all campus incidents via GET /api/incidents.
     */
    public List<IncidentResponse> getIncidents(String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/incidents"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<IncidentResponse>>() {});
            } else {
                throw new RuntimeException("Failed to fetch incidents (HTTP " + response.statusCode() + ")");
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Retrieves a single incident by ID via GET /api/incidents/{incidentId}.
     */
    public IncidentResponse getIncident(Integer incidentId, String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/incidents/" + incidentId))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), IncidentResponse.class);
            } else if (response.statusCode() == 404) {
                throw new NoSuchElementException("Incident not found: INC-" + incidentId);
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Failed to fetch incident (HTTP " + response.statusCode() + ")");
                throw new RuntimeException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Retrieves tickets linked to an incident via GET /api/incidents/{incidentId}/tickets.
     */
    public List<IncidentTicketResponse> getIncidentTickets(Integer incidentId, String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/incidents/" + incidentId + "/tickets"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<IncidentTicketResponse>>() {});
            } else if (response.statusCode() == 404) {
                throw new NoSuchElementException("Incident not found: INC-" + incidentId);
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Failed to fetch incident tickets (HTTP " + response.statusCode() + ")");
                throw new RuntimeException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Retrieves all support agents via GET /api/admin/agents.
     */
    public List<UserResponse> getAgents(String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/admin/agents"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<UserResponse>>() {});
            } else {
                throw new RuntimeException("Failed to fetch agents (HTTP " + response.statusCode() + ")");
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Retrieves closed tickets log via GET /api/admin/tickets/closed.
     */
    public List<ClosedTicketResponse> getClosedTicketsLog(String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/admin/tickets/closed"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), new TypeReference<List<ClosedTicketResponse>>() {});
            } else {
                throw new RuntimeException("Failed to fetch closed tickets log (HTTP " + response.statusCode() + ")");
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Closes a resolved ticket via POST /api/admin/tickets/{ticketId}/close.
     */
    public void closeTicket(Integer ticketId, String note, String authHeader) throws Exception {
        CloseTicketRequest req = new CloseTicketRequest(note);
        String jsonBody = objectMapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/admin/tickets/" + ticketId + "/close"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return;
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Failed to close ticket (HTTP " + response.statusCode() + ")");
                throw new IllegalArgumentException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Reopens a resolved ticket via POST /api/admin/tickets/{ticketId}/reopen.
     */
    public void reopenTicket(Integer ticketId, String reason, String authHeader) throws Exception {
        ReopenTicketRequest req = new ReopenTicketRequest(reason);
        String jsonBody = objectMapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/admin/tickets/" + ticketId + "/reopen"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return;
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Failed to reopen ticket (HTTP " + response.statusCode() + ")");
                throw new IllegalArgumentException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Declares a new incident via POST /api/incidents.
     */
    public IncidentResponse createIncident(String title, String location, String severity, String description, String authHeader) throws Exception {
        CreateIncidentRequest req = new CreateIncidentRequest(title, description, location, severity);
        String jsonBody = objectMapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/incidents"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                return objectMapper.readValue(response.body(), IncidentResponse.class);
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Failed to declare incident (HTTP " + response.statusCode() + ")");
                throw new IllegalArgumentException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Creates a new support agent via POST /api/admin/agents.
     */
    public UserResponse createAgent(String name, String email, String password, String authHeader) throws Exception {
        CreateAgentRequest req = new CreateAgentRequest(name, email, password);
        String jsonBody = objectMapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/admin/agents"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                return objectMapper.readValue(response.body(), UserResponse.class);
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Failed to create agent (HTTP " + response.statusCode() + ")");
                throw new IllegalArgumentException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Links a ticket to an incident via PUT /api/admin/incidents/{incidentId}/tickets/{ticketId}.
     */
    public void linkTicketToIncident(Integer incidentId, Integer ticketId, String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/admin/incidents/" + incidentId + "/tickets/" + ticketId))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return;
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Failed to link ticket (HTTP " + response.statusCode() + ")");
                throw new IllegalArgumentException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    /**
     * Assigns or reassigns an agent to a ticket via POST /api/admin/tickets/{ticketId}/assign.
     */
    public void assignTicket(Integer ticketId, Integer agentId, String reason, String authHeader) throws Exception {
        AssignTicketRequest req = new AssignTicketRequest(agentId, reason);
        String jsonBody = objectMapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/admin/tickets/" + ticketId + "/assign"))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return;
            } else {
                String errorMsg = extractErrorMessage(response.body(), "Failed to assign ticket (HTTP " + response.statusCode() + ")");
                throw new IllegalArgumentException(errorMsg);
            }
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new IllegalStateException("Cannot reach the server.");
        }
    }

    private String extractErrorMessage(String responseBody, String defaultMsg) {
        if (responseBody == null || responseBody.isBlank()) {
            return defaultMsg;
        }
        try {
            ErrorResponse err = objectMapper.readValue(responseBody, ErrorResponse.class);
            if (err.error() != null && !err.error().isBlank()) {
                return err.error();
            }
        } catch (Exception ignored) {
        }
        return defaultMsg;
    }
}
