package com.pdfreader.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdfreader.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String USERS_KEY = "users/data.json";

    private final S3Service s3Service;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    public User register(String username, String password) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Username is required");
        if (password == null || password.length() < 4) throw new IllegalArgumentException("Password must be at least 4 characters");

        List<User> users = loadUsers();
        boolean exists = users.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
        if (exists) throw new IllegalArgumentException("Username already taken");

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(username.trim())
                .passwordHash(passwordEncoder.encode(password))
                .build();
        users.add(user);
        saveUsers(users);
        log.info("Registered user: {}", username);
        return user;
    }

    public Optional<User> login(String username, String password) {
        return loadUsers().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .findFirst();
    }

    private List<User> loadUsers() {
        try {
            String json = s3Service.downloadJson(USERS_KEY);
            if (json == null) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Could not load users from S3: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private void saveUsers(List<User> users) {
        try {
            String json = objectMapper.writeValueAsString(users);
            s3Service.uploadJson(USERS_KEY, json);
        } catch (Exception e) {
            log.error("Could not save users to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to save user data");
        }
    }
}
