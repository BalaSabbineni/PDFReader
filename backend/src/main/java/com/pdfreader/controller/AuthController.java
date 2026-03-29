package com.pdfreader.controller;

import com.pdfreader.config.JwtUtil;
import com.pdfreader.controller.dto.AuthResponse;
import com.pdfreader.controller.dto.LoginRequest;
import com.pdfreader.controller.dto.RegisterRequest;
import com.pdfreader.model.User;
import com.pdfreader.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            User user = userService.register(req.getUsername(), req.getPassword());
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, user.getUsername()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Optional<User> user = userService.login(req.getUsername(), req.getPassword());
        if (user.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
        String token = jwtUtil.generateToken(user.get().getId(), user.get().getUsername());
        return ResponseEntity.ok(new AuthResponse(token, user.get().getUsername()));
    }
}
