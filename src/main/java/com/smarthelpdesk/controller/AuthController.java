package com.smarthelpdesk.controller;

import com.smarthelpdesk.config.SecurityUtils;
import com.smarthelpdesk.dto.RegisterRequest;
import com.smarthelpdesk.dto.UserResponse;
import com.smarthelpdesk.exception.ResourceNotFoundException;
import com.smarthelpdesk.model.User;
import com.smarthelpdesk.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        User user = authService.register(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        Integer actorUserId = SecurityUtils.getActorUserId(authentication);
        User user = authService.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
