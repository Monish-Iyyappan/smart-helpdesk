package com.smarthelpdesk.service;

import com.smarthelpdesk.dao.UserDAO;
import com.smarthelpdesk.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserDAO userDAO, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String name, String email, String rawPassword) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        String cleanEmail = email.trim();
        if (!cleanEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        if (userDAO.findByEmail(cleanEmail).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + cleanEmail);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = new User(name.trim(), cleanEmail, passwordHash, "STUDENT");
        userDAO.insert(user);
        return user;
    }

    public Optional<User> login(String email, String rawPassword) {
        if (email == null || email.trim().isEmpty() || rawPassword == null) {
            return Optional.empty();
        }

        Optional<User> userOpt = userDAO.findByEmail(email.trim());
        if (userOpt.isPresent() && passwordEncoder.matches(rawPassword, userOpt.get().getPasswordHash())) {
            return userOpt;
        }
        return Optional.empty();
    }

    public Optional<User> findById(Integer userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userDAO.findById(userId);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return userDAO.findByEmail(email.trim());
    }
}
